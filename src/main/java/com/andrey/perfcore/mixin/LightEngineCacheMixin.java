package com.andrey.perfcore.mixin;

import com.andrey.perfcore.PerfCoreConfig;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Дедупликация обновлений освещения в пределах одного тика.
 *
 * <p>При взрывах и {@code /fill} {@code checkBlock} вызывается тысячи раз,
 * часто для одной и той же позиции с тем же состоянием блока. Если состояние
 * не изменилось с прошлого вызова в этом тике, свет пересчитывать не нужно.</p>
 *
 * <p>Кэш живёт один игровой тик и чистится при смене {@code Level#getGameTime()}.
 * На интегрированном сервере (singleplayer) серверный и клиентский уровни
 * имеют независимые {@code LevelLightEngine}, поэтому конфликтов нет.</p>
 *
 * <p><b>Ограничение:</b> если соседний блок изменился, но сам блок — нет,
 * дедупликация может пропустить пересчёт. Это может дать кратковременный
 * (1–2 тика) визуальный артефакт света во время взрывов, но не ломает
 * распространение света в целом. При обнаружении артефактов уменьшите
 * {@code lightCacheSize} или отключите {@code lightCache} в конфиге.</p>
 */
@Mixin(LevelLightEngine.class)
public abstract class LightEngineCacheMixin {

    @Unique private LightChunkGetter perfcore$chunkSource;
    @Unique private final Long2IntMap perfcore$stateHashes = new Long2IntOpenHashMap();
    @Unique private long perfcore$lastTick = Long.MIN_VALUE;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void perfcore$captureCtor(LightChunkGetter lightChunkGetter,
                                      boolean blockLight,
                                      boolean skyLight,
                                      CallbackInfo ci) {
        this.perfcore$chunkSource = lightChunkGetter;
    }

    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true)
    private void perfcore$dedupCheckBlock(BlockPos pos, CallbackInfo ci) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.lightCache) return;
        if (perfcore$chunkSource == null) return;

        BlockGetter level = perfcore$chunkSource.getLevel();
        if (!(level instanceof Level lvl)) return;

        long now = lvl.getGameTime();
        if (now != perfcore$lastTick) {
            perfcore$lastTick = now;
            perfcore$stateHashes.clear();
        }

        BlockState state = lvl.getBlockState(pos);
        int stateId = state.hashCode();
        long key = pos.asLong();

        if (perfcore$stateHashes.containsKey(key) && perfcore$stateHashes.get(key) == stateId) {
            ci.cancel();
            return;
        }

        if (perfcore$stateHashes.size() >= PerfCoreConfig.lightCacheSize) {
            perfcore$stateHashes.clear();
        }
        perfcore$stateHashes.put(key, stateId);
    }
}
