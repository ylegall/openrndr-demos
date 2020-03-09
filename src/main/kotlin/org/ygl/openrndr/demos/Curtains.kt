package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.rangeMap
import org.ygl.openrndr.utils.vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val WIDTH = 800
private const val HEIGHT = 800
private const val BORDER = 100
private const val TOTAL_FRAMES = 360

private const val PATHS = 128
private const val PATH_POINTS = 512

private const val NOISE_SCALE = 0.37
private const val NOISE_RADIUS = 150.0

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val noise = FastNoise()
        val fgColor = ColorRGBa.fromHex(0xFE5F55).opacify(0.7)
        val bgColor = ColorRGBa.fromHex(0x1B202A)

        val points = MutableList(PATH_POINTS) { Vector2.ZERO }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.stroke = null
                drawer.fill = fgColor

                val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (path in 0 until PATHS) {
                    val x = path.rangeMap(0, PATHS, -WIDTH/2 + BORDER, WIDTH/2 - BORDER)

                    for (i in 0 until PATH_POINTS) {
                        val pathProgress = (i / PATH_POINTS.toDouble())
                        val y = (pathProgress).pow(2).rangeMap(0, 1, BORDER, HEIGHT - BORDER)

                        val noiseAngle = (2 * PI * (pathProgress - timeProgress))

                        val noiseX = noise.getSimplex(
                                x = x * NOISE_SCALE * 2 * pathProgress,
                                y = y * NOISE_SCALE,
                                z = NOISE_RADIUS * cos(noiseAngle).toFloat(),
                                w = NOISE_RADIUS * sin(noiseAngle).toFloat()
                        )
                        val noiseY = noise.getSimplex(
                                x = x * NOISE_SCALE * 7 * pathProgress,
                                y = y * NOISE_SCALE,
                                z = NOISE_RADIUS * cos(noiseAngle).toFloat(),
                                w = NOISE_RADIUS * sin(noiseAngle).toFloat()
                        )

                        val dx = 67 * noiseX * pathProgress
                        val dy = 33 * noiseY * (1 - pathProgress) * pathProgress

                        points[i] = vector2(x + dx, y + dy)
                    }
                    drawer.circles(points, 3.0)
                }
            }
        }

        extend(ScreenRecorder()) {
            frameRate = 60
            frameClock = true
        }

        extend {
            drawer.translate(width/2.0, 0.0)
            composite.draw(drawer)

            if (frameCount >= TOTAL_FRAMES) {
                application.exit()
            }
        }
    }
}