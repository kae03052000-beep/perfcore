package com.andrey.perfcore.mixin;

import com.andrey.perfcore.PerfCoreConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Скип перестройки секций, которые гарантированно пустые (только воздух).
 *
 * <p>Это безопасная часть occlusion culling: пустая секция не может
 * содержать видимую геометрию, поэтому её перестройка бессмысленна.
 * На больших высотах (над облаками) и в океанах это экономит CPU.</p>
 *
 * <p><b>Полноценное occlusion culling</b> (скрытие чанков за стенами)
 * требует анализа {@code SectionOcclusionGraph} и тестов с шейдерами.
 * Запланировано на 1.3.</p>
 */
@Mixin(SectionRenderDispatcher.RenderSection.class)
public abstract class SmartChunkCullingMixin {

    @Shadow @Final private SectionPos sectionPos;

    @Inject(method = "rebuildAsync", at = @At("HEAD"), cancellable = true, require = 0)
    private void perfcore$skipEmptyRebuild(CallbackInfo ci) {
        if (!PerfCoreConfig.enabled || !PerfCoreConfig.smartChunkCulling) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int chunkX = sectionPos.x();
        int chunkZ = sectionPos.z();
        int sectionY = sectionPos.y();
        if (!level.hasChunk(chunkX, chunkZ)) {
            return;
        }
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        LevelChunkSection section = chunk.getSection(sectionY);
        if (section == null) {
            return;
        }
        if (!section.hasOnlyAir()) {
            return;
        }
        // Секция пустая и не содержит block entities — перестройка не нужна.
        ci.cancel();
    }
}
