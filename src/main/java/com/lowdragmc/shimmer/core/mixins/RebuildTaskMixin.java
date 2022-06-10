package com.lowdragmc.shimmer.core.mixins;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.core.IRenderChunk;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

/**
 * @author KilaBash
 * @date 2022/05/02
 * @implNote RebuildTaskMixin, used to compile and save light info to the chunk.
 */
@Mixin(targets = {"net/minecraft/client/renderer/chunk/ChunkRenderDispatcher$RenderChunk$RebuildTask"})
public abstract class RebuildTaskMixin {
    @Shadow(aliases = {"this$1", "f_112859_"}) @Final ChunkRenderDispatcher.RenderChunk this$1;
    ImmutableList.Builder<ColorPointLight> lights;
    ImmutableList.Builder<ColorPointLight> lights2;

    @Redirect(method = "compile",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    ordinal = 0))
    private BlockState injectCompile(RenderChunkRegion instance, BlockPos pPos) {
        BlockState blockstate = instance.getBlockState(pPos);
        FluidState fluidstate = blockstate.getFluidState();
        ItemStack item = ItemStack.EMPTY;
        BlockPos itemEntityPos = BlockPos.ZERO;
        List<ItemEntity> itemEntityList = sync();
        if(itemEntityList != null) {
            for (ItemEntity itemEntity : itemEntityList) {
                item = itemEntity.getItem();
                itemEntityPos = new BlockPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ());
            }
        }
        if (LightManager.INSTANCE.isItemHasLight(item.getItem())) {
            ColorPointLight light2 = LightManager.INSTANCE.getItemStackLight(itemEntityPos, item);
            if (light2 != null) {
                lights2.add(light2);
            }
        }

        if (LightManager.INSTANCE.isBlockHasLight(blockstate.getBlock(), fluidstate)) {
            ColorPointLight light = LightManager.INSTANCE.getBlockStateLight(instance, pPos, blockstate, fluidstate);

            if (light != null) {
                lights.add(light);
            }
        }
        return blockstate;
    }

    @Inject(method = "compile", at = @At(value = "HEAD"))
    private void injectCompilePre(float pX, float pY, float pZ, ChunkRenderDispatcher.CompiledChunk pCompiledChunk, ChunkBufferBuilderPack pBuffers, CallbackInfoReturnable<Set<BlockEntity>> cir) {
        lights = ImmutableList.builder();
        lights2 = ImmutableList.builder();
    }

    @Inject(method = "compile", at = @At(value = "RETURN"))
    private void injectCompilePost(float pX, float pY, float pZ, ChunkRenderDispatcher.CompiledChunk pCompiledChunk, ChunkBufferBuilderPack pBuffers, CallbackInfoReturnable<Set<BlockEntity>> cir) {
        if (this$1 instanceof IRenderChunk) {
            ((IRenderChunk) this$1).setShimmerLights(lights.build());
            ((IRenderChunk) this$1).setShimmerLights(lights2.build());
        }
    }

    public synchronized List<ItemEntity> sync() {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            if (Minecraft.getInstance().player != null) {
                BlockPos blockPos = new BlockPos(Minecraft.getInstance().player.getBlockX(), Minecraft.getInstance().player.getBlockY(), Minecraft.getInstance().player.getBlockZ());
                AABB aabb = new AABB(blockPos.offset(-15, -15, -15), blockPos.offset(15, 15, 15));
                    return level.getEntities(new EntityTypeTest<>() {
                        @Override
                        public @NotNull ItemEntity tryCast(@NotNull Entity pEntity) {
                            return (ItemEntity) pEntity;
                        }

                        @Override
                        public @NotNull Class<? extends Entity> getBaseClass() {
                            return ItemEntity.class;
                        }
                    }, aabb, itemEntity -> true);
                }
            }
        return null;
    }
}
