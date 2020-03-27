package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.math.smoothstep
import org.ygl.openrndr.demos.util.simplexNoise2D
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.vector2
import kotlin.random.Random

private const val PAUSE_FRAMES = 180
private const val TOTAL_FRAMES = 480
private const val SEED = 415

fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    program {

        class Particle(
                val pos: Vector2
        ) {
            var p1 = pos
            val xScale = Random.nextDouble(0.5, 1.5)
            val yScale = Random.nextDouble(0.5, 1.5)

            fun update(progress: Double) {
                val radius = 0.2 * if (progress < 0.5) {
                    smoothstep(0.0, 0.5, progress)
                } else {
                    smoothstep(0.0, 0.5, 1.0 - progress)
                }

                val dx = xScale * 150 * simplexNoise2D(SEED, progress, radius = radius, xOffset = pos.x, yOffset = pos.y)
                val dy = yScale * 300 * simplexNoise2D(3 * SEED, progress, radius = radius, xOffset = pos.x, yOffset = pos.y)
                p1 = pos + vector2(dx, dy)
            }

        }

        var timeProgress: Double
        var loops = 0
        val fgColor = ColorRGBa.fromHex(0x0ED1A0)
        val bgColor = ColorRGBa.fromHex(0x1C0F1B)
        val particles = mutableListOf<Particle>()

        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .output("poop.mp4")
                .frameRate(60)
                .start()

        val textImage = loadImage("data/text.png")
        val shadow = textImage.shadow
        shadow.download()
        for (i in 0 until width step 2) {
            for (j in 0 until height step 2) {
                val color = shadow[i, j]
                val totalValue = (color.r + color.g + color.b)
                if (totalValue >= 2.0) {
                    particles.add(Particle(vector2(i, j)))
                }
            }
        }
        println(particles.size)

        fun getTimeProgress(): Double {
            val frameOffset = frameCount % TOTAL_FRAMES
            return if (frameOffset < PAUSE_FRAMES) {
                0.0
            } else {
                (frameOffset - PAUSE_FRAMES).toDouble() / (TOTAL_FRAMES - PAUSE_FRAMES)
            }
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.stroke = null
                drawer.fill = fgColor

                timeProgress = getTimeProgress()
                particles.forEach {
                    it.update(timeProgress)
                }
                drawer.circles(particles.map { it.p1 }, 3.0)
            }
            post(GaussianBloom())
            post(FrameBlur())
        }


        extend {
            drawer.isolatedWithTarget(videoTarget) {
                composite.draw(drawer)
            }

            if (frameCount % TOTAL_FRAMES == 0) {
                loops++
//                println("loop")
                if (loops > 1) {
                    application.exit()
                }
            }

            if (loops > 0) {
                videoWriter.frame(videoTarget.colorBuffer(0))
            }
        }
    }
}