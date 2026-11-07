package com.andrey.perfcore.client;

import com.andrey.perfcore.PerfCore;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;

public final class PerfCoreClient {
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int SETTLE_TICKS = 200;
    private static final int LOG_INTERVAL_TICKS = 100;

    private static int particleBudget;
    private static int soundBudget;
    private static int droppedParticlesByBudget;
    private static int droppedParticlesByDistance;
    private static int droppedSounds;
    private static int clientTicks;
    private static int ticksInWorld;
    private static int lowFpsSamples;
    private static int highFpsSamples;
    private static int changeCooldown;
    private static int baselineRenderDistance = -1;
    private static int lastKnownRenderDistance = -1;
    private static boolean modifiedByMod;
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
        particleBudget = 0;
        soundBudget = 0;
    }

    private static void onClientTickPost() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            resetWorldState(minecraft);
            return;
        }

        if (minecraft.level != lastLevel) {
            restoreRenderDistance(minecraft);
            lastLevel = minecraft.level;
            int selected = minecraft.options.renderDistance().get();
            baselineRenderDistance = selected;
            lastKnownRenderDistance = selected;
            modifiedByMod = false;
            ticksInWorld = 0;
            lowFpsSamples = 0;
            highFpsSamples = 0;
            changeCooldown = 0;
        }

        clientTicks++;
        logDrops();

        if (ticksInWorld < SETTLE_TICKS) {
            ticksInWorld++;
            return;
        }

        if (!PerfCoreClientConfig.enabled || !PerfCoreClientConfig.dynamicRenderDistance) {
            restoreRenderDistance(minecraft);
            return;
        }

        int current = minecraft.options.renderDistance().get();
        if (current != lastKnownRenderDistance) {
            baselineRenderDistance = current;
            lastKnownRenderDistance = current;
            modifiedByMod = false;
            lowFpsSamples = 0;
            highFpsSamples = 0;
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

        int targetFps = PerfCoreClientConfig.targetFps;
        int minDistance = Math.max(2, PerfCoreClientConfig.minRenderDistance);
        int maxDistance = Math.max(minDistance, PerfCoreClientConfig.maxRenderDistance);

        if (baselineRenderDistance < minDistance || baselineRenderDistance > maxDistance) {
            return;
        }

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

    private static void logDrops() {
        if (!PerfCoreClientConfig.debugLog || clientTicks % LOG_INTERVAL_TICKS != 0) {
            return;
        }
        if (droppedParticlesByBudget == 0 && droppedParticlesByDistance == 0 && droppedSounds == 0) {
            return;
        }

        PerfCore.LOGGER.info(
                "PerfCore: за последние {} тиков отброшено частиц по лимиту {}, по дистанции {}, звуков {}",
                LOG_INTERVAL_TICKS, droppedParticlesByBudget, droppedParticlesByDistance, droppedSounds
        );
        droppedParticlesByBudget = 0;
        droppedParticlesByDistance = 0;
        droppedSounds = 0;
    }

    private static void setRenderDistance(Minecraft minecraft, int value, int fps) {
        int oldValue = minecraft.options.renderDistance().get();
        if (value == oldValue) {
            return;
        }

        minecraft.options.renderDistance().set(value);
        lastKnownRenderDistance = value;
        modifiedByMod = true;

        if (PerfCoreClientConfig.debugLog) {
            PerfCore.LOGGER.info(
                    "PerfCore: дальность прорисовки {} -> {} (FPS {})",
                    oldValue, value, fps
            );
        }
    }

    private static void restoreRenderDistance(Minecraft minecraft) {
        if (modifiedByMod && baselineRenderDistance >= 0) {
            int current = minecraft.options.renderDistance().get();
            if (current != baselineRenderDistance) {
                minecraft.options.renderDistance().set(baselineRenderDistance);
            }
            lastKnownRenderDistance = baselineRenderDistance;

            if (PerfCoreClientConfig.debugLog) {
                PerfCore.LOGGER.info(
                        "PerfCore: дальность прорисовки восстановлена до {}",
                        baselineRenderDistance
                );
            }
        }

        modifiedByMod = false;
    }

    private static void resetWorldState(Minecraft minecraft) {
        restoreRenderDistance(minecraft);
        lastLevel = null;
        baselineRenderDistance = -1;
        lastKnownRenderDistance = -1;
        modifiedByMod = false;
        ticksInWorld = 0;
        lowFpsSamples = 0;
        highFpsSamples = 0;
        changeCooldown = 0;
        droppedParticlesByBudget = 0;
        droppedParticlesByDistance = 0;
        droppedSounds = 0;
    }

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

    public static boolean allowParticle(Particle particle) {
        if (!PerfCoreClientConfig.enabled || !PerfCoreClientConfig.particleLimit) {
            return true;
        }

        if (particleBudget >= PerfCoreClientConfig.maxParticlesPerTick) {
            droppedParticlesByBudget++;
            return false;
        }

        double cullDistance = PerfCoreClientConfig.particleCullDistance;
        if (cullDistance > 0.0D) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                Vec3 camera = minecraft.gameRenderer.mainCamera().position();
                Vec3 particlePos = particle.getPos();
                if (camera.distanceToSqr(particlePos) > cullDistance * cullDistance) {
                    droppedParticlesByDistance++;
                    return false;
                }
            }
        }

        particleBudget++;
        return true;
    }

    public static boolean allowSound(SoundInstance sound) {
        if (!PerfCoreClientConfig.enabled || !PerfCoreClientConfig.soundThrottling) {
            return true;
        }

        if (sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
            return true;
        }

        if (soundBudget >= PerfCoreClientConfig.maxSoundsPerTick) {
            droppedSounds++;
            return false;
        }

        soundBudget++;
        return true;
    }
}
