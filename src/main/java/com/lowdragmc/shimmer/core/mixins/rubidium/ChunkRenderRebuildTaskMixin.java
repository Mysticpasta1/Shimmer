package com.lowdragmc.shimmer.core.mixins.rubidium;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.shimmer.client.EventListener;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.core.IRenderChunk;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote ChunkRenderRebuildTaskMixin
 */
@Mixin(ChunkRenderRebuildTask.class)
public abstract class ChunkRenderRebuildTaskMixin {
    private static ImmutableList.Builder<ColorPointLight> lights;
    @Shadow @Final private RenderSection render;

    @Redirect(method = "performBuild",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/world/WorldSlice;getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false
            )
    private BlockState injectCompile(WorldSlice instance, int x, int y, int z) {
        BlockPos pPos = new BlockPos(x, y, z);
        BlockState blockstate = instance.getBlockState(x, y, z);
        FluidState fluidstate = blockstate.getFluidState();

        ItemEntity itemEntity = EventListener.itemEntityHashMap.get(pPos);
        if(itemEntity != null) {
            ColorPointLight light2 = LightManager.INSTANCE.getItemStackLight(pPos, itemEntity.getItem());
            if(light2 != null) {
                lights.add(light2);
                light2.setPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ());
                light2.update();
            }
        }

        if (LightManager.INSTANCE.isBlockHasLight(blockstate.getBlock(), fluidstate)) {
            ColorPointLight light = LightManager.INSTANCE.getBlockStateLight(instance, new BlockPos(x, y, z), blockstate, fluidstate);
            if (light != null) {
                EventListener.lights.add(light);
            }
        }
        return blockstate;
    }

    @Inject(method = "performBuild", at = @At(value = "HEAD"), remap = false)
    private void injectCompilePre(ChunkBuildContext buildContext,
                                  CancellationSource cancellationSource,
                                  CallbackInfoReturnable<ChunkBuildResult> cir) {
        lights = ImmutableList.builder();
    }

    @Inject(method = "performBuild", at = @At(value = "RETURN"), remap = false)
    private void injectCompilePost(ChunkBuildContext buildContext,
                                   CancellationSource cancellationSource,
                                   CallbackInfoReturnable<ChunkBuildResult> cir) {
        if (this.render instanceof IRenderChunk renderChunk) {
            if(EventListener.lights != null) {
                renderChunk.setShimmerLights(lights.build());
            }
        }
    }
}
