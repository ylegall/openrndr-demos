package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.noise.simplex
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.openrndr.demos.util.simplexNoise2D
import org.ygl.openrndr.utils.isolated
import org.ygl.openrndr.utils.rangeMap
import org.ygl.openrndr.utils.vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val WIDTH = 800
private const val HEIGHT = 800
private const val TOTAL_FRAMES = 360
private const val FRAME_PERIODS = 1.0
private const val CURVE_POINTS = 2048


fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    val bgColor = ColorRGBa(0.0, 0.0, 0.0)
    val particleColor = ColorRGBa.WHITE

    fun getCurvePosition(progress: Double, timeOffset: Double = 0.0): Vector2 {
        val rads = PI * (progress - timeOffset)
        val mag = sin(rads * 3.0)
        val x = WIDTH/2.0 * cos(rads) * mag
        val y = HEIGHT/2.0 * sin(rads) * mag
        return vector2(x, y)
    }

    class SmokeSegment(
            private val start: Double = 0.0,
            private val length: Double = 1.0,
            private val points: Int = (length * CURVE_POINTS * 2).toInt()
    ) {
        private val seedX = 451
        private val seedY = 672

        fun draw(drawer: Drawer, frameCount: Int) = drawer.isolated {
            val timeOffset = (frameCount * FRAME_PERIODS) / TOTAL_FRAMES.toDouble()
            for (i in 0 until points) {
                val progress = (i / points.toDouble())
                val curvePosition = start + length * progress
//                val position = getCurvePosition(curveProgress, timeOffset)
                val position = getCurvePosition(curvePosition, 0.0)
                val distortion = 10.0 * progress
                val dx = 16 * simplexNoise2D(seedX, progress, timeOffset, distortion)
                val dy = 16 * simplexNoise2D(seedY, progress, timeOffset, distortion)

                val opacity = abs(0.4 - progress).rangeMap(0.0, 0.6, 1.0, 0.0)

                stroke = null
                fill = particleColor.opacify(opacity)
                drawer.circle(position.x + dx, position.y + dy, 3.0)
            }
        }
    }

    program {

//        val segments = List(1) {
//            SmokeSegment( 0.0, 1.0, CURVE_POINTS)
//        }

        val segments = listOf(
                SmokeSegment(0.00, 0.3),
                SmokeSegment(0.15, 0.3),
                SmokeSegment(0.30, 0.3),
                SmokeSegment(0.45, 0.3),
                SmokeSegment(0.60, 0.3),
                SmokeSegment(0.75, 0.3),
                SmokeSegment(0.90, 0.3)
        )

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.translate(width/2.0, height/2.0 + 20.0)
                segments.forEach { segment ->
                    segment.draw(drawer, frameCount)
                }
            }
            post(FrameBlur())
        }

//        extend(ScreenRecorder()) {
//            frameRate = 60
//            frameClock = true
//        }
        extend {
            composite.draw(drawer)
            if (frameCount >= TOTAL_FRAMES) {
                application.exit()
            }
        }
    }
}
