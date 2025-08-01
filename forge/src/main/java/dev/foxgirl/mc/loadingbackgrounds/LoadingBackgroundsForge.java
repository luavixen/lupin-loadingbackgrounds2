package dev.foxgirl.mc.loadingbackgrounds;

import dev.foxgirl.mc.loadingbackgrounds.LoadingBackgrounds;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LoadingBackgrounds.MOD_ID)
public final class LoadingBackgroundsForge {
    public LoadingBackgroundsForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(LoadingBackgrounds.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        LoadingBackgrounds.init();
    }
}
