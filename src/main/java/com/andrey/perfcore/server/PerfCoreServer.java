package com.andrey.perfcore.server;

import com.andrey.perfcore.PerfCore;
import com.andrey.perfcore.PerfCoreConfig;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class PerfCoreServer {
    private static final double RESTING_HORIZONTAL_SPEED_SQ = 1.0E-4D;

    private static long checkedAiTicks;
    private static long skippedAiTicks;
    private static long skippedEntityTicks;
    private static long serverTicks;
    private static int tntExplosionsThisTick;
    private static long tntExplosionsTotal;
    private static long tntExplosionsDeferred;

    private PerfCoreServer() {
    }

    private static boolean hasPlayerWithin(ServerLevel level, Entity entity, double radiusSq, boolean ignoreSpectators) {
        List<ServerPlayer> players = level.players();
        for (int i = 0, size = players.size(); i < size; i++) {
            ServerPlayer player = players.get(i);
            if (ignoreSpectators && player.isSpectator()) {
                continue;
            }
            if (player.distanceToSqr(entity) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldSkipAi(Mob mob) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.aiThrottle) {
            return false;
        }

        int interval = PerfCoreConfig.aiTickInterval;
        if (interval <= 1) {
            return false;
        }

        if (mob.tickCount < 20) {
            return false;
        }

        if (PerfCoreConfig.protectPersistentMobs && mob.isPersistenceRequired()) {
            return false;
        }

        if (mob.isPassenger() || mob.isVehicle()) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        double radius = PerfCoreConfig.aiThrottleDistance;
        if (hasPlayerWithin(level, mob, radius * radius, true)) {
            return false;
        }

        boolean skip = Math.floorMod(mob.tickCount + mob.getId(), interval) != 0;

        if (PerfCoreConfig.logStats) {
            checkedAiTicks++;
            if (skip) {
                skippedAiTicks++;
            }
        }

        return skip;
    }

    public static boolean shouldSkipRestingEntity(Entity entity) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.freezeFarEntities) {
            return false;
        }

        if (entity.tickCount % 20 < 2) {
            return false;
        }

        if (!entity.onGround() || entity.isInWater() || entity.isInLava()) {
            return false;
        }

        if (entity.getDeltaMovement().horizontalDistanceSqr() > RESTING_HORIZONTAL_SPEED_SQ) {
            return false;
        }

        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }

        double distance = PerfCoreConfig.freezeDistance;
        if (hasPlayerWithin(level, entity, distance * distance, false)) {
            return false;
        }

        if (PerfCoreConfig.logStats) {
            skippedEntityTicks++;
        }

        return true;
    }

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

    public static void onServerTickPre() {
        tntExplosionsThisTick = 0;
    }

    public static void onServerTick() {
        serverTicks++;
        if (PerfCoreConfig.logStats && serverTicks % 1200L == 0L) {
            PerfCore.LOGGER.info(
                    "PerfCore: {} тиков сервера, ИИ проверено {}{}, тиков покоящихся сущностей пропущено {}, "
                            + "взрывов динамита {}{}",
                    serverTicks,
                    checkedAiTicks,
                    checkedAiTicks == 0L
                            ? ""
                            : " (пропущено " + skippedAiTicks + ", "
                                    + (100L * skippedAiTicks / checkedAiTicks) + "%)",
                    skippedEntityTicks,
                    tntExplosionsTotal,
                    tntExplosionsDeferred == 0L
                            ? ""
                            : " (отложено " + tntExplosionsDeferred + ")"
            );
        }
    }

    public static void onServerStopped() {
        if (PerfCoreConfig.logStats) {
            PerfCore.LOGGER.info(
                    "PerfCore: сервер остановлен. Тиков: {}, проверок ИИ: {}, пропущено: {}, "
                            + "пропущено тиков сущностей: {}, взрывов динамита: {}, отложено: {}",
                    serverTicks, checkedAiTicks, skippedAiTicks, skippedEntityTicks,
                    tntExplosionsTotal, tntExplosionsDeferred
            );
        }
        checkedAiTicks = 0L;
        skippedAiTicks = 0L;
        skippedEntityTicks = 0L;
        serverTicks = 0L;
        tntExplosionsThisTick = 0;
        tntExplosionsTotal = 0L;
        tntExplosionsDeferred = 0L;
    }
}
