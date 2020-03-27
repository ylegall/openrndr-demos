package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.fx.distort.StackRepeat
import org.openrndr.extra.shadestyles.radialGradient
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.ygl.fastnoise.lerp
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.radians
import org.ygl.openrndr.utils.rotate
import org.ygl.openrndr.utils.vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


private const val TOTAL_FRAMES = 360
private const val DELAY_FRAMES = TOTAL_FRAMES / 2

private const val RING_POINTS = 20
private const val PATH_POINTS = 512
private const val DELAY_FACTOR = 3


fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    val innerPoints = ArrayList<Vector2>(RING_POINTS)
    val outerPoints = ArrayList<Vector2>(RING_POINTS)
    for (i in 0 until RING_POINTS) {
        val angle = 2 * PI * (i / RING_POINTS.toDouble())
        outerPoints.add(Vector2((Configuration.Width/2 - 20) * cos(angle), (Configuration.Height/2 - 20) * sin(angle)))
        innerPoints.add(Vector2(20 * cos(angle), 20 * sin(angle)))
    }

    val pathbuffer = MutableList(PATH_POINTS) { vector2(0, 0) }

    fun getOuterRingPosition(index: Int, progress: Double): Vector2 {
        val angleOffset = index / RING_POINTS.toDouble()
        val angle = (2 * PI * (progress - angleOffset)).radians()
        return outerPoints[index] + (Vector2.UNIT_Y.rotate(angle) * 20.0)
    }

    fun getInnerRingPosition(index: Int, progress: Double): Vector2 {
        val angleOffset = index / RING_POINTS.toDouble()
        val angle = (2 * PI * (progress + angleOffset)).radians()
        return innerPoints[index] + (Vector2.UNIT_Y.rotate(angle) * 10.0)
    }

    fun getPathPoints(index: Int, timeProgress: Double): List<Vector2> {
        for (i in pathbuffer.indices) {
            val pathProgress = i / PATH_POINTS.toDouble()
            val delay = timeProgress - DELAY_FACTOR * pathProgress
            val endPoint = getOuterRingPosition(index, delay)
            val startPoint = getInnerRingPosition(index, delay)
            val x = lerp(startPoint.x, endPoint.x, pathProgress)
            val y = lerp(startPoint.y, endPoint.y, pathProgress)
            pathbuffer[i] = Vector2(x, y)
        }
        return pathbuffer
    }

    program {

        val videoTarget = renderTarget(Configuration.Width, Configuration.Height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(Configuration.Width, Configuration.Height)
                .output("video/dark-nova.mp4")
                .frameRate(60)
                .start()

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
            }

            layer {
                draw {
                    drawer.stroke = null
                    drawer.fill = ColorRGBa.WHITE

                    val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()
                    for (index in 0 until RING_POINTS) {
                        val pathPoints = getPathPoints(index, timeProgress)
                        drawer.circles(pathPoints, 2.0)
                    }
                }
                post(StackRepeat()) {
                    repeats = 8
                    zoom = 1.0 / 9.0
                    rotation = 45.0
                }
            }

            post(GaussianBloom())

            if (Configuration.Recording) {
                post(FrameBlur())
            }
        }

        extend {
            drawer.translate(width/2.0, height/2.0)
            drawer.scale(1.5)
            drawer.shadeStyle = radialGradient(
                    ColorRGBa.WHITE,
                    ColorRGBa.BLUE,
                    length = 0.5
            )

            if (Configuration.Recording) {
                if (frameCount >= TOTAL_FRAMES + DELAY_FRAMES) {
                    videoWriter.stop()
                    application.exit()
                }
                if (frameCount >= DELAY_FRAMES) {
                    drawer.isolatedWithTarget(videoTarget) {
                        composite.draw(this)
                    }
                    videoWriter.frame(videoTarget.colorBuffer(0))
                }
            } else {
                composite.draw(drawer)
            }
        }
    }
}