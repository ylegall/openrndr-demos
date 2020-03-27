package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.math.Vector2
import org.ygl.openrndr.demos.util.FastSimplexNoise4D
import org.ygl.openrndr.utils.ColorMap
import kotlin.math.round

private const val GRID_SIZE = 300
private const val TOTAL_FRAMES = 300

private const val PATH_POINTS = 200
private const val NOISE_RADIUS = 15.45
private const val MAX_MAGNITUDE = 400
private const val NOISE_SCALE = 1.0

private val noise = FastSimplexNoise4D()

private fun getFieldVector(
        progress: Double,
        x: Double,
        y: Double
): Vector2 {
    val noiseX = MAX_MAGNITUDE * noise.simplexNoise4D(
            progress,
            radius = NOISE_RADIUS,
            x = x * NOISE_SCALE,
            y = y * NOISE_SCALE
    )
    val noiseY = MAX_MAGNITUDE * noise.simplexNoise4D(
            progress,
            radius = NOISE_RADIUS,
            x = 1000 + x * NOISE_SCALE,
            y = y * NOISE_SCALE
    )
    val dstX = 2.5 * 0.15 * round(noiseX) / 100.0
    val dstY = 2.5 * 0.15 * round(noiseY) / 100.0
    return Vector2(dstX, dstY)
}

fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    program {

        val colorMap = ColorMap(listOf(
                ColorRGBa.fromHex(0x400566),
                ColorRGBa.fromHex(0xD62828),
                ColorRGBa.fromHex(0xF77F00),
                ColorRGBa.fromHex(0xFCC24E),
                ColorRGBa.fromHex(0xFCF1B0)
        ))

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
                drawer.stroke = null

                val progress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (y in -GRID_SIZE/2 until GRID_SIZE/2 step 5) {
                    for (x in -GRID_SIZE/2 until GRID_SIZE/2 step 5) {
                        var pathX = x.toDouble()
                        var pathY = y.toDouble()

                        drawer.fill = colorMap[0.0]
                        drawer.circle(pathX, pathY, 1.8)

                        for (i in 0 until PATH_POINTS) {
                            val fieldVector = getFieldVector(progress, pathX, pathY)
                            pathX += fieldVector.x
                            pathY += fieldVector.y
                            drawer.fill = colorMap[i / PATH_POINTS.toDouble()]
                            drawer.circle(pathX, pathY, 1.8)
                        }

                        drawer.fill = colorMap[1.0]
                        drawer.circle(pathX, pathY, 1.8)
                    }
                }
            }
//            post(FrameBlur())
        }

//        extend(ScreenRecorder()) {
//            frameRate = 60
//            frameClock = true
//        }

        extend {
            drawer.translate(width/2.0, height/2.0)
            drawer.scale(1.3)
            composite.draw(drawer)

            if (frameCount >= TOTAL_FRAMES) {
                application.exit()
            }
        }
    }
}