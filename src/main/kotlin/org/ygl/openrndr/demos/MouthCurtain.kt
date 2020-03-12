package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.loadImage
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.rangeMap
import org.ygl.openrndr.utils.vector2
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val WIDTH = 640
private const val HEIGHT = 640
private const val BOTTOM_BORDER = 0
private const val TOTAL_FRAMES = 360

private const val PATH_POINTS = 512


fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        fun findStartPoints(image: ColorBuffer): List<Vector2> {
            val points = mutableListOf<Vector2>()
            val shadow = image.shadow.also { it.download() }
            for (col in 0 until image.width step 3) {
                for (row in 0 until image.height) {
                    val color = shadow[col, row]
                    if (color.r > 0.5 && color.b < 0.3) {
                        points.add(vector2(col, row))
                        break
                    }
                }
            }
            return points.also { println(it.size) }
        }

        val background = loadImage("data/mouth2.png")
        val startPoints = findStartPoints(background).sortedBy { it.x }

        val noise = FastNoise()
        val fgColor = ColorRGBa.fromHex(0xFE5F55).opacify(0.7)
        //val bgColor = ColorRGBa.fromHex(0x1B202A)
        val points = MutableList(PATH_POINTS) { Vector2.ZERO }

        val parameters = @Description("settings") object {
            @DoubleParameter("noise scale x", 0.0, 4.0)
            var scaleX = 0.646

            @DoubleParameter("noise scale y", 0.0, 4.0)
            var scaleY = 0.571

            @DoubleParameter("noise radius x", 0.0, 600.0)
            var radiusX = 67.081

            @DoubleParameter("noise radius y", 0.0, 600.0)
            var radiusY = 95.031

            @DoubleParameter("noise magnitude x", 0.0, 400.0)
            var magX = 57.143

            @DoubleParameter("noise magnitude y", 0.0, 400.0)
            var magY = 50.932
        }

        val composite = compose {
            draw {
                drawer.stroke = null
                drawer.fill = fgColor

                val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (point in startPoints.indices) {
                    val xProgress = point / startPoints.size.toDouble()
                    val (startX, startY) = startPoints[point]
                    val adjustedX = startX - width / 2

                    for (i in 0 until PATH_POINTS) {
                        val pathProgress = (i / PATH_POINTS.toDouble())
                        val y = pathProgress.rangeMap(0, 1, startY, HEIGHT - BOTTOM_BORDER)
                        val x = adjustedX + pathProgress * xProgress.rangeMap(0, 1, -60, 60)

                        val noiseAngle = (2 * PI * (pathProgress - timeProgress))

                        val noiseX = noise.getSimplex(
                                x = x * parameters.scaleX,
                                y = y * parameters.scaleY,
                                z = parameters.radiusX * cos(noiseAngle).toFloat(),
                                w = parameters.radiusX * sin(noiseAngle).toFloat()
                        )
                        val noiseY = noise.getSimplex(
                                x = x * parameters.scaleX,
                                y = y * parameters.scaleY,
                                z = parameters.radiusY * cos(noiseAngle).toFloat(),
                                w = parameters.radiusY * sin(noiseAngle).toFloat()
                        )

                        val dx = parameters.magX * noiseX * pathProgress
                        val dy = parameters.magY * noiseY * pathProgress

                        points[i] = vector2(x + dx, y + dy)
                    }
                    drawer.circles(points, 3.0)
                }
            }
        }

//        extend(ScreenRecorder()) {
//            frameRate = 60
//            frameClock = true
//        }

        extend(GUI()) {
            add(parameters)
            loadParameters(File("data/curtain-mouth-noise-params.json"))
        }

        extend {
            drawer.image(background)
            drawer.translate(width/2.0, 0.0)
            composite.draw(drawer)

//            if (frameCount >= TOTAL_FRAMES) {
//                application.exit()
//            }
        }
    }
}