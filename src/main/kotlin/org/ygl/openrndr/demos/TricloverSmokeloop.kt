package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private const val SPIRAL_ARMS = 16
private const val PATH_POINTS = 1024
private const val TOTAL_FRAMES = 360
private const val DELAY_FRAMES = TOTAL_FRAMES / 4

fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    program {

        val camera = OrbitalCamera(eye = Vector3(0.0, 0.0, -800.0), lookAt = Vector3.UNIT_Z)

        val params = @Description("params") object {
            @DoubleParameter("noise radius", 1.0, 200.0)
            var noiseRadius = 10.0
            @DoubleParameter("noise scale", 0.1, 10.0)
            var noiseScale = 0.87
            @DoubleParameter("noise magnitude", 1.0, 200.0)
            var magnitude = 32.0
        }

        val fgColor = ColorRGBa.fromHex(0xFF9C4C)
        val bgColor = ColorRGBa.fromHex(0x1C333D)

        val noise = FastNoise()
        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .output("video/smoke-helix.mp4")
                .start()

        val angleOffsets = List(SPIRAL_ARMS) { Random.nextDouble() }
        val signs = List(SPIRAL_ARMS) { if (Random.nextBoolean()) 1 else -1 }
        val pathPoints = MutableList(PATH_POINTS) { Vector2.ZERO }

        fun computePathPoints(
                time: Double,
                pathIndex: Int,
                sign: Int
        ) {
            val angleOffset = angleOffsets[pathIndex]
            for (i in 0 until PATH_POINTS) {
                val pathProgress = i / PATH_POINTS.toDouble()
                //val easedProgress = (1 - (1 - pathProgress).pow(2))
                val easedProgress = pathProgress//.pow(2)
                val radius = pathProgress.pow(2).rangeMap(0, 1, 0, 200)
                val angle = 4 * PI * (pathProgress - time + angleOffset)
                val x0 = radius * cos(sign * angle)
                val z0 = radius * sin(sign * angle)
                val y0 = sign * (easedProgress * Configuration.Height * 0.7)

                val noiseScale = pathProgress * params.noiseScale

                noise.seed = 31 * pathIndex
                val dx = easedProgress * params.magnitude * noise.getSimplex(
                        x = x0 * noiseScale + params.noiseRadius * cos(2 * PI * time),
                        y = y0 * noiseScale + params.noiseRadius * sin(2 * PI * time)
                )
                noise.seed = 97 * pathIndex
                val dy = easedProgress * params.magnitude * noise.getSimplex(
                        x = x0 * noiseScale + params.noiseRadius * cos(2 * PI * time),
                        y = y0 * noiseScale + params.noiseRadius * sin(2 * PI * time)
                )
                noise.seed = 153 * pathIndex
                val dz = easedProgress * params.magnitude * noise.getSimplex(
                        x = params.noiseRadius * cos(2 * PI * time),
                        y = params.noiseRadius * sin(2 * PI * time)
                )
                //val dx = 0.0
                //val dz = 0.0

                val z = z0 + dz
                val distanceFactor = 800 / (600 + z)
                val x = x0 * distanceFactor
                val y = y0 * distanceFactor

                pathPoints[i] = Vector2(x + dx, y + dy)
            }
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                //drawer.stroke = ColorRGBa.BLACK
                //drawer.strokeWeight = 1.0
                drawer.stroke = null
                drawer.fill = fgColor
                drawer.rotate(45.0)

                val time = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (i in 0 until SPIRAL_ARMS) {
                    //val angleOffset = i / SPIRAL_ARMS.toDouble()
                    computePathPoints(time, i, signs[i])

                    for (j in pathPoints.indices) {
                        val p = j / pathPoints.size.toDouble()
                        drawer.fill = fgColor.opacify(1 - p)
                        drawer.circle(pathPoints[j], 5.0)
                    }

                    //drawer.circles(pathPoints, 6.0)
                }

            }
            if (Configuration.Recording) {
                post(GaussianBloom())
                post(FrameBlur())
            }
        }

        extend(camera)
        //extend(OrbitalControls(camera))
        //extend(GUI()) { add(params) }

        extend {
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
