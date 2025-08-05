package dev.foxgirl.mc.loadingbackgrounds;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

@Mod("loadingbackgrounds")
public final class LoadingBackgroundsNeoForge extends LoadedMod {

    public LoadingBackgroundsNeoForge(IEventBus eventBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            LoadingBackgrounds.init();
        }
    }

    @Override
    public Environment getEnvironment() {
        return FMLEnvironment.dist == Dist.CLIENT
            ? Environment.CLIENT : Environment.SERVER;
    }

    @Override
    public Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }

}
