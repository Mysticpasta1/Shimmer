package com.lowdragmc.shimmer.core.mixins.rubidium;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.core.IRenderChunk;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

/**
 * @author KilaBash
 * @date 2022/05/28
 * @implNote ChunkRenderRebuildTaskMixin
 */
@Mixin(ChunkRenderRebuildTask.class)
public abstract class ChunkRenderRebuildTaskMixin {
    @Shadow @Final private RenderSection render;
    ImmutableList.Builder<ColorPointLight> lights;
    ImmutableList.Builder<ColorPointLight> lights2;

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
        ItemStack item = ItemStack.EMPTY;
        BlockPos itemEntityPos = BlockPos.ZERO;
        List<ItemEntity> itemEntityList = sync();
        if(itemEntityList != null) {
            for (ItemEntity itemEntity : itemEntityList) {
                item = itemEntity.getItem();
                itemEntityPos = new BlockPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ());
            }
        }

        if(LightManager.INSTANCE.isItemHasLight(item.getItem())) {
            if(item.getFrame() != null) {
                ColorPointLight light2 = LightManager.INSTANCE.getItemStackLight(itemEntityPos, item);
                if (light2 != null) {
                    lights2.add(light2);
                }
            }
        }

        if (LightManager.INSTANCE.isBlockHasLight(blockstate.getBlock(), fluidstate)) {
            ColorPointLight light = LightManager.INSTANCE.getBlockStateLight(instance, new BlockPos(x, y, z), blockstate, fluidstate);

            if (light != null) {
                lights.add(light);
            }
        }

        return blockstate;
    }

    @Inject(method = "performBuild", at = @At(value = "HEAD"), remap = false)
    private void injectCompilePre(ChunkBuildContext buildContext,
                                  CancellationSource cancellationSource,
                                  CallbackInfoReturnable<ChunkBuildResult> cir) {
        lights = ImmutableList.builder();
        lights2 = ImmutableList.builder();
    }

    @Inject(method = "performBuild", at = @At(value = "RETURN"), remap = false)
    private void injectCompilePost(ChunkBuildContext buildContext,
                                   CancellationSource cancellationSource,
                                   CallbackInfoReturnable<ChunkBuildResult> cir) {
        if (this.render instanceof IRenderChunk renderChunk) {
            renderChunk.setShimmerLights(lights.build());
            renderChunk.setShimmerLights(lights2.build());
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
