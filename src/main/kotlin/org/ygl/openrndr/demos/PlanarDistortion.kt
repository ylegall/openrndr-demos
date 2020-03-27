package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.shape.compound
import org.ygl.openrndr.demos.util.FastSimplexNoise4D
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.randomPoint
import org.ygl.openrndr.utils.rect
import kotlin.math.pow


private const val TOTAL_FRAMES = 480
private const val TOTAL_POINTS = 30000
private const val OFFSET_MAG = 160
private const val NOISE_RADIUS = 44.3


fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    program {

        val videoTarget = renderTarget(Configuration.Width, Configuration.Height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(Configuration.Width, Configuration.Height)
                .frameRate(60)
                .start()

        val noise = FastSimplexNoise4D(451)
//        val noise = FastNoise()
        val fgColor = ColorRGBa.fromHex(0xFCC8B2).opacify(0.9)
        val screen = rect(0, 0, Configuration.Width, Configuration.Height).let { it.moved(-it.center) }
        val bounds = rect(0, 0, Configuration.Width - 235, Configuration.Height - 235).let { it.moved(-it.center) }

        val frame = compound {
            difference {
                shape(screen.shape)
                shape(bounds.shape)
            }
        }

        val points = List(TOTAL_POINTS) { bounds.randomPoint() }

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)

                val progress = frameCount / TOTAL_FRAMES.toDouble()
                val newPoints = points.map { point ->
                    val intensity = (1 - point.length/bounds.width).pow(3.75)
                    val dx = intensity * OFFSET_MAG * noise.simplexNoise4D(
                            progress,
                            radius = NOISE_RADIUS,
                            x = 10 + point.x,
                            y = point.y
                    )
                    val dy = intensity * OFFSET_MAG * noise.simplexNoise4D(
                            progress,
                            radius = NOISE_RADIUS,
                            x = point.x,
                            y = 0.9 * point.y
                    )
                    Vector2(point.x + dx, point.y + dy)
                }

                drawer.stroke = null
                drawer.fill = fgColor
                drawer.circles(newPoints, 3.0)

                drawer.fill = null
                drawer.strokeWeight = 1.5
                drawer.stroke = fgColor
                drawer.rectangle(bounds)

                drawer.stroke = null
                drawer.fill = ColorRGBa.BLACK
                drawer.shapes(frame)
            }
            if (Configuration.Recording) { post(FrameBlur()) }
        }

        extend {
            if (Configuration.Recording) {
                drawer.isolatedWithTarget(videoTarget) {
                    translate(width / 2.0, height / 2.0)
                    rotate(45.0)
                    composite.draw(this)
                }

                if (frameCount >= TOTAL_FRAMES + 60) {
                    videoWriter.stop()
                    application.exit()
                }
                if (frameCount >= 60) {
                    videoWriter.frame(videoTarget.colorBuffer(0))
                }
            } else {
                drawer.translate(width / 2.0, height / 2.0)
                drawer.rotate(45.0)
                composite.draw(drawer)
            }
        }
    }
}