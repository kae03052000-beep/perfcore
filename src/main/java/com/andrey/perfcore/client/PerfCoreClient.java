package com.andrey.perfcore.client;

import com.andrey.perfcore.PerfCore;
import com.andrey.perfcore.mixin.SoundEngineMixin;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;

/**
 * Клиентские оптимизации PerfCore.
 *
 * <p>Все обработчики работают в render thread и не используют общие
 * изменяемые коллекции без необходимости.</p>
 */
public final class PerfCoreClient {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int SETTLE_TICKS = 200;

    private static int particleBudget;
    private static int droppedByBudget;
    private static int droppedByDistance;
    private static int clientTicks;
    private static int ticksInWorld;
    private static int lowFpsSamples;
    private static int highFpsSamples;
    private static int changeCooldown;
    private static int baselineRenderDistance = -1;
    private static int appliedRenderDistance = -1;
    private static Object lastLevel;

    private PerfCoreClient() {
    }

    public static void init() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> onClientTickPre());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onClientTickPost());

        @SuppressWarnings({"rawtypes", "unchecked"})
        CancellableEventBus<RenderLivingEvent.Pre> bus =
                (CancellableEventBus) RenderLivingEvent.Pre.BUS;
        bus.addListener((Predicate<RenderLivingEvent.Pre>) PerfCoreClient::shouldCullLiving);
    }

    private static void onClientTickPre() {
        // Бюджет частиц считается заново каждый тик, а не за кадр: так взрыв
        // не может «съесть» лимит на несколько секунд вперёд.
        particleBudget = 0;
        // Сброс бюджета звуков на новый тик.
        SoundEngineMixin.resetCounter();
    }

    private static void onClientTickPost() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            resetWorldState(minecraft);
            return;
        }

        if (minecraft.level != lastLevel) {
            // При смене мира возвращаем дальность прорисовки, которую выбрал
            // игрок, и запоминаем её как базу для нового мира.
            restoreRenderDistance(minecraft);
            lastLevel = minecraft.level;
            baselineRenderDistance = minecraft.options.renderDistance().get();
            ticksInWorld = 0;
            lowFpsSamples = 0;
            highFpsSamples = 0;
            changeCooldown = 0;
        }

        if (PerfCoreClientConfig.debugLog && clientTicks % 100 == 0
                && (droppedByBudget > 0 || droppedByDistance > 0)) {
            PerfCore.LOGGER.info(
                    "PerfCore: отброшено частиц за последние 100 тиков — по лимиту {}, по дистанции {}",
                    droppedByBudget, droppedByDistance
            );
            droppedByBudget = 0;
            droppedByDistance = 0;
        }

        clientTicks++;
        if (ticksInWorld < SETTLE_TICKS) {
            ticksInWorld++;
            return;
        }

        if (!PerfCoreClientConfig.enabled || !PerfCoreClientConfig.dynamicRenderDistance) {
            restoreRenderDistance(minecraft);
            return;
        }

        if (changeCooldown > 0) {
            changeCooldown--;
            return;
        }

        if (clientTicks % CHECK_INTERVAL_TICKS != 0 || minecraft.isPaused()) {
            return;
        }

        int fps = minecraft.getFps();
        if (fps <= 0) {
            return;
        }

        int current = minecraft.options.renderDistance().get();

        // Если игрок вручную поменял дальность, уважаем его выбор: берём новое
        // значение за базу и больше не трогаем настройку до конца мира.
        if (appliedRenderDistance >= 0 && current != appliedRenderDistance) {
            baselineRenderDistance = current;
            appliedRenderDistance = -1;
            return;
        }

        int targetFps = PerfCoreClientConfig.targetFps;
        int minDistance = Math.max(2, PerfCoreClientConfig.minRenderDistance);
        int maxDistance = Math.max(minDistance, PerfCoreClientConfig.maxRenderDistance);

        // Работаем только в пределах, которые игрок выбрал сам. Если его
        // дальность вне настроек PerfCore, не навязываем свои значения.
        if (baselineRenderDistance < minDistance || baselineRenderDistance > maxDistance) {
            return;
        }

        // Никогда не поднимаем дальность выше изначального выбора игрока.
        maxDistance = Math.min(maxDistance, baselineRenderDistance);

        if (fps < targetFps * 0.80D) {
            lowFpsSamples++;
            highFpsSamples = 0;
        } else if (fps > targetFps * 1.15D) {
            highFpsSamples++;
            lowFpsSamples = 0;
        } else {
            lowFpsSamples = 0;
            highFpsSamples = 0;
        }

        if (lowFpsSamples >= 3 && current > minDistance) {
            setRenderDistance(minecraft, current - 1, fps);
            lowFpsSamples = 0;
            changeCooldown = 100;
        } else if (highFpsSamples >= 8 && current < maxDistance) {
            setRenderDistance(minecraft, current + 1, fps);
            highFpsSamples = 0;
            changeCooldown = 200;
        }
    }

    private static void setRenderDistance(Minecraft minecraft, int value, int fps) {
        int oldValue = minecraft.options.renderDistance().get();
        if (value == oldValue) {
            return;
        }

        minecraft.options.renderDistance().set(value);
        appliedRenderDistance = value;

        if (PerfCoreClientConfig.debugLog) {
            PerfCore.LOGGER.info(
                    "PerfCore: дальность прорисовки {} -> {} (FPS {})",
                    oldValue, value, fps
            );
        }
    }

    /**
     * Возвращает дальность прорисовки, выбранную игроком до вмешательства мода.
     *
     * <p>Без этого автоподстройка навсегда меняла бы настройку в options.txt
     * после выхода из мира.</p>
     */
    private static void restoreRenderDistance(Minecraft minecraft) {
        if (appliedRenderDistance >= 0 && baselineRenderDistance >= 0) {
            int current = minecraft.options.renderDistance().get();
            if (current != baselineRenderDistance) {
                minecraft.options.renderDistance().set(baselineRenderDistance);
            }

            if (PerfCoreClientConfig.debugLog) {
                PerfCore.LOGGER.info(
                        "PerfCore: дальность прорисовки восстановлена до {}",
                        baselineRenderDistance
                );
            }
        }

        appliedRenderDistance = -1;
    }

    private static void resetWorldState(Minecraft minecraft) {
        restoreRenderDistance(minecraft);
        lastLevel = null;
        baselineRenderDistance = -1;
        ticksInWorld = 0;
        lowFpsSamples = 0;
        highFpsSamples = 0;
        changeCooldown = 0;
        droppedByBudget = 0;
        droppedByDistance = 0;
    }

    /**
     * @return {@code true}, если сущность нужно пропустить (отменить отрисовку).
     */
    @SuppressWarnings("rawtypes")
    private static boolean shouldCullLiving(RenderLivingEvent.Pre event) {
        if (!PerfCoreClientConfig.enabled || !PerfCoreClientConfig.entityRenderCulling) {
            return false;
        }

        var state = event.getState();
        if (state == null || state.entityType == null || state.entityType == EntityTypes.PLAYER) {
            return false;
        }

        int distance = PerfCoreClientConfig.entityRenderDistance;
        if (distance <= 0) {
            return false;
        }

        double maxDistanceSq = (double) distance * distance;
        return state.distanceToCameraSq > maxDistanceSq;
    }

    /**
     * Вызывается из миксина {@code ParticleEngine#add}.
     *
     * @return {@code true}, если частицу можно создать.
     */
    public static boolean allowParticle(Particle particle) {
        if (!PerfCoreClientConfig.enabled || !PerfCoreClientConfig.particleLimit) {
            return true;
        }

        if (particleBudget >= PerfCoreClientConfig.maxParticlesPerTick) {
            droppedByBudget++;
            return false;
        }

        double cullDistance = PerfCoreClientConfig.particleCullDistance;
        if (cullDistance > 0.0D) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                Vec3 camera = minecraft.gameRenderer.mainCamera().position();
                Vec3 particlePos = particle.getPos();
                if (camera.distanceToSqr(particlePos) > cullDistance * cullDistance) {
                    droppedByDistance++;
                    return false;
                }
            }
        }

        particleBudget++;
        return true;
    }
}
