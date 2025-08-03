package dev.foxgirl.mc.loadingbackgrounds;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod("loadingbackgrounds")
public final class LoadingBackgroundsNeoForge {

    public LoadingBackgroundsNeoForge(IEventBus eventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            LoadingBackgrounds.init();
        }
    }

}
