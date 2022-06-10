package com.lowdragmc.shimmer;

import com.lowdragmc.shimmer.client.ClientProxy;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

@Mod(ShimmerMod.MODID)
public class ShimmerMod {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "shimmer";

    public ShimmerMod() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> CommonProxy::new);
    }

    public static boolean isBiomeLightingLoaded() {return ModList.get().isLoaded("biomelighting");}
    public static boolean isRubidiumLoaded() {
        return ModList.get().isLoaded("rubidium");
    }
}
