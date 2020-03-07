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
import org.openrndr.shape.contour
import org.ygl.kxa.ease.Ease
import org.ygl.openrndr.utils.isolatedWithTarget
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val WIDTH = 640
private const val HEIGHT = 640
private const val TWO_PI = 2 * PI

private const val TOTAL_FRAMES = 720
private const val FRAME_DELAY = TOTAL_FRAMES
private const val FRAME_SPEED = 1
private const val RECORDING = false

private const val CURVE_POINTS = 2000
private const val MAX_CURVATURE = 16

private fun getSpiralPoint(
        progress: Double,
        curvature: Double,
        flip: Boolean
): Vector2 {
    val angle = PI/2.0 * (1 - progress) * curvature
    val radius = WIDTH / 2.0 * sqrt(progress)
    return if (flip) {
        Vector2(radius * cos(angle), -radius * sin(angle))
    } else {
        Vector2(-radius * cos(angle), radius * sin(angle))
    }
}


fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val bgColor = ColorRGBa.fromHex(0x00263A)
        val fgColor = ColorRGBa.fromHex(0xF27654)

        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(120)
                .start()

        fun getFrameProgress() = ((frameCount * FRAME_SPEED) % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

        fun getTimeProgress(): Double {
            val frameProgress = getFrameProgress()
            return if (frameProgress < 0.5) {
                Ease.CUBE_INOUT(frameProgress * 2) / 2
            } else {
                0.5
            }
        }

        fun getZoom(): Double {
            val frameProgress = getFrameProgress()
            return if (frameProgress < 0.5) {
                1.0
            } else {
                val eased = Ease.EXP_INOUT((frameProgress - 0.5) * 2)
                WIDTH * Ease.CUBE_INOUT(eased) + 1
            }
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.stroke = null
                drawer.fill = fgColor

                val timeProgress = getTimeProgress()
                val curvaturePercent = (1 + sin(TWO_PI * (timeProgress - 0.25))) / 2.0
                val zoom = getZoom()

                val shapeContour = contour {
                    moveTo(0.0, 0.0)
                    lineTo(-0.5 * zoom, 0.0)
                    for (i in 1 until CURVE_POINTS) {
                        val curveProgress = (i / CURVE_POINTS.toDouble())
                        val point = getSpiralPoint(curveProgress, MAX_CURVATURE * curvaturePercent, false)
                        lineTo(point * zoom)
                    }
                    lineTo(-width/2.0 - 10, 0.0)
                    lineTo(-width/2.0 - 10, height/2.0 + 10)
                    lineTo(width/2.0 + 10, height/2.0 + 10)
                    lineTo(width/2.0 + 10, 0.0)
                    for (i in CURVE_POINTS - 1 downTo 1) {
                        val curveProgress = (i / CURVE_POINTS.toDouble())
                        val point = getSpiralPoint(curveProgress, MAX_CURVATURE * curvaturePercent, true)
                        lineTo(point * zoom)
                    }
                    lineTo(0.5 * zoom, 0.0)
                    close()
                }
                drawer.contour(shapeContour)
            }
            if (RECORDING) { post(FrameBlur()) }
        }

        extend {
            if (RECORDING) {
                drawer.isolatedWithTarget(videoTarget) {
                    drawer.translate(width / 2.0, height / 2.0)
                    composite.draw(drawer)
                }
                if (frameCount >= FRAME_DELAY + TOTAL_FRAMES) {
                    videoWriter.stop()
                    application.exit()
                } else if (frameCount >= FRAME_DELAY) {
                    videoWriter.frame(videoTarget.colorBuffer(0))
                }
            } else {
                drawer.translate(width / 2.0, height / 2.0)
                composite.draw(drawer)
            }
        }
    }
}