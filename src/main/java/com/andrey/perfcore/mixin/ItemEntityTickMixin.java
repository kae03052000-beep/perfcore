package com.andrey.perfcore.mixin;

import com.andrey.perfcore.server.PerfCoreServer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityTickMixin {
    @Shadow
    private int age;

    @Unique
    private int perfcore$skippedTicks;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void perfcore$throttleItem(CallbackInfo ci) {
        if (PerfCoreServer.shouldSkipRestingEntity((ItemEntity) (Object) this)) {
            perfcore$skippedTicks++;
            ci.cancel();
            return;
        }

        if (perfcore$skippedTicks > 0) {
            if (age != Short.MIN_VALUE) {
                age += perfcore$skippedTicks;
            }
            perfcore$skippedTicks = 0;
        }
    }
}
