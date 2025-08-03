package dev.foxgirl.mc.loadingbackgrounds

import com.google.common.collect.Iterators
import com.mojang.blaze3d.systems.RenderSystem
import dev.foxgirl.mc.loadingbackgrounds.impl.PNG
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.*
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.repository.PackRepository
import net.minecraft.server.packs.resources.ReloadableResourceManager
import net.minecraft.util.Unit
import org.apache.logging.log4j.Logger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

const val MOD_ID = "loadingbackgrounds"

val logger: Logger get() = LoadingBackgrounds.LOGGER

val config: Config by lazy { Config.load() }

val minecraft: Minecraft get() = Minecraft.getInstance()

val resourcePacks: PackRepository
    get() = minecraft.resourcePackRepository
val resourceManager: ReloadableResourceManager
    get() = minecraft.resourceManager as ReloadableResourceManager

val textureManager: TextureManager
    get() = minecraft.textureManager

val backgroundExecutor: ExecutorService
    get() = Util.backgroundExecutor()
val ioExecutor: ExecutorService
    get() = Util.ioPool()

val gameExecutor: Executor
    get() = minecraft

val loadingMessageTranslationKeys = setOf(
    "menu.generatingLevel",
    "menu.generatingTerrain",
    "menu.loadingForcedChunks",
    "menu.loadingLevel",
    "menu.preparingSpawn",
    "menu.savingChunks",
    "menu.savingLevel",
    "menu.working",
    "multiplayer.downloadingStats",
    "multiplayer.downloadingTerrain",
    "selectWorld.data_read",
    "selectWorld.loading_list",
    "selectWorld.resource_load",
    "resourcepack.downloading",
    "resourcepack.progress",
    "download.pack.title",
)

fun isLoadingMessage(component: Component?): Boolean {
    if (component == null) return false
    val contents = component.contents
    return (contents is TranslatableContents && contents.key in loadingMessageTranslationKeys)
        || (component.siblings.any(::isLoadingMessage))
}

fun isLoadingScreen(screen: Screen): Boolean {
    if (screen is ProgressScreen) return true
    if (screen is ConnectScreen) return true
    if (screen is LevelLoadingScreen) return true
    if (screen is ReceivingLevelScreen) return true
    return isLoadingMessage(screen.title)
}

val resourcePackRegex = Regex("load(ing)?[\\W_]{0,3}(background|bg|image|img|pic)", RegexOption.IGNORE_CASE)

var reloadResourcePacksHash = 0

fun reloadResourcePacks(): Boolean {
    val newHash = resourcePacks.availablePacks.map { it.id }.hashCode()
    val oldHash = reloadResourcePacksHash
    if (oldHash == newHash) return false
    reloadResourcePacksHash = newHash

    var added = false

    for (pack in resourcePacks.availablePacks) {
        if (
            resourcePackRegex.containsMatchIn(pack.id) ||
            resourcePackRegex.containsMatchIn(pack.title.string)
        ) {
            added = resourcePacks.addPack(pack.id) || added
        }
    }

    if (added) {
        resourceManager.createReload(
            backgroundExecutor,
            gameExecutor,
            CompletableFuture.completedFuture(Unit.INSTANCE),
            resourcePacks.openAllSelected(),
        )
        return true
    } else {
        return false
    }
}

fun selectPNGLocations(): List<ResourceLocation> {
    val resources = resourceManager.listResources("textures/gui/backgrounds") { it.path.endsWith(".png") }
    val locations = resources.keys.toList()
    return locations
}

fun selectPNGLocationsOrReload(): List<ResourceLocation> {
    var locations = selectPNGLocations()
    if (locations.isEmpty() && config.shouldLoadResources) {
        if (reloadResourcePacks()) {
            locations = selectPNGLocations()
        }
    }
    return locations
}

fun selectPNGs(): List<PNG> {
    val locations = selectPNGLocationsOrReload()
    if (locations.isNotEmpty()) {
        return PNG.get(locations).get(5, TimeUnit.SECONDS)
    } else {
        return emptyList()
    }
}

fun renderPNG(screen: Screen, gui: GuiGraphics, png: PNG, brightness: Float, opacity: Float) {
    var offsetX = 0.0F
    var offsetY = 0.0F

    // Calculate scale factors
    var scaleX: Float = screen.width.toFloat() / png.width.toFloat()
    var scaleY: Float = screen.height.toFloat() / png.height.toFloat()

    // Check if the texture aspect ratio matches the screen aspect ratio
    if (scaleX < scaleY) {
        // The texture is wider than the screen, so we need to adjust the scale and offset
        scaleX = scaleY
        offsetX = 0.0F - ((screen.width.toFloat() - (png.width.toFloat() * scaleX)) * 0.5F)
    } else {
        // The texture is taller than the screen or has the same aspect ratio, so we adjust the scale and offset accordingly
        scaleY = scaleX
        offsetY = 0.0F - ((screen.height.toFloat() - (png.height.toFloat() * scaleY)) * 0.5F)
    }

    RenderSystem.defaultBlendFunc()
    RenderSystem.enableBlend()

    val shader = RenderSystem.getShader()
    val shaderColor = RenderSystem.getShaderColor()
    val shaderTexture = RenderSystem.getShaderTexture(0)

    RenderSystem.setShaderColor(brightness, brightness, brightness, opacity)

    gui.blit(
        png.location,
        0, 0, // 0,
        offsetX, offsetY,
        screen.width, screen.height,
        (png.width.toFloat() * scaleX).toInt(),
        (png.height.toFloat() * scaleY).toInt(),
    )

    RenderSystem.setShader { shader }
    RenderSystem.setShaderColor(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3])
    RenderSystem.setShaderTexture(0, shaderTexture)

    RenderSystem.disableBlend()
}

val seconds: () -> Double = run {
    val start = System.nanoTime()
    return@run { (System.nanoTime() - start).toDouble() * 1e-9 }
}

var pngIterator: Iterator<PNG>? = null

var pngPrevious: PNG? = null
var pngCurrent: PNG? = null

var secondsAtChange = 0.0

var isFading = false

fun renderBackground(screen: Screen, gui: GuiGraphics): Boolean {

    if (!isLoadingScreen(screen)) {
        return false
    }

    val secondsNow = seconds()
    var secondsSinceChange = secondsNow - secondsAtChange

    if (
        pngIterator == null ||
        secondsSinceChange > Math.max(config.secondsFade, config.secondsStay) + 5.0
    ) {
        secondsSinceChange = 0.0

        secondsAtChange = secondsNow
        isFading = false

        val pngs = selectPNGs()
        if (pngs.isEmpty()) {
            return false
        }

        pngIterator = Iterators.cycle(pngs)

        pngPrevious = pngIterator!!.next()
        pngCurrent = pngIterator!!.next()
    }

    if (isFading) {
        renderPNG(screen, gui, pngPrevious!!, config.brightness, 1.0F)
        renderPNG(screen, gui, pngCurrent!!, config.brightness, (secondsSinceChange / config.secondsFade).coerceIn(0.0, 1.0).toFloat())
        if (secondsSinceChange > config.secondsFade) {
            secondsAtChange = secondsNow
            isFading = false
        }
    } else {
        renderPNG(screen, gui, pngCurrent!!, config.brightness, 1.0F)
        if (secondsSinceChange > config.secondsStay) {
            secondsAtChange = secondsNow
            isFading = true
            pngPrevious = pngCurrent
            pngCurrent = pngIterator!!.next()
        }
    }

    return true

}

fun init() {
    logger.info("Hello from Loading Backgrounds :3c")
}
