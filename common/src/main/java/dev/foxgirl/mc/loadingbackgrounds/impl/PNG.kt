package dev.foxgirl.mc.loadingbackgrounds.impl

import dev.foxgirl.mc.loadingbackgrounds.impl.async.Async
import dev.foxgirl.mc.loadingbackgrounds.impl.async.Promise
import dev.foxgirl.mc.loadingbackgrounds.ioExecutor
import dev.foxgirl.mc.loadingbackgrounds.resourceManager
import net.minecraft.resources.ResourceLocation
import java.io.BufferedInputStream
import java.io.DataInputStream

/**
 * Represents a PNG to be used as a Minecraft texture,
 * with copies of the image's width and height,
 * stored at the [ResourceLocation].
 *
 * Minecraft doesn't provide an easy way to get the resolution/dimensions of a
 * texture. Previously, I used some mixin hacks to access this information, but
 * that isn't portable across versions. However, reading the PNG's IHDR right
 * off of the disk and stuffing that in a struct is completely portable!
 *
 * This class uses my [Promise] and [Async] libraries to get the job done efficiently.
 *
 * @author Lua MacDougall <lua@foxgirl.dev>
 */
data class PNG(
    /** Resource location of the PNG. */
    val location: ResourceLocation,
    /** Width of the PNG in pixels. */
    val width: Int,
    /** Height of the PNG in pixels. */
    val height: Int,
) {

    companion object {

        // I assume that any good PNG should have dimensions in this range.
        // This class will ignore invalid, zero sized, too small, and too big PNGs.
        private val DIMENSION_LIMITS = 4..32767

        /**
         * Attempts to read the [PNG] at the given location.
         */
        private fun read(location: ResourceLocation): PNG? {
            DataInputStream(BufferedInputStream(resourceManager.open(location), 24))
                .use { stream ->
                    stream.skipNBytes(16)
                    val width = stream.readInt()
                    val height = stream.readInt()
                    if (width !in DIMENSION_LIMITS) return null
                    if (height !in DIMENSION_LIMITS) return null
                    return PNG(location, width, height)
                }
        }

        // To save us from fetching information about the same PNG twice - an expensive task - we use a cache.
        // The cache contains Promises which can be either completed or still pending.
        // The cache is always checked when get is called:
        //   If there is no entry, or if something went wrong (null result or exception), we call read on an IO thread.
        //   Otherwise, we have a valid pending or completed Promise, so we just return that.
        private val cache = HashMap<ResourceLocation, Promise<PNG?>>()

        /**
         * Attempts to read the [PNG] at the given location.
         * This task will be performed on an IO thread, and will use a cache.
         */
        fun get(location: ResourceLocation): Promise<PNG?> {
            synchronized(cache) {
                return cache.compute(location) { _, promise ->
                    if (promise == null || promise.result?.getOrNull() == null) {
                        // No cache entry, or existing entry found nothing / crashed
                        Promise(ioExecutor) { read(location) }
                    } else {
                        // Existing cache entry, either completed or still pending
                        promise
                    }
                }!!
            }
        }

        /**
         * Attempts to get as many [PNG]s as possible from a list of locations.
         * This is done by calling the singlet [get] efficiently with a throttle.
         * The returned list is shuffled randomly.
         */
        fun get(locations: Iterable<ResourceLocation>): Promise<List<PNG>> {
            val lock = Any()
            val iterator = locations.iterator()

            // Start four coroutines that will concurrently process tasks from the iterator
            val promises = (0 until 4).map {
                // Each coroutine will:
                Async.go {
                    // Create a list of resulting PNGs
                    val pngs = mutableListOf<PNG>()
                    // Loop until we run out of locations
                    while (true) {
                        val location = synchronized(lock) {
                            if (iterator.hasNext()) iterator.next() else break
                        }
                        // Attempt to get the PNG at the location
                        val png = Async.await(get(location))
                        if (png != null) pngs.add(png)
                    }
                    // Shuffle and return the resulting PNGs
                    pngs.shuffle()
                    pngs
                }
            }

            // Finally, collect all the results into a single list
            return Promise.all(promises).then { it.flatten() }
        }

    }

}
