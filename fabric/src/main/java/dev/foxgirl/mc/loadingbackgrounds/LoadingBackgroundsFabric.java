package dev.foxgirl.mc.loadingbackgrounds;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class LoadingBackgroundsFabric extends LoadedMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LoadingBackgroundsKt.init();
    }

    @Override
    public Environment getEnvironment() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT
            ? Environment.CLIENT : Environment.SERVER;
    }

    @Override
    public Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir();
    }

}
