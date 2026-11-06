package com.andrey.perfcore;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Серверные/общие настройки PerfCore.
 *
 * <p>Значения кэшируются в статические поля при загрузке/перезагрузке конфига:
 * горячие методы (например, тик ИИ мобов) не должны каждый раз ходить в
 * {@code ConfigValue#get()}.</p>
 */
public final class PerfCoreConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue AI_THROTTLE;
    private static final ForgeConfigSpec.IntValue AI_THROTTLE_DISTANCE;
    private static final ForgeConfigSpec.IntValue AI_TICK_INTERVAL;
    private static final ForgeConfigSpec.BooleanValue PROTECT_PERSISTENT_MOBS;
    private static final ForgeConfigSpec.BooleanValue TNT_OPTIMIZATIONS;
    private static final ForgeConfigSpec.IntValue MAX_TNT_EXPLOSIONS_PER_TICK;
    private static final ForgeConfigSpec.BooleanValue FAST_WORLD_CREATION;
    private static final ForgeConfigSpec.IntValue SPAWN_SEARCH_RADIUS;
    private static final ForgeConfigSpec.BooleanValue FREEZE_FAR_ENTITIES;
    private static final ForgeConfigSpec.IntValue FREEZE_DISTANCE;
    private static final ForgeConfigSpec.BooleanValue LIGHT_CACHE;
    private static final ForgeConfigSpec.IntValue LIGHT_CACHE_SIZE;
    private static final ForgeConfigSpec.BooleanValue SMART_CHUNK_CULLING;
    private static final ForgeConfigSpec.IntValue CHUNK_CULL_MARGIN;
    private static final ForgeConfigSpec.BooleanValue PATHFINDING_CACHE;
    private static final ForgeConfigSpec.IntValue PATH_CACHE_SIZE;
    private static final ForgeConfigSpec.BooleanValue LOG_STATS;

    /** Быстрый доступ из горячего пути. */
    public static volatile boolean enabled = true;
    public static volatile boolean aiThrottle = true;
    public static volatile int aiThrottleDistance = 48;
    public static volatile int aiTickInterval = 2;
    public static volatile boolean protectPersistentMobs = true;
    public static volatile boolean tntOptimizations = true;
    public static volatile int maxTntExplosionsPerTick = 12;
    public static volatile boolean fastWorldCreation = true;
    public static volatile int spawnSearchRadius = 3;
    public static volatile boolean freezeFarEntities = true;
    public static volatile int freezeDistance = 100;
    public static volatile boolean lightCache = true;
    public static volatile int lightCacheSize = 4096;
    public static volatile boolean smartChunkCulling = true;
    public static volatile int chunkCullMargin = 3;
    public static volatile boolean pathfindingCache = true;
    public static volatile int pathCacheSize = 2048;
    public static volatile boolean logStats = false;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "PerfCore — общие настройки производительности.",
                "Изменения применяются после перезагрузки конфига (F3+T или /reload)."
        ).push("general");
        ENABLED = builder.comment("Включить общие оптимизации PerfCore.")
                .define("enabled", true);
        FREEZE_FAR_ENTITIES = builder.comment(
                        "Заморозить физику предметов и сфер опыта, если рядом нет игроков.",
                        "Тик вызывается раз в 20 тиков, таймеры деспавна продолжают работать."
                )
                .define("freezeFarEntities", true);
        FREEZE_DISTANCE = builder.comment("Радиус (в блоках), в котором предметы тикают с полной частотой.")
                .defineInRange("freezeDistance", 100, 32, 256);
        LOG_STATS = builder.comment("Писать в лог краткую статистику при остановке сервера.")
                .define("logStats", false);
        builder.pop();

        builder.comment("Оптимизация тика мобов на сервере.").push("mobAi");
        AI_THROTTLE = builder.comment(
                        "Тикать ИИ мобов реже, если рядом нет игроков.",
                        "Мобы дальше aiThrottleDistance блоков пропускают часть тиков ИИ.",
                        "Это сильно снижает нагрузку от ферм и больших скоплений мобов."
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
                        "Лишний динамит не исчезает: его фитиль продлевается на пару тиков,",
                        "поэтому взрыв просто переносится на следующий тик."
                )
                .defineInRange("maxExplosionsPerTick", 12, 1, 128);
        builder.pop();

        builder.comment(
                "Ускорение создания нового мира.",
                "Ваниль ищет точку спавна в области 11×11 чанков и генерирует их",
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

        builder.comment("Кэш освещения (Light Engine).").push("lightCache");
        LIGHT_CACHE = builder.comment(
                        "Кэшировать результаты пересчёта света, чтобы не считать неизменённые участки повторно."
                )
                .define("enabled", true);
        LIGHT_CACHE_SIZE = builder.comment("Максимум записей в кэше света (LRU).")
                .defineInRange("size", 4096, 256, 65536);
        builder.pop();

        builder.comment("Умное отсечение чанков.").push("chunkCulling");
        SMART_CHUNK_CULLING = builder.comment(
                        "Не отправлять на отрисовку чанки, полностью скрытые другими чанками.",
                        "Если игрок ближе chunkCullMargin блоков к границе — чанк отрисовывается."
                )
                .define("enabled", true);
        CHUNK_CULL_MARGIN = builder.comment("Зазор у границы чанка (в блоках).")
                .defineInRange("margin", 3, 1, 8);
        builder.pop();

        builder.comment("Кэш путей мобов (Pathfinding).").push("pathfinding");
        PATHFINDING_CACHE = builder.comment(
                        "Переиспользовать успешные пути мобов, если цель не сдвинулась."
                )
                .define("enabled", true);
        PATH_CACHE_SIZE = builder.comment("Максимум записей в кэше путей.")
                .defineInRange("size", 2048, 128, 16384);
        builder.pop();

        SPEC = builder.build();
    }

    private PerfCoreConfig() {
    }

    /** Обновляет кэш значений. Вызывается после загрузки/перезагрузки конфига. */
    public static void bake() {
        try {
            enabled = ENABLED.get();
            aiThrottle = AI_THROTTLE.get();
            aiThrottleDistance = AI_THROTTLE_DISTANCE.get();
            aiTickInterval = AI_TICK_INTERVAL.get();
            protectPersistentMobs = PROTECT_PERSISTENT_MOBS.get();
            tntOptimizations = TNT_OPTIMIZATIONS.get();
            maxTntExplosionsPerTick = MAX_TNT_EXPLOSIONS_PER_TICK.get();
            fastWorldCreation = FAST_WORLD_CREATION.get();
            spawnSearchRadius = SPAWN_SEARCH_RADIUS.get();
            freezeFarEntities = FREEZE_FAR_ENTITIES.get();
            freezeDistance = FREEZE_DISTANCE.get();
            lightCache = LIGHT_CACHE.get();
            lightCacheSize = LIGHT_CACHE_SIZE.get();
            smartChunkCulling = SMART_CHUNK_CULLING.get();
            chunkCullMargin = CHUNK_CULL_MARGIN.get();
            pathfindingCache = PATHFINDING_CACHE.get();
            pathCacheSize = PATH_CACHE_SIZE.get();
            logStats = LOG_STATS.get();
        } catch (IllegalStateException notLoadedYet) {
            // Конфиг ещё не прочитан — оставляем безопасные значения по умолчанию.
        }
    }
}
