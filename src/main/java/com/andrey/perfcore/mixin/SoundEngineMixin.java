package com.andrey.perfcore.mixin;

import com.andrey.perfcore.client.PerfCoreClientConfig;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    private static int perfcore$soundsThisTick = 0;

    public static void resetCounter() {
        perfcore$soundsThisTick = 0;
    }

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void perfcore$limitSounds(SoundInstance sound, CallbackInfo ci) {
        if (PerfCoreClientConfig.soundThrottling) {
            if (perfcore$soundsThisTick >= PerfCoreClientConfig.maxSoundsPerTick) {
                ci.cancel();
            } else {
                perfcore$soundsThisTick++;
            }
        }
    }
}
