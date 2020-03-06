package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Darken
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.noise.uniformRing
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.shape.compound
import org.openrndr.shape.shape
import org.ygl.openrndr.demos.util.FastSimplexNoise4D
import org.ygl.openrndr.demos.util.simplexNoise2D
import org.ygl.openrndr.utils.distanceFrom
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.randomPoint
import org.ygl.openrndr.utils.rect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val WIDTH = 800
private const val HEIGHT = 800
private const val TOTAL_FRAMES = 480
private const val TOTAL_POINTS = 30000
private const val OFFSET_MAG = 160
private const val NOISE_RADIUS = 44.3

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val videoTarget = renderTarget(WIDTH, HEIGHT) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(WIDTH, HEIGHT)
                .frameRate(60)
                //.output("video/wobbly-surface.p4")
                .start()

        val fastNoise = FastSimplexNoise4D(451)
        val fgColor = ColorRGBa.fromHex(0xFCC8B2).opacify(0.9)
        val screen = rect(0, 0, WIDTH, HEIGHT).let { it.moved(-it.center) }
        val bounds = rect(0, 0, WIDTH - 235, HEIGHT - 235).let { it.moved(-it.center) }

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
                    val dx = intensity * OFFSET_MAG * fastNoise.simplexNoise4D(
                            progress,
                            radius = NOISE_RADIUS,
                            x = 10 + point.x,
                            y = point.y
                    )
                    val dy = intensity * OFFSET_MAG * fastNoise.simplexNoise4D(
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
            post(FrameBlur())
        }

        extend {
            drawer.isolatedWithTarget(videoTarget) {
                translate(width/2.0, height/2.0)
                rotate(45.0)
                composite.draw(this)
            }

            if (frameCount >= TOTAL_FRAMES + 60) {
//                println("loop")
                videoWriter.stop()
                application.exit()
            }
            if (frameCount >= 60) {
                videoWriter.frame(videoTarget.colorBuffer(0))
            }
        }
    }
}