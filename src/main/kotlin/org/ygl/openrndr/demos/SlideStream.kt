package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector3
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val POINTS = 256

private const val TOTAL_FRAMES = 360
private const val DELAY_FRAMES = TOTAL_FRAMES / 4


fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    program {

        val camera = OrbitalCamera(eye = Vector3(0.0, 0.0, -50.0), lookAt = Vector3.UNIT_Z)

        val params = @Description("params") object {
            @DoubleParameter("radius", 1.0, 500.0)
            var radius = 100.0

            @DoubleParameter("noiseRadius", 1.0, 200.0)
            var noiseRadius = 128.0

            @DoubleParameter("noiseMagnitude", 1.0, 200.0)
            var magnitude = 16.0
        }

        val noise = FastNoise()
        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .output("video/SlideStream.mp4")
                .start()

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
                //drawer.stroke = ColorRGBa.BLACK
                //drawer.strokeWeight = 1.0
                drawer.stroke = null
                drawer.fill = ColorRGBa.WHITE

                val time = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (x in -100 .. 100 step 4) {
                    for (point in 0 until POINTS) {

                        val pathProgress = point / POINTS.toDouble()
                        val angle = -PI/2 + (PI * pathProgress)
                        val y = (params.radius - 10) + params.radius * sin(angle)
                        val z = params.radius * cos(angle)

                        val t = (time + pathProgress)

                        noise.seed = 1234
                        val noiseX = noise.getSimplex(
                                x = params.noiseRadius * cos(2 * PI * t),
                                y = params.noiseRadius * sin(2 * PI * t)
                        )

                        noise.seed = 4321
                        val noiseZ = noise.getSimplex(
                                x = x + params.noiseRadius * cos(2 * PI * t),
                                y = y + params.noiseRadius * sin(2 * PI * t)
                        )

                        val dx = params.magnitude * pathProgress * noiseX
                        val dz = params.magnitude * pathProgress * noiseZ

                        val depthFactor = 50 / (z + dz)

                        val radius = z.rangeMap(1, 100, 2.7, 1.3)
                        drawer.circle((x + dx) * depthFactor, y * depthFactor, radius)
                    }
                }
            }
            if (Configuration.recording) {
                post(FrameBlur())
            }
        }

        extend(camera)
        //extend(OrbitalControls(camera))

        //extend(GUI()) {
        //    add(params)
        //}

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
