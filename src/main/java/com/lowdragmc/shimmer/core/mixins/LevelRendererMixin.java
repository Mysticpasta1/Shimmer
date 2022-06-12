package com.lowdragmc.shimmer.core.mixins;

import com.lowdragmc.shimmer.client.EventListener;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.client.postprocessing.PostProcessing;
import com.lowdragmc.shimmer.core.IRenderChunk;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.nio.FloatBuffer;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote LevelRendererMixin, used to inject level renderer, for block, entity, particle postprocessing.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Nullable private ClientLevel level;

    @Shadow @Final private ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum;

    @Inject(method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z"))
    private void injectRenderLevel(PoseStack poseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera camera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.level.getProfiler().popPush("block_bloom");
        PostProcessing.getBlockBloom().renderBlockPost();
    }

    @Inject(method = "renderChunkLayer",
            at = @At(value = "HEAD"))
    private void preRenderChunkLayer(RenderType pRenderType,
                                        PoseStack pPoseStack, double pCamX,
                                        double pCamY, double pCamZ,
                                        Matrix4f pProjectionMatrix,
                                        CallbackInfo ci) {
        GL30.glDrawBuffers(new int[] {GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
    }

    @Inject(method = "renderChunkLayer",
            at = @At(value = "RETURN"))
    private void postRenderChunkLayer(RenderType pRenderType,
                                        PoseStack pPoseStack, double pCamX,
                                        double pCamY, double pCamZ,
                                        Matrix4f pProjectionMatrix,
                                        CallbackInfo ci) {
        GL30.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
    }

    @Inject(method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
                    ordinal = 1))
    private void injectRenderLevelBloom(PoseStack poseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera camera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f projectionMatrix, CallbackInfo ci) {
        ProfilerFiller profilerFiller = this.level.getProfiler();
        for (PostProcessing postProcessing : PostProcessing.values()) {
            postProcessing.renderEntityPost(profilerFiller);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "HEAD"))
    private void injectRenderLevelPre(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        Vec3 position = pCamera.getPosition();
        int blockLightSize = 0;
        int left = LightManager.INSTANCE.leftLightCount();
        FloatBuffer buffer = LightManager.INSTANCE.getBuffer();
        buffer.clear();
        for (LevelRenderer.RenderChunkInfo chunkInfo : renderChunksInFrustum) {
            if (left <= blockLightSize) {
                break;
            }
            if (chunkInfo.chunk instanceof IRenderChunk) {
                for (ColorPointLight shimmerLight : ((IRenderChunk) chunkInfo.chunk).getShimmerLights()) {
                    if (left <= blockLightSize) {
                        break;
                    }
                    shimmerLight.uploadBuffer(buffer);
                    blockLightSize++;
                }
            }
        }
        buffer.flip();
        LightManager.INSTANCE.renderLevelPre(blockLightSize, (float)position.x,(float) position.y, (float)position.z);
    }

    @Inject(method = "renderLevel", at = @At(value = "RETURN"))
    private void injectRenderLevelPost(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix, CallbackInfo ci) {
        LightManager.INSTANCE.renderLevelPost();
    }

    @Inject(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("TAIL"), cancellable = true)
    private static void getLightingColor(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos, CallbackInfoReturnable<Integer> cir) {

        ColorPointLight itemLight = null;
        ItemEntity itemEntity = EventListener.itemEntityHashMap.get(pPos);
        if(itemEntity != null) {
            if (LightManager.INSTANCE.isItemHasLight(itemEntity.getItem().getItem())) {
                itemLight = LightManager.INSTANCE.getItemStackLight(pPos, itemEntity.getItem());
            }
        }
        if(itemLight != null) {
            cir.setReturnValue(itemLight.getColor((int) itemLight.r, (int) itemLight.g, (int) itemLight.b, (int) itemLight.a));
        }
    }
}
