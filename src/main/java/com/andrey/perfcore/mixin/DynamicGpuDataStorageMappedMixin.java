package com.andrey.perfcore.mixin;

import com.andrey.perfcore.client.PerfCoreClientConfig;
import net.minecraft.client.renderer.DynamicGpuDataStorageMapped;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Увеличивает начальную ёмкость mapped GPU-буферов.
 *
 * <p>В Minecraft 26.3 такие буферы создаются с ёмкостью 1–2 блока и затем
 * многократно пересоздаются в течение одного кадра. Это вызывает
 * переаллокации памяти и спам {@code Resizing ...} в логе.</p>
 */
@Mixin(DynamicGpuDataStorageMapped.class)
public abstract class DynamicGpuDataStorageMappedMixin {
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
