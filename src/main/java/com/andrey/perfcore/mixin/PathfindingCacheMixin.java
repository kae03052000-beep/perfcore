package com.andrey.perfcore.mixin;

import com.andrey.perfcore.PerfCoreConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Троттлинг пересчёта пути мобов.
 *
 * <p>При преследовании игрока {@code recomputePath()} вызывается каждый тик,
 * потому что цель движется. Каждый вызов — BFS по сетке блоков через
 * {@code PathFinder}, что очень дорого. Ограничение до 1 раза в 2 тика
 * снижает нагрузку, не ломая поведение: у моба уже есть валидный путь.</p>
 *
 * <p>Интервал 2 тика = 100 мс — незаметно для игрока, но экономит ~50%
 * вызовов pathfinding.</p>
 */
@Mixin(PathNavigation.class)
public abstract class PathfindingCacheMixin {

    @Shadow @Final protected Mob mob;

    @Unique private int perfcore$lastRecomputeTick = Integer.MIN_VALUE;

    @Inject(method = "recomputePath", at = @At("HEAD"), cancellable = true, require = 0)
    private void perfcore$throttleRecompute(CallbackInfo ci) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.pathfindingCache) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel)) {
            return;
        }
        int now = mob.tickCount;
        if (now - perfcore$lastRecomputeTick < 2) {
            ci.cancel();
            return;
        }
        perfcore$lastRecomputeTick = now;
    }
}
