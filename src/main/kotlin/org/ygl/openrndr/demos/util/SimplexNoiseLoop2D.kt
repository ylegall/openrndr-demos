package org.ygl.openrndr.demos.util

import org.openrndr.extra.noise.simplex
import org.openrndr.math.Polar
import org.ygl.openrndr.utils.NoiseLoop2D
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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