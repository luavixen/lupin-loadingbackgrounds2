package dev.foxgirl.mc.loadingbackgrounds;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents this mod as loaded by the mod loader.
 * Provides various abstract methods that are implemented differently for each mod loader.
 */
public abstract class LoadedMod {

    private static LoadedMod INSTANCE;

    public LoadedMod() {
        if (INSTANCE != null) throw new IllegalStateException("LoadedMod INSTANCE already set");
        INSTANCE = this;
    }

    /**
     * Returns the current {@link LoadedMod} instance.
     */
    public static LoadedMod getInstance() {
        return Objects.requireNonNull(INSTANCE, "LoadedMod INSTANCE is null");
    }

    /**
     * Represents the environment type of the running game instance.
     */
    public enum Environment {
        CLIENT,
        SERVER,
    }

    /**
     * Returns the current environment type.
     */
    public abstract Environment getEnvironment();

    /**
     * Returns the path of the config directory.
     */
    public abstract Path getConfigPath();

}
