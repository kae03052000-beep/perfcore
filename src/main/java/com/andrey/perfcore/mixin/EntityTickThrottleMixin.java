package com.andrey.perfcore.mixin;

import com.andrey.perfcore.server.PerfCoreServer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class EntityTickThrottleMixin {

    @Mixin(ItemEntity.class)
    public static abstract class Item {
        @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
        private void perfcore$throttleItem(CallbackInfo ci) {
            if (PerfCoreServer.shouldSkipEntityTick((ItemEntity) (Object) this)) {
                ci.cancel();
            }
        }
    }

    @Mixin(ExperienceOrb.class)
    public static abstract class Orb {
        @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
        private void perfcore$throttleOrb(CallbackInfo ci) {
            if (PerfCoreServer.shouldSkipEntityTick((ExperienceOrb) (Object) this)) {
                ci.cancel();
            }
        }
    }
}
