package dev.foxgirl.mc.loadingbackgrounds

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import java.nio.file.Files

class Config {

    var secondsStay: Double = 5.0
    var secondsFade: Double = 0.5
    var brightness: Float = 1.0F
    var position: Position = Position.BOTTOM_RIGHT
    var shouldLoadResources: Boolean = true

    enum class Position {
        CENTER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    companion object {

        private val DEFAULT =
            """
            // Loading Backgrounds v2 configuration JSON file
            {
              // Amount of time that each background is displayed for
              "secondsStay": 5.0,
              // Amount of time it takes to fade between backgrounds
              "secondsFade": 0.5,
              // Background brightness, between 0.0 and 1.0
              "brightness": 1.0,
              // Level loading indicator position
              // One of "CENTER", "BOTTOM_LEFT", "BOTTOM_RIGHT", "TOP_LEFT", or "TOP_RIGHT"
              "position": "BOTTOM_RIGHT",
              // Should we try to forcefully load any resource packs that could contain background images?
              "shouldLoadResources": true
            }
            """.trimIndent()

        fun load(): Config {
            val path = LoadedMod.getInstance().getConfigPath().resolve("loadingbackgrounds.json")

            val gson = GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .setPrettyPrinting()
                .setLenient()
                .create()

            try {
                Files.newBufferedReader(path).use { reader -> return gson.fromJson(reader, Config::class.java) }
            } catch (cause: java.nio.file.NoSuchFileException) {
                logger.warn("Config file loadingbackgrounds.json not found, saving default config")
            } catch (cause: JsonParseException) {
                logger.warn("Config file loadingbackgroudns.json is invalid! ${cause.message}")
            } catch (cause: Exception) {
                logger.error("Exception while reading config file", cause)
            }

            try {
                Files.createDirectories(path.parent)
                Files.writeString(path, DEFAULT)
            } catch (cause: Exception) {
                logger.error("Exception while saving default config file", cause)
            }

            return gson.fromJson(DEFAULT, Config::class.java)
        }

    }

}
