package com.andrey.perfcore.mixin;

import com.andrey.perfcore.client.PerfCoreClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ограничивает количество и дальность новых частиц.
 *
 * <p>Отсечение выполняется до добавления частицы в очередь рендера, поэтому
 * лишние объекты не создают нагрузку на тик и на отрисовку.</p>
 */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void perfcore$limitParticle(Particle particle, CallbackInfo callback) {
        if (!PerfCoreClient.allowParticle(particle)) {
            callback.cancel();
        }
    }
}
