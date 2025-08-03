package dev.foxgirl.mc.loadingbackgrounds;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("loadingbackgrounds")
public final class LoadingBackgroundsForge {

    public LoadingBackgroundsForge() {
        EventBuses.registerModEventBus("loadingbackgrounds", FMLJavaModLoadingContext.get().getModEventBus());
        DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> LoadingBackgrounds::initCallable);
    }

}
