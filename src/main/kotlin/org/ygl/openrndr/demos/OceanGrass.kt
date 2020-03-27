package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.ColorMap
import org.ygl.openrndr.utils.vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


private const val BORDER = 40
private const val TOTAL_FRAMES = 360
private const val ROWS = 48
private const val COLS = 48
private const val MAX_OFFSET = 20.0
private const val SEED1 = 72346
private const val SEED2 = 12987


fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    val noise = FastNoise()
    val bgColor = ColorRGBa.fromHex(0x22153E)
    val colorMap = ColorMap(listOf(
            ColorRGBa.fromHex(0xBCE784),
            ColorRGBa.fromHex(0x5DD39E),
            ColorRGBa.fromHex(0x348AA7)
    ))

    class Leaf(
            val pos: Vector2,
            val color: ColorRGBa,
            val length: Double
    )

    val points = List(ROWS) { row ->
        val y = BORDER + row * ((Configuration.height - BORDER) / ROWS)
        List(COLS) { col ->
            val x = BORDER + col * ((Configuration.width - BORDER) / COLS)
            Leaf(
                    pos = vector2(
                            x + Random.nextDouble(-MAX_OFFSET/2, MAX_OFFSET/2),
                            y + Random.nextDouble(-MAX_OFFSET/2, MAX_OFFSET/2)
                    ),
                    color = colorMap[Random.nextDouble()],
                    length = Random.nextDouble(0.5, 1.5)
            )
        }
    }

    val params = @Description("params") object {
        @DoubleParameter("noise radius", 0.0, 200.0)
        var radius = 77.0

        @DoubleParameter("noise magnitude", 0.0, 400.0)
        var magnitude = 31.0

        @DoubleParameter("noise scale", 0.0, 5.0)
        var scale = 0.285
    }

    program {

        val composite = compose {
            draw {
                drawer.background(bgColor)
                //drawer.stroke = fgColor
                drawer.strokeWeight = 4.0
                drawer.lineCap = LineCap.ROUND
//                drawer.fill = fgColor

                val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (row in points) {
                    for (point in row) {
                        val (x, y) = point.pos

                        noise.seed = SEED1
                        val dx = noise.getSimplex(
                                x = params.scale * x,
                                y = params.scale * y,
                                z = params.radius * cos(2 * PI * timeProgress),
                                w = params.radius * sin(2 * PI * timeProgress)
                        )

                        noise.seed = SEED2
                        val dy = noise.getSimplex(
                                x = params.scale * x,
                                y = params.scale * y,
                                z = params.radius * cos(2 * PI * timeProgress),
                                w = params.radius * sin(2 * PI * timeProgress)
                        )

                        val x2 = x + params.magnitude * point.length * dx
                        val y2 = y + params.magnitude * point.length * dy

                        drawer.stroke = point.color
                        drawer.lineSegment(x, y, x2, y2)
                    }
                }

            }
            if (Configuration.recording) {
                post(FrameBlur())
            }
        }

        if (Configuration.recording) {
            extend(ScreenRecorder()) {
                outputFile = "video1/ocean-grass.mp4"
                frameRate = 60
                frameClock = true
            }
        }

         // extend(GUI()) {
         //     add(params)
         //     loadParameters(File("data/settings/ocean-grass-params.json"))
         // }

        extend {
            composite.draw(drawer)

            if (Configuration.recording) {
                if (frameCount >= TOTAL_FRAMES) {
                    application.exit()
                }
            }
        }
    }
}