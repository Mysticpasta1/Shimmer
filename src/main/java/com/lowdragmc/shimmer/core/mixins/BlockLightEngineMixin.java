package com.lowdragmc.shimmer.core.mixins;

import com.lowdragmc.shimmer.client.EventListener;
import com.lowdragmc.shimmer.client.light.LightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author KilaBash
 * @date 2022/05/29
 * @implNote LevelChunkMixin
 */
@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineMixin {

    @Redirect(method = "getLightEmission", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getLightEmission(Lnet/minecraft/core/BlockPos;)I"))
    private int injectResize(BlockGetter instance, BlockPos pPos) {

        ItemStack item = ItemStack.EMPTY;

        ItemEntity itemEntity = EventListener.itemEntityHashMap.get(pPos);
        if(itemEntity != null) {
            item = itemEntity.getItem();
        }

        int light = LightManager.INSTANCE.getLight(instance, pPos, item.getItem());
        return light > 0 ? light : instance.getLightEmission(pPos);
    }

}
