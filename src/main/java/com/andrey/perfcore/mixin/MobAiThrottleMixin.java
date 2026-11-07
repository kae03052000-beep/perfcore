package com.andrey.perfcore.mixin;

import com.andrey.perfcore.server.PerfCoreServer;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobAiThrottleMixin {
    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void perfcore$throttleServerAi(CallbackInfo callback) {
        if (PerfCoreServer.shouldSkipAi((Mob) (Object) this)) {
            callback.cancel();
        }
    }
}
