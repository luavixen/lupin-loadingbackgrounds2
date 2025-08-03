package dev.foxgirl.mc.loadingbackgrounds.mixin;

import dev.foxgirl.mc.loadingbackgrounds.LoadingBackgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    /*
    From 1.20 to 1.20.4 there is only a simple tiled dirt background.
    Replacing it is very simple:
    */

    @Inject(
        method = "renderDirtBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At("RETURN"), require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderDirtBackground$mojang(GuiGraphics gui, CallbackInfo info) {
        LoadingBackgrounds.renderBackground((Screen) (Object) this, gui);
    }
    @Inject(
        method = "method_25434(Lnet/minecraft/class_332;)V",
        at = @At("RETURN"), require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderDirtBackground$intermediary(GuiGraphics gui, CallbackInfo info) {
        LoadingBackgrounds.renderBackground((Screen) (Object) this, gui);
    }
    @Inject(
        method = "m_280039_(Lnet/minecraft/src/C_279497_;)V",
        at = @At("RETURN"), require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderDirtBackground$srg(GuiGraphics gui, CallbackInfo info) {
        LoadingBackgrounds.renderBackground((Screen) (Object) this, gui);
    }

    /*
    From 1.20.5 to 1.21.8, as of writing, there is a fancy panoramic background.
    Unfortunately, background rendering is now splintered across a few different methods.
    Mojang, in their infinite wisdom, often calls them separately across the wide codebase.
    So we need to keep track of some state and cancel some of these method calls as appropriate. Weh!
    */

    /*
    My injections into renderBlurredBackground and renderMenuBackground need to know if
    LoadingBackgrounds.renderBackground was *just* called, and if it succeeded or not.
    There are many solutions, but I want something that is extremely portable.
    So, I simply check how long it's been since the last successful
    LoadingBackgrounds.renderBackground using System.nanoTime().
    Such an evil hack!
    */

    // The current millisecond when LoadingBackgrounds.renderBackground last succeeded
    private long loadingbackgrounds$renderedMillisecond = Long.MIN_VALUE;

    // Returns the current millisecond with high accuracy
    private long loadingbackgrounds$currentMillisecond() {
        // Use nanoTime instead of currentTimeMillis for better accuracy
        // It's possible for currentTimeMillis to be only accurate to a hundredth of a second
        return System.nanoTime() / 1000000L;
    }

    // Checks if LoadingBackgrounds.renderBackground succeeded less than ~2 milliseconds ago
    private boolean loadingbackgrounds$hasRendered() {
        long now = loadingbackgrounds$currentMillisecond();
        return loadingbackgrounds$renderedMillisecond == now
            || loadingbackgrounds$renderedMillisecond == now - 1;
    }

    /*
    Now, we inject into the renderPanorama method which may or may not be called.
    This method is stable from 1.20.5 to 1.21.8.
    */

    @Inject(
        method = "renderPanorama(Lnet/minecraft/client/gui/GuiGraphics;F)V",
        at = @At("RETURN"), require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderPanorama$mojang(GuiGraphics gui, float partialTick, CallbackInfo info) {
        if (LoadingBackgrounds.renderBackground((Screen) (Object) this, gui)) {
            loadingbackgrounds$renderedMillisecond = loadingbackgrounds$currentMillisecond();
        }
    }
    @Inject(
        method = "method_57728(Lnet/minecraft/class_332;F)V",
        at = @At("RETURN"), require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderPanorama$intermediary(GuiGraphics gui, float partialTick, CallbackInfo info) {
        if (LoadingBackgrounds.renderBackground((Screen) (Object) this, gui)) {
            loadingbackgrounds$renderedMillisecond = loadingbackgrounds$currentMillisecond();
        }
    }

    /*
    Then, if we rendered over the panorama, we need to cancel background blurring.
    We'll inject into the renderBlurredBackground method and cancel it in that case.
    This method is not stable:
        - From 1.20.5 to 1.21.1 it is `void renderBlurredBackground(float partialTick)`
        - From 1.21.2 to 1.21.5 it is `void renderBlurredBackground()`
        - From 1.21.6 to 1.21.8 it is `void renderBlurredBackground(GuiGraphics gui)`
    So, we need three injections.
    */

    @Inject(
        method = "renderBlurredBackground(F)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderBlurredBackground$mojang$0(float partialTick, CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }
    @Inject(
        method = "method_57734(F)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderBlurredBackground$intermediary$0(float partialTick, CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }

    @Inject(
        method = "renderBlurredBackground()V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderBlurredBackground$mojang$1(CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }
    @Inject(
        method = "method_57734()V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderBlurredBackground$intermediary$1(CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }

    @Inject(
        method = "renderBlurredBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderBlurredBackground$mojang$2(GuiGraphics gui, CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }
    @Inject(
        method = "method_57734(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderBlurredBackground$intermediary$2(GuiGraphics gui, CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }

    /*
    Finally, if we rendered over the panorama, we also need to cancel background "darkening".
    This is done by the renderMenuBackground method, so we'll cancel it in that case.
    This method is also stable from 1.20.5 to 1.21.8.
    */

    @Inject(
        method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderMenuBackground$mojang(GuiGraphics gui, CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }
    @Inject(
        method = "method_57735(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At("HEAD"), cancellable = true, require = 0, remap = false
    )
    private void loadingbackgrounds$inject$renderMenuBackground$intermediary(GuiGraphics gui, CallbackInfo info) {
        if (loadingbackgrounds$hasRendered()) info.cancel();
    }

}
