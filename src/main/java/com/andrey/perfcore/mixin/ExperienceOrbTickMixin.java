package com.andrey.perfcore.mixin;

import com.andrey.perfcore.server.PerfCoreServer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbTickMixin {
    @Shadow
    private int age;

    @Unique
    private int perfcore$skippedTicks;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void perfcore$throttleOrb(CallbackInfo ci) {
        if (PerfCoreServer.shouldSkipRestingEntity((ExperienceOrb) (Object) this)) {
            perfcore$skippedTicks++;
            ci.cancel();
            return;
        }

        if (perfcore$skippedTicks > 0) {
            age += perfcore$skippedTicks;
            perfcore$skippedTicks = 0;
        }
    }
}
