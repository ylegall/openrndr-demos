package org.ygl.openrndr.demos.util

import org.openrndr.extra.noise.simplex
import org.ygl.fastnoise.FastNoise
import org.ygl.fastnoise.NoiseType
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = 2 * Math.PI

fun simplexNoise2D(
        seed: Int,
        progress: Double,
        timeOffset: Double = 0.0,
        radius: Double = 1.0,
        xOffset: Double = 0.0,
        yOffset: Double = 0.0,
        noiseSpeed: Double = 1.0
): Double {
    val radians = TWO_PI * noiseSpeed * (progress - timeOffset)
    val x = xOffset + radius * cos(radians)
    val y = yOffset + radius * sin(radians)
    return simplex(seed, x, y)
}

class FastSimplexNoise4D(seed: Int = 1337) {
    private val fastNoise = FastNoise(seed)

    init {
        fastNoise.noiseType = NoiseType.SIMPLEX
    }

    fun simplexNoise2D(
            progress: Double,
            timeOffset: Double = 0.0,
            radius: Double = 1.0,
            xOffset: Double = 0.0,
            yOffset: Double = 0.0
    ): Double {
        val radians = TWO_PI * (progress - timeOffset)
        val x = xOffset + radius * cos(radians)
        val y = yOffset + radius * sin(radians)
        return fastNoise.getSimplex(x.toFloat(), y.toFloat()).toDouble()
    }

    fun simplexNoise4D(
            progress: Double,
            timeOffset: Double = 0.0,
            radius: Double = 1.0,
            x: Double = 0.0,
            y: Double = 0.0,
            zOffset: Double = 0.0,
            wOffset: Double = 0.0
    ): Double {
        val radians = TWO_PI * (progress - timeOffset)
        val z = zOffset + radius * cos(radians)
        val w = wOffset + radius * sin(radians)
        return fastNoise.getSimplex(x, y, z, w)
    }
}

