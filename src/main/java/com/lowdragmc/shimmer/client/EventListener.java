package com.lowdragmc.shimmer.client;

import com.google.common.collect.ImmutableList;
import com.lowdragmc.shimmer.ShimmerMod;
import com.lowdragmc.shimmer.client.light.ColorPointLight;
import com.lowdragmc.shimmer.client.light.LightManager;
import com.lowdragmc.shimmer.client.postprocessing.PostProcessing;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author KilaBash
 * @date 2022/5/12
 * @implNote EventListener
 */
@Mod.EventBusSubscriber(modid = ShimmerMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class EventListener {
    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() == Minecraft.getInstance().level) {
            LightManager.clear();
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("shimmer")
                .then(Commands.literal("reload_postprocessing")
                        .executes(context -> {
                            for (PostProcessing post : PostProcessing.values()) {
                                post.onResourceManagerReload(null);
                            }
                            return 1;
                        }))
                .then(Commands.literal("clear_lights")
                        .executes(context -> {
                            LightManager.clear();
                            return 1;
                        })));
    }

    public static Map<BlockPos, ItemEntity> itemEntityHashMap = new HashMap<>();
    public static ImmutableList.Builder<ColorPointLight> lights;
    private static ColorPointLight light2;
    public static int light = 0;

    @SubscribeEvent
    public static void onEntityJoinWorldEvent(EntityJoinWorldEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            BlockPos blockPos1 = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
            AABB aabb = new AABB(blockPos1.offset(-15, -15, -15), blockPos1.offset(15, 15, 15));
            List<ItemEntity> itemEntityList = event.getWorld().getEntitiesOfClass(ItemEntity.class, aabb, itemEntity -> true);
            for (ItemEntity itemEntity : itemEntityList) {
                BlockPos blockPos = new BlockPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ());
                if (LightManager.INSTANCE.isItemHasLight(itemEntity.getItem().getItem())) {
                    itemEntityHashMap.put(blockPos, itemEntity);
                    ItemEntity itemEntity1 = EventListener.itemEntityHashMap.get(blockPos);
                    if (itemEntity1 != null) {
                        if (LightManager.INSTANCE.isItemHasLight(itemEntity1.getItem().getItem())) {
                            light2 = LightManager.INSTANCE.getItemStackLight(blockPos, itemEntity.getItem());
                            if (light2 != null) {
                                lights.add(light2);
                                light2.update();
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        int light1 = 0;
        ColorPointLight lightStack1 = null;
        Player player = Minecraft.getInstance().player;
        if(player != null) {
            BlockPos blockPos1 = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
            AABB aabb = new AABB(blockPos1.offset(-15, -15, -15), blockPos1.offset(15, 15, 15));
            List<ItemEntity> itemEntityList = event.world.getEntitiesOfClass(ItemEntity.class, aabb, itemEntity -> true);
            for (ItemEntity itemEntity : itemEntityList) {
                if(itemEntity != null) {
                light1 = LightManager.INSTANCE.getItemLight(itemEntity.getItem().getItem(), blockPos1);
                }
            }
        }
        light = light1;
        if (light2 != null) {
            light2.update();
        }
    }

    @SubscribeEvent
    public static void onEntityLeaveWorldEvent(EntityLeaveWorldEvent event){
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            BlockPos blockPos1 = new BlockPos(player.getBlockX(), player.getBlockY(), player.getBlockZ());
            AABB aabb = new AABB(blockPos1.offset(-15, -15, -15), blockPos1.offset(15, 15, 15));
            List<ItemEntity> itemEntityList = event.getWorld().getEntitiesOfClass(ItemEntity.class, aabb, itemEntity -> true);
            for (ItemEntity itemEntity : itemEntityList) {
                BlockPos blockPos = new BlockPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ());
                if (itemEntityHashMap.containsKey(blockPos)) {
                    itemEntityHashMap.remove(new BlockPos(itemEntity.getBlockX(), itemEntity.getBlockY(), itemEntity.getBlockZ()), itemEntity);
                }
                if(light2 != null) {
                    light2.remove();
                    light2.update();
                }
            }
        }
    }
}

