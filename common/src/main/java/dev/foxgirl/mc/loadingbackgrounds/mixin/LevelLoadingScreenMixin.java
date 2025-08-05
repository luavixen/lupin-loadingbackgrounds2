package dev.foxgirl.mc.loadingbackgrounds.mixin;

import dev.foxgirl.mc.loadingbackgrounds.Config;
import dev.foxgirl.mc.loadingbackgrounds.LoadingBackgrounds;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * This mixin adjusts the position of the level loading progress widget based on the Loading Backgrounds configuration.
 */
@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    private LevelLoadingScreenMixin(Component title) {
        super(title);
    }

    @Shadow @Final
    private StoringChunkProgressListener progressListener;

    // Adjust X position:

    @ModifyVariable(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At("STORE"), ordinal = 2
    )
    private int loadingbackgrounds$render$0(int x) {
        var position = LoadingBackgrounds.getConfig().getPosition();
        if (position != Config.Position.CENTER) {
            int width = this.width;
            int diameter = progressListener.getDiameter();

            switch (position.ordinal()) {
                case 1:
                case 3:
                    return diameter + (diameter / 4);
                case 2:
                case 4:
                    return width - diameter - (diameter / 4);
            }
        }

        return x;
    }

    // Adjust Y position:

    @ModifyVariable(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
        at = @At("STORE"), ordinal = 3
    )
    private int loadingbackgrounds$render$1(int y) {
        var position = LoadingBackgrounds.getConfig().getPosition();
        if (position != Config.Position.CENTER) {
            int height = this.height;
            int diameter = progressListener.getDiameter();

            switch (position.ordinal()) {
                case 1:
                case 2:
                    return diameter + (diameter / 4) + 15;
                case 3:
                case 4:
                    return height - diameter - (diameter / 4);
            }
        }

        return y;
    }

}
