package dev.foxgirl.mc.loadingbackgrounds;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LoadingBackgroundsKt.MOD_ID)
public final class LoadingBackgroundsForge {

    public LoadingBackgroundsForge() {
        EventBuses.registerModEventBus(LoadingBackgroundsKt.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        LoadingBackgroundsKt.init();
    }

}
