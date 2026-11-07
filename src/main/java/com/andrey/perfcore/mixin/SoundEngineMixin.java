package com.andrey.perfcore.mixin;

import com.andrey.perfcore.client.PerfCoreClient;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void perfcore$limitSounds(SoundInstance sound, CallbackInfo ci) {
        if (!PerfCoreClient.allowSound(sound)) {
            ci.cancel();
        }
    }
}
