package dev.foxgirl.mc.loadingbackgrounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class LoadingBackgrounds {

    public static final @NotNull Logger LOGGER = LogManager.getLogger("loadingbackgrounds");

    /**
     * Safely calls the {@link LoadingBackgroundsKt#getConfig()} method.
     */
    public static Config getConfig() {
        try {
            if (LoadedMod.getInstance().getEnvironment() == LoadedMod.Environment.CLIENT) {
                return LoadingBackgroundsKt.getConfig();
            } else {
                LOGGER.warn("Unexpected call to getConfig on server?");
            }
        } catch (Throwable throwable) {
            LOGGER.error("Exception in getConfig", throwable);
        }
        return new Config();
    }

    /**
     * Safely calls the {@link LoadingBackgroundsKt#renderBackground(Screen, GuiGraphics)} method.
     */
    public static boolean renderBackground(@NotNull Object screen, @NotNull Object gui) {
        try {
            if (LoadedMod.getInstance().getEnvironment() == LoadedMod.Environment.CLIENT) {
                return LoadingBackgroundsKt.renderBackground((Screen) screen, (GuiGraphics) gui);
            } else {
                LOGGER.warn("Unexpected call to renderBackground on server?");
            }
        } catch (Throwable throwable) {
            LOGGER.error("Exception in renderBackground", throwable);
        }
        return false;
    }

    /**
     * Safely calls the {@link LoadingBackgroundsKt#init()} method.
     */
    public static void init() {
        try {
            if (LoadedMod.getInstance().getEnvironment() == LoadedMod.Environment.CLIENT) {
                LoadingBackgroundsKt.init();
            } else {
                LOGGER.warn("Unexpected call to init on server?");
            }
        } catch (Throwable throwable) {
            LOGGER.error("Exception in init", throwable);
        }
    }

}
