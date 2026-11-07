package com.andrey.perfcore.mixin;

import com.andrey.perfcore.client.PerfCoreClientConfig;
import net.minecraft.client.renderer.DynamicGpuDataStorageNonMapped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DynamicGpuDataStorageNonMapped.class)
public abstract class DynamicGpuDataStorageNonMappedMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/util/Mth;smallestEncompassingPowerOfTwo(I)I"
            ),
            index = 0
    )
    private int perfcore$preSizeBuffer(int initialCapacity) {
        return PerfCoreClientConfig.effectiveGpuBufferCapacity(initialCapacity);
    }
}
