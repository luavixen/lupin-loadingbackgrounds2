package dev.foxgirl.mc.loadingbackgrounds;

import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public final class LoadingBackgrounds {

    public static final @NotNull Logger LOGGER = LogManager.getLogger("loadingbackgrounds");

    public static Config getConfig() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            try {
                return LoadingBackgroundsKt.getConfig();
            } catch (Throwable throwable) {
                LOGGER.error("Exception in getConfig", throwable);
            }
        } else {
            LOGGER.warn("Unexpected call to getConfig on server?");
        }
        return new Config();
    }

    public static boolean renderBackground(@NotNull Object screen, @NotNull Object gui) {
        if (Platform.getEnvironment() == Env.CLIENT) {
            try {
                return LoadingBackgroundsKt.renderBackground((Screen) screen, (GuiGraphics) gui);
            } catch (Throwable throwable) {
                LOGGER.error("Exception in renderBackground", throwable);
            }
        } else {
            LOGGER.warn("Unexpected call to renderBackground on server?");
        }
        return false;
    }

    public static void init() {
        if (Platform.getEnvironment() != Env.CLIENT) {
            try {
                LoadingBackgroundsKt.init();
            } catch (Throwable throwable) {
                LOGGER.error("Exception in init", throwable);
            }
        } else {
            LOGGER.warn("Unexpected call to init on client?");
        }
    }

    public static Void initCallable() {
        init();
        return null;
    }

}
