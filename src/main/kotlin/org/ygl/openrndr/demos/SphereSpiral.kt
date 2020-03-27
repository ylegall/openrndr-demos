package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.noise.lerp
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val POINTS = 2048
private const val TOTAL_FRAMES = 420
private const val DELAY_FRAMES = TOTAL_FRAMES / 2

fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    program {

        val camera = OrbitalCamera(Vector3.UNIT_Z * -500.0, Vector3.ZERO, 90.0, 0.1, 1000.0)
        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .output("video/sphere-spiral.mp4")
                .start()

        fun getCurvePoint(
                pathProgress: Double,
                time: Double,
                flip: Boolean
        ): Vector3 {
            val sign = if (flip) -1 else 1
            val theta = sign * PI * pathProgress
            val curve = time * 12.0 * theta
            return Vector3(
                x = lerp(0.0, 400 * sin(theta) * cos(curve) * time, time),
                y = lerp(0.0, 400 * sin(theta) * sin(curve) * time, time),
                //x = 400 * sin(theta) * cos(curve) * time,
                //y = 400 * sin(theta) * sin(curve) * time,
                z = 400 * cos(theta) * time
            )
        }

        fun drawCurve(time: Double, flip: Boolean) {
            val points = ArrayList<Vector2>(POINTS)
            val radii = ArrayList<Double>(POINTS)

            val easedTime = 2 * if (time < 0.5) {
                time
            } else {
                1 - time
            }

            for (i in 0 until POINTS) {
                val pathProgress = i / POINTS.toDouble()
                val pos3D = getCurvePoint(pathProgress, easedTime, flip)
                val rotation = transform { rotate(Vector3.UNIT_Y, -90 + 180 * time) }
                val pos2D = (rotation * drawer.projection) * pos3D
                //val pos2D = drawer.projection * pos3D

                val perspective = if (flip) {
                    1000 / (1000 - pos2D.z)
                } else {
                    1000 / (pos2D.z - 1000)
                }

                val radius = pos2D.z.rangeMap(-500, 500, 2.0, 10.0)
                radii.add(radius)
                if (flip) {
                    points.add(Vector2(-pos2D.x * perspective, pos2D.y * perspective))
                } else {
                    points.add(Vector2(pos2D.x * perspective, pos2D.y * perspective))
                }
            }
            val color = ColorRGBa.WHITE.copy(g = easedTime)
            drawer.fill = color
            drawer.stroke = color
            drawer.strokeWeight = 1.0
            drawer.circles(points, radii)
        }

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)

                val progress = frameCount / TOTAL_FRAMES.toDouble()
                val time = progress % 1.0
                drawCurve(time, flip = false)
                drawCurve(time, flip = true)
            }
            if (Configuration.recording) {
                post(GaussianBloom())
                post(FrameBlur())
            }
        }

        extend(camera)
        extend {
            if (Configuration.recording) {
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