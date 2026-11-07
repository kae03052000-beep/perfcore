package com.andrey.perfcore.client;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PerfCoreClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue DEBUG_LOG;
    private static final ForgeConfigSpec.BooleanValue DYNAMIC_RENDER_DISTANCE;
    private static final ForgeConfigSpec.IntValue TARGET_FPS;
    private static final ForgeConfigSpec.IntValue MIN_RENDER_DISTANCE;
    private static final ForgeConfigSpec.IntValue MAX_RENDER_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue PARTICLE_LIMIT;
    private static final ForgeConfigSpec.IntValue MAX_PARTICLES_PER_TICK;
    private static final ForgeConfigSpec.DoubleValue PARTICLE_CULL_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue ENTITY_RENDER_CULLING;
    private static final ForgeConfigSpec.IntValue ENTITY_RENDER_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue PRE_SIZE_GPU_BUFFERS;
    private static final ForgeConfigSpec.IntValue GPU_BUFFER_INITIAL_CAPACITY;
    private static final ForgeConfigSpec.BooleanValue SOUND_THROTTLING;
    private static final ForgeConfigSpec.IntValue MAX_SOUNDS_PER_TICK;

    public static volatile boolean enabled = true;
    public static volatile boolean debugLog = false;
    public static volatile boolean dynamicRenderDistance = true;
    public static volatile int targetFps = 60;
    public static volatile int minRenderDistance = 6;
    public static volatile int maxRenderDistance = 16;
    public static volatile boolean particleLimit = true;
    public static volatile int maxParticlesPerTick = 512;
    public static volatile double particleCullDistance = 32.0D;
    public static volatile boolean entityRenderCulling = true;
    public static volatile int entityRenderDistance = 96;
    public static volatile boolean preSizeGpuBuffers = true;
    public static volatile int gpuBufferInitialCapacity = 4096;
    public static volatile boolean soundThrottling = true;
    public static volatile int maxSoundsPerTick = 8;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "PerfCore — клиентские оптимизации.",
                "Все значения можно менять без перезапуска игры."
        ).push("general");
        ENABLED = builder.comment("Включить клиентские оптимизации PerfCore.")
                .define("enabled", true);
        DEBUG_LOG = builder.comment("Писать в лог изменения дальности прорисовки и счётчики отброшенных частиц и звуков.")
                .define("debugLog", false);
        builder.pop();

        builder.comment("Ограничение звуков (защита от одновременных взрывов).").push("sounds");
        SOUND_THROTTLING = builder.comment(
                        "Ограничивать количество одновременно запускаемых звуков окружения.",
                        "Музыка и звуки интерфейса не ограничиваются."
                )
                .define("soundThrottling", true);
        MAX_SOUNDS_PER_TICK = builder.comment("Максимум новых звуков окружения за один клиентский тик.")
                .defineInRange("maxSoundsPerTick", 8, 1, 32);
        builder.pop();

        builder.comment("Автоматическая подстройка дальности прорисовки под FPS.").push("dynamicRenderDistance");
        DYNAMIC_RENDER_DISTANCE = builder.comment(
                        "Если FPS стабильно ниже цели — дальность прорисовки уменьшается,",
                        "если выше — постепенно возвращается к значению, выбранному игроком."
                )
                .define("enabled", true);
        TARGET_FPS = builder.comment("Желаемый FPS.")
                .defineInRange("targetFps", 60, 30, 240);
        MIN_RENDER_DISTANCE = builder.comment("Нижняя граница дальности прорисовки (чанки).")
                .defineInRange("minDistance", 6, 2, 16);
        MAX_RENDER_DISTANCE = builder.comment("Верхняя граница дальности прорисовки (чанки).")
                .defineInRange("maxDistance", 16, 4, 32);
        builder.pop();

        builder.comment("Ограничение частиц — главный источник просадок в боях и взрывах.").push("particles");
        PARTICLE_LIMIT = builder.comment(
                        "Ограничивать количество новых частиц за тик и отсекать далёкие частицы."
                )
                .define("enabled", true);
        MAX_PARTICLES_PER_TICK = builder.comment(
                        "Максимум новых частиц за один клиентский тик.",
                        "Ванильные эффекты почти никогда не превышают это значение;",
                        "взрывы и модовые эффекты — легко."
                )
                .defineInRange("maxPerTick", 512, 64, 4096);
        PARTICLE_CULL_DISTANCE = builder.comment(
                        "Не создавать частицы дальше этого расстояния от камеры (в блоках).",
                        "0 — отключить отсечение по расстоянию."
                )
                .defineInRange("cullDistance", 32.0D, 0.0D, 128.0D);
        builder.pop();

        builder.comment("Отсечение отрисовки далёких живых сущностей.").push("entityRendering");
        ENTITY_RENDER_CULLING = builder.comment(
                        "Не отрисовывать живых сущностей дальше указанного радиуса.",
                        "Игроки не отсекаются никогда."
                )
                .define("enabled", true);
        ENTITY_RENDER_DISTANCE = builder.comment("Радиус отрисовки сущностей (в блоках).")
                .defineInRange("distance", 96, 32, 256);
        builder.pop();

        builder.comment(
                "Предварительный размер GPU-буферов.",
                "В Minecraft 26.3 динамические буферы начинаются с очень малой ёмкости и",
                "многократно пересоздаются, засоряя лог. PerfCore сразу выделяет",
                "достаточный буфер."
        ).push("gpuBuffers");
        PRE_SIZE_GPU_BUFFERS = builder.comment("Выделять GPU-буферы с запасом при создании.")
                .define("preSize", true);
        GPU_BUFFER_INITIAL_CAPACITY = builder.comment(
                        "Начальная ёмкость динамических GPU-буферов (степень двойки подбирается автоматически)."
                )
                .defineInRange("initialCapacity", 4096, 1024, 16384);
        builder.pop();

        SPEC = builder.build();
    }

    private PerfCoreClientConfig() {
    }

    public static int effectiveGpuBufferCapacity(int vanillaCapacity) {
        if (!enabled || !preSizeGpuBuffers) {
            return vanillaCapacity;
        }

        int configured = gpuBufferInitialCapacity;
        return configured > vanillaCapacity ? configured : vanillaCapacity;
    }

    public static void bake() {
        try {
            enabled = ENABLED.get();
            debugLog = DEBUG_LOG.get();
            dynamicRenderDistance = DYNAMIC_RENDER_DISTANCE.get();
            targetFps = TARGET_FPS.get();
            minRenderDistance = MIN_RENDER_DISTANCE.get();
            maxRenderDistance = MAX_RENDER_DISTANCE.get();
            particleLimit = PARTICLE_LIMIT.get();
            maxParticlesPerTick = MAX_PARTICLES_PER_TICK.get();
            particleCullDistance = PARTICLE_CULL_DISTANCE.get();
            entityRenderCulling = ENTITY_RENDER_CULLING.get();
            entityRenderDistance = ENTITY_RENDER_DISTANCE.get();
            preSizeGpuBuffers = PRE_SIZE_GPU_BUFFERS.get();
            gpuBufferInitialCapacity = GPU_BUFFER_INITIAL_CAPACITY.get();
            soundThrottling = SOUND_THROTTLING.get();
            maxSoundsPerTick = MAX_SOUNDS_PER_TICK.get();
        } catch (IllegalStateException notLoadedYet) {
            return;
        }
    }
}
