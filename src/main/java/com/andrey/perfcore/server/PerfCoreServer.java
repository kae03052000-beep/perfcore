package com.andrey.perfcore.server;

import com.andrey.perfcore.PerfCore;
import com.andrey.perfcore.PerfCoreConfig;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Серверная логика, вызываемая из миксинов и событий Forge.
 *
 * <p>Класс намеренно не содержит клиентских ссылок: он должен безопасно
 * загружаться на выделенном сервере.</p>
 */
public final class PerfCoreServer {
    private static long checkedAiTicks;
    private static long skippedAiTicks;
    private static long serverTicks;
    private static int tntExplosionsThisTick;
    private static long tntExplosionsTotal;
    private static long tntExplosionsDeferred;

    private PerfCoreServer() {
    }

    /**
     * Решает, можно ли пропустить {@code Mob#serverAiStep()} в текущем тике.
     *
     * <p>Метод вызывается для каждого моба каждый тик, поэтому здесь нет
     * потоков, стримов, боксинга и обращений к конфигу через ForgeConfigSpec.</p>
     */
    public static boolean shouldSkipAi(Mob mob) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.aiThrottle) {
            return false;
        }

        int interval = PerfCoreConfig.aiTickInterval;
        if (interval <= 1) {
            return false;
        }

        // Первые двадцать тиков после спавна не трогаем: моб должен корректно
        // инициализироваться и не зависнуть в воздухе.
        if (mob.tickCount < 20) {
            return false;
        }

        if (PerfCoreConfig.protectPersistentMobs && mob.isPersistenceRequired()) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        double radius = PerfCoreConfig.aiThrottleDistance;
        double radiusSq = radius * radius;

        List<ServerPlayer> players = level.players();
        for (int i = 0, size = players.size(); i < size; i++) {
            ServerPlayer player = players.get(i);
            if (player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(mob) <= radiusSq) {
                return false;
            }
        }

        // Разные мобы пропускают разные тики, иначе вся нагрузка приходится
        // на один тик и появляются рывки.
        boolean skip = Math.floorMod(mob.tickCount + mob.getId(), interval) != 0;

        if (PerfCoreConfig.logStats) {
            checkedAiTicks++;
            if (skip) {
                skippedAiTicks++;
            }
        }

        return skip;
    }

    /**
     * Решает, можно ли пропустить тик предмета/сферы опыта.
     * Тик выполняется раз в 20 тиков, чтобы таймеры деспавна работали.
     */
    public static boolean shouldSkipEntityTick(net.minecraft.world.entity.Entity entity) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.freezeFarEntities) {
            return false;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        if (entity.tickCount % 20 != 0) {
            double radiusSq = (double) PerfCoreConfig.freezeDistance
                    * PerfCoreConfig.freezeDistance;
            List<ServerPlayer> players = level.players();
            for (int i = 0, size = players.size(); i < size; i++) {
                if (players.get(i).distanceToSqr(entity) <= radiusSq) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Резервирует слот для взрыва динамита в текущем тике.
     *
     * <p>Если лимит исчерпан, миксин {@code PrimedTnt} продлевает фитиль на
     * пару тиков. Так большое количество взрывов не обрабатывается за один
     * тик и не вызывает фриз, но ни один динамит не пропадает.</p>
     *
     * @return {@code true}, если взрыв можно обработать сейчас.
     */
    public static boolean tryReserveTntExplosion() {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.tntOptimizations) {
            return true;
        }

        if (tntExplosionsThisTick < PerfCoreConfig.maxTntExplosionsPerTick) {
            tntExplosionsThisTick++;
            tntExplosionsTotal++;
            return true;
        }

        tntExplosionsDeferred++;
        return false;
    }

    /** Вызывается из {@code TickEvent.ServerTickEvent.Pre}. */
    public static void onServerTickPre() {
        tntExplosionsThisTick = 0;
    }

    /** Вызывается из {@code TickEvent.ServerTickEvent.Post}. */
    public static void onServerTick() {
        serverTicks++;
        if (PerfCoreConfig.logStats && serverTicks % 1200L == 0L) {
            PerfCore.LOGGER.info(
                    "PerfCore: {} тиков сервера, ИИ проверено {}{}, взрывов динамита {}{}",
                    serverTicks,
                    checkedAiTicks,
                    checkedAiTicks == 0L
                            ? ""
                            : " (пропущено " + skippedAiTicks + ", "
                                    + (100L * skippedAiTicks / checkedAiTicks) + "%)",
                    tntExplosionsTotal,
                    tntExplosionsDeferred == 0L
                            ? ""
                            : " (отложено " + tntExplosionsDeferred + ")"
            );
        }
    }

    /** Вызывается при остановке сервера. */
    public static void onServerStopped() {
        if (PerfCoreConfig.logStats) {
            PerfCore.LOGGER.info(
                    "PerfCore: сервер остановлен. Тиков: {}, проверок ИИ: {}, пропущено: {}, "
                            + "взрывов динамита: {}, отложено: {}",
                    serverTicks, checkedAiTicks, skippedAiTicks,
                    tntExplosionsTotal, tntExplosionsDeferred
            );
        }
        checkedAiTicks = 0L;
        skippedAiTicks = 0L;
        serverTicks = 0L;
        tntExplosionsThisTick = 0;
        tntExplosionsTotal = 0L;
        tntExplosionsDeferred = 0L;
    }
}
