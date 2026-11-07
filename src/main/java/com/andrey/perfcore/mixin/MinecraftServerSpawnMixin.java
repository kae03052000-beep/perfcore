package com.andrey.perfcore.mixin;

import com.andrey.perfcore.PerfCoreConfig;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerSpawnMixin {
    private static final int VANILLA_SEARCH_SIDE = 11;

    @ModifyArg(
            method = "setInitialSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;square(I)I"
            ),
            index = 0,
            require = 0
    )
    private static int perfcore$shrinkSpawnSearch(int vanillaSide) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.fastWorldCreation) {
            return vanillaSide;
        }
        if (vanillaSide != VANILLA_SEARCH_SIDE) {
            return vanillaSide;
        }

        return PerfCoreConfig.spawnSearchRadius * 2 + 1;
    }
}
