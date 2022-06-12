package com.lowdragmc.shimmer.core.mixins;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.shimmer.client.EventListener;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.core.IRenderChunk;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote RebuildTaskMixin, used to compile and save light info to the chunk.
 */
@Mixin(targets = {"net/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask"})
public abstract class RebuildTaskMixin {
    @Shadow(aliases = {"this$1", "f_112859_"}) @Final ChunkRenderDispatcher.RenderChunk this$1;

    @Redirect(method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 0))
    private BlockState injectCompile(RenderChunkRegion instance, BlockPos pPos) {
        BlockState blockstate = instance.getBlockState(pPos);
        FluidState fluidstate = blockstate.getFluidState();

        if (LightManager.INSTANCE.isBlockHasLight(blockstate.getBlock(), fluidstate)) {
            ColorPointLight light = LightManager.INSTANCE.getBlockStateLight(instance, pPos, blockstate, fluidstate);
            if (light != null) {
                EventListener.lights.add(light);
            }
        }
        return blockstate;
    }

    @Inject(method = "compile", at = @At(value = "HEAD"))
    private void injectCompilePre(float pX, float pY, float pZ, ChunkRenderDispatcher.CompiledChunk pCompiledChunk, ChunkBufferBuilderPack pBuffers, CallbackInfoReturnable<Set<BlockEntity>> cir) {
        EventListener.lights = ImmutableList.builder();
    }

    @Inject(method = "compile", at = @At(value = "RETURN"))
    private void injectCompilePost(float pX, float pY, float pZ, ChunkRenderDispatcher.CompiledChunk pCompiledChunk, ChunkBufferBuilderPack pBuffers, CallbackInfoReturnable<Set<BlockEntity>> cir) {
        if (this$1 instanceof IRenderChunk) {
            if(EventListener.lights != null) {
                ((IRenderChunk) this$1).setShimmerLights(EventListener.lights.build());
            }
        }
    }
}
