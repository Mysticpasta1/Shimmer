package com.lowdragmc.shimmer.core.mixins;

import com.lowdragmc.shimmer.client.light.LightManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * @author KilaBash
 * @date 2022/05/29
 * @implNote LevelChunkMixin
 */
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin extends LayerLightEngineMixin{

    @Redirect(method = "getLightEmission", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getLightEmission(Lnet/minecraft/core/BlockPos;)I"))
    private int injectResize(BlockGetter instance, BlockPos pPos) {
        ItemStack item = ItemStack.EMPTY;
        List<ItemEntity> itemEntityList = sync();
        if(itemEntityList != null) {
            for (ItemEntity itemEntity : itemEntityList) {
                item = itemEntity.getItem();
                pPos = new BlockPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ());
            }
        }
        int light = LightManager.INSTANCE.getLight(instance, pPos, item.getItem());
        return light > 0 ? light : instance.getLightEmission(pPos);
    }

    public synchronized List<ItemEntity> sync() {
        Level level = (Level) chunkSource.getLevel();
        if (level != null && Minecraft.getInstance().player != null) {
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
        return null;
    }
}
