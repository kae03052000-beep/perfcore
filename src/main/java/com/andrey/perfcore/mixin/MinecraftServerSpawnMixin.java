package com.andrey.perfcore.mixin;

import com.andrey.perfcore.PerfCoreConfig;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ускоряет создание нового мира за счёт меньшей области поиска спавна.
 *
 * <p>Ваниль перебирает 11×11 чанков и синхронно генерирует их, пока не найдёт
 * подходящую точку. С радиусом 3 достаточно 7×7 чанков — это до 2,5 раза
 * меньше генерации в худшем случае (океан/острова).</p>
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSpawnMixin {
    @ModifyArg(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;square(I)I"
            ),
            index = 0
    )
    private static int perfcore$shrinkSpawnSearch(int vanillaSide) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.fastWorldCreation) {
            return vanillaSide;
        }

        int radius = PerfCoreConfig.spawnSearchRadius;
        return radius * 2 + 1;
    }
}
