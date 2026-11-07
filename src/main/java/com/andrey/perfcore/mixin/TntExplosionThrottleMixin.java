package com.andrey.perfcore.mixin;

import com.andrey.perfcore.server.PerfCoreServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class TntExplosionThrottleMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void perfcore$deferExplosion(CallbackInfo ci) {
        PrimedTnt tnt = (PrimedTnt) (Object) this;
        if (!(tnt.level() instanceof ServerLevel)) {
            return;
        }
        if (tnt.getFuse() <= 1 && !PerfCoreServer.tryReserveTntExplosion()) {
            if (tnt.getFuse() < 1) {
                tnt.setFuse(1);
            }
            ci.cancel();
        }
    }
}
