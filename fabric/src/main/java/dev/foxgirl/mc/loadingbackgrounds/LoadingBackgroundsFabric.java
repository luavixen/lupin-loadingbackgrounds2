package dev.foxgirl.mc.loadingbackgrounds;

import net.fabricmc.api.ClientModInitializer;

public final class LoadingBackgroundsFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LoadingBackgroundsKt.init();
    }

}
