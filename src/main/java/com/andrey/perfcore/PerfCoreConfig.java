package com.andrey.perfcore;

import net.minecraftforge.common.ForgeConfigSpec;

public final class PerfCoreConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue FREEZE_FAR_ENTITIES;
    private static final ForgeConfigSpec.IntValue FREEZE_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue LOG_STATS;
    private static final ForgeConfigSpec.BooleanValue AI_THROTTLE;
    private static final ForgeConfigSpec.IntValue AI_THROTTLE_DISTANCE;
    private static final ForgeConfigSpec.IntValue AI_TICK_INTERVAL;
    private static final ForgeConfigSpec.BooleanValue PROTECT_PERSISTENT_MOBS;
    private static final ForgeConfigSpec.BooleanValue TNT_OPTIMIZATIONS;
    private static final ForgeConfigSpec.IntValue MAX_TNT_EXPLOSIONS_PER_TICK;
    private static final ForgeConfigSpec.BooleanValue FAST_WORLD_CREATION;
    private static final ForgeConfigSpec.IntValue SPAWN_SEARCH_RADIUS;

    public static volatile boolean enabled = true;
    public static volatile boolean freezeFarEntities = true;
    public static volatile int freezeDistance = 100;
    public static volatile boolean logStats = false;
    public static volatile boolean aiThrottle = true;
    public static volatile int aiThrottleDistance = 48;
    public static volatile int aiTickInterval = 2;
    public static volatile boolean protectPersistentMobs = true;
    public static volatile boolean tntOptimizations = true;
    public static volatile int maxTntExplosionsPerTick = 12;
    public static volatile boolean fastWorldCreation = true;
    public static volatile int spawnSearchRadius = 3;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "PerfCore — общие настройки производительности.",
                "Изменения подхватываются при сохранении файла."
        ).push("general");
        ENABLED = builder.comment("Включить общие оптимизации PerfCore.")
                .define("enabled", true);
        FREEZE_FAR_ENTITIES = builder.comment(
                        "Не тикать покоящиеся предметы и сферы опыта, если рядом нет игроков.",
                        "Такие сущности выполняют полный тик раз в 20 тиков, а пропущенное время",
                        "добавляется к их таймеру деспавна, поэтому исчезают они вовремя."
                )
                .define("freezeFarEntities", true);
        FREEZE_DISTANCE = builder.comment(
                        "Радиус (в блоках), в котором предметы и сферы опыта тикают с полной частотой."
                )
                .defineInRange("freezeDistance", 100, 32, 256);
        LOG_STATS = builder.comment("Писать в лог краткую статистику каждую минуту и при остановке сервера.")
                .define("logStats", false);
        builder.pop();

        builder.comment("Оптимизация тика мобов на сервере.").push("mobAi");
        AI_THROTTLE = builder.comment(
                        "Тикать ИИ мобов реже, если рядом нет игроков.",
                        "Мобы дальше distance блоков пропускают часть тиков ИИ.",
                        "Мобы-наездники и мобы с пассажирами не затрагиваются."
                )
                .define("enabled", true);
        AI_THROTTLE_DISTANCE = builder.comment(
                        "Радиус (в блоках), в котором моб всегда тикает с полной частотой."
                )
                .defineInRange("distance", 48, 16, 128);
        AI_TICK_INTERVAL = builder.comment(
                        "Во сколько раз реже тикает ИИ далёких мобов.",
                        "2 = каждый второй тик, 4 = каждый четвёртый. Большие значения экономнее,",
                        "но далёкие мобы будут двигаться заметно рывками."
                )
                .defineInRange("tickInterval", 2, 2, 10);
        PROTECT_PERSISTENT_MOBS = builder.comment(
                        "Не трогать мобов с persistenceRequired (боссы, именованные мобы,",
                        "мобы из спавн-яиц и т.п.)."
                )
                .define("protectPersistentMobs", true);
        builder.pop();

        builder.comment(
                "Оптимизация взрывов динамита.",
                "При большом количестве одновременных взрывов сервер обрабатывает их",
                "частями — оставшийся динамит взрывается на следующий тик, а не теряется."
        ).push("tnt");
        TNT_OPTIMIZATIONS = builder.comment(
                        "Ограничивать число взрывов динамита, обрабатываемых за один тик.",
                        "Помогает против фризов от цепных реакций и «ТНТ-пушек»."
                )
                .define("enabled", true);
        MAX_TNT_EXPLOSIONS_PER_TICK = builder.comment(
                        "Максимум взрывов динамита за один тик сервера.",
                        "Лишний динамит не исчезает: он ждёт на последнем тике фитиля",
                        "и взрывается, как только освободится место."
                )
                .defineInRange("maxExplosionsPerTick", 12, 1, 128);
        builder.pop();

        builder.comment(
                "Ускорение создания нового мира.",
                "Ваниль ищет точку спавна спиралью по области 11×11 чанков и генерирует их",
                "синхронно. Меньший радиус заметно сокращает время создания мира."
        ).push("worldCreation");
        FAST_WORLD_CREATION = builder.comment(
                        "Уменьшить область поиска точки спавна при создании мира."
                )
                .define("enabled", true);
        SPAWN_SEARCH_RADIUS = builder.comment(
                        "Радиус поиска спавна в чанках (1 = 3×3, 3 = 7×7, 5 = ванильные 11×11).",
                        "Меньшие значения быстрее, но при спавне в океане берег может быть",
                        "чуть дальше от изначальной точки."
                )
                .defineInRange("spawnSearchRadius", 3, 1, 5);
        builder.pop();

        SPEC = builder.build();
    }

    private PerfCoreConfig() {
    }

    public static void bake() {
        try {
            enabled = ENABLED.get();
            freezeFarEntities = FREEZE_FAR_ENTITIES.get();
            freezeDistance = FREEZE_DISTANCE.get();
            logStats = LOG_STATS.get();
            aiThrottle = AI_THROTTLE.get();
            aiThrottleDistance = AI_THROTTLE_DISTANCE.get();
            aiTickInterval = AI_TICK_INTERVAL.get();
            protectPersistentMobs = PROTECT_PERSISTENT_MOBS.get();
            tntOptimizations = TNT_OPTIMIZATIONS.get();
            maxTntExplosionsPerTick = MAX_TNT_EXPLOSIONS_PER_TICK.get();
            fastWorldCreation = FAST_WORLD_CREATION.get();
            spawnSearchRadius = SPAWN_SEARCH_RADIUS.get();
        } catch (IllegalStateException notLoadedYet) {
            return;
        }
    }
}
