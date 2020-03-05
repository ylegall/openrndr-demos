package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.kxa.ease.Ease
import org.ygl.kxa.ease.EaseFunctions
import org.ygl.openrndr.demos.util.simplexNoise2D
import org.ygl.openrndr.utils.distanceFrom
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private const val WIDTH = 800
private const val HEIGHT = 800
private const val PI_2 = PI / 2
private const val TOTAL_FRAMES = 360
private const val FRAME_SPEED = 0.5
private const val POINTS = 2048
private const val MAX_CURVATURE = 12

private fun getSpiralPoint(
        progress: Double,
        curvature: Double
): Vector2 {
    val angle = PI_2 * progress.pow(0.7) * curvature
    val radius = WIDTH/2.0 * progress * sqrt(progress)
    return Vector2(radius * cos(angle), radius * sin(angle))
}


fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val bgColor = ColorRGBa.BLACK
        val fgColor = ColorRGBa.WHITE
        var timeProgress = 0.0

        fun drawSpiralArms(
                seed: Int,
                timeProgress: Double
        ) {
            //val curvature = MAX_CURVATURE * (1 + sin(TWO_PI * timeProgress)) / 2
            val curvature = MAX_CURVATURE.toDouble()

            for (i in 0 until POINTS) {
                val curveProgress = (i / POINTS.toDouble())
                val p1 = getSpiralPoint(curveProgress, curvature)
                val p2 = p1 * -1.0
                val dist = p1.length / (WIDTH / 1.5)
                val distortion = 18 * curveProgress * dist
                val maxDelta = dist * 29
                val dx1 = maxDelta * simplexNoise2D(seed * 1, curveProgress, timeOffset = timeProgress, radius = distortion)
                val dy1 = maxDelta * simplexNoise2D(seed * 2, curveProgress, timeOffset = timeProgress, radius = distortion)
                val dx2 = maxDelta * simplexNoise2D(seed * 3, curveProgress, timeOffset = timeProgress, radius = distortion)
                val dy2 = maxDelta * simplexNoise2D(seed * 4, curveProgress, timeOffset = timeProgress, radius = distortion)
                drawer.fill = fgColor.opacify(0.9 - distortion/15.0)
                drawer.circle(p1.x + dx1, p1.y + dy1, 3.0)
                drawer.circle(p2.x + dx2, p2.y + dy2, 3.0)
            }
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.stroke = null

                drawSpiralArms(617, timeProgress)
                drawSpiralArms(43, timeProgress)
                drawSpiralArms(251, timeProgress)
            }
            post(FrameBlur()) {
            }
        }

//        extend(ScreenRecorder()) {
//            frameRate = 60
//            frameClock = true
//        }
        extend {
            timeProgress = ((frameCount * FRAME_SPEED) % TOTAL_FRAMES) / TOTAL_FRAMES
            drawer.translate(width/2.0, height/2.0)
            drawer.rotate(4 * -360 * timeProgress)
            composite.draw(drawer)
            if (frameCount >= TOTAL_FRAMES) {
                application.exit()
            }
        }
    }
}