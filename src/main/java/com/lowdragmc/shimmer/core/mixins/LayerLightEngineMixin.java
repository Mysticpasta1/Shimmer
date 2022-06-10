package com.lowdragmc.shimmer.core.mixins;

import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LayerLightEngine.class)
public class LayerLightEngineMixin {
    @Shadow @Final protected LightChunkGetter chunkSource;
}