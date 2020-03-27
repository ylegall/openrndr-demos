package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.transforms.transform
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.Radians
import org.ygl.openrndr.utils.rangeMap
import org.ygl.openrndr.utils.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


private const val TOTAL_FRAMES = 360
private const val DELAY_FRAMES = TOTAL_FRAMES / 4

private const val LAYERS = 100
private const val SHAPE_POINTS = 192

fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    program {

        val fgColor = ColorRGBa.fromHex(0x6FFFE9)
        val bgColor = ColorRGBa.fromHex(0x0B132B)
        //val fgColor = ColorRGBa.fromHex(0xF93131)
        //val bgColor = ColorRGBa.fromHex(0x30222F)
        val noise = FastNoise()
        val camera = OrbitalCamera(Vector3(0.0, 0.0, -500.0))
        //val camera = OrbitalCamera()
        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(30)
                .output("video/TriangleContours.mp4")
                .start()

        fun computeBaseShape(): ShapeContour {
            return contour {
                moveTo(0.0, 100.0)
                lineTo(75.0, 0.0)
                lineTo(-75.0, 0.0)
                lineTo(0.0, 100.0)
            }.transform( transform {
                translate(0.0, -50.0)
            })
        }
        //fun computeBaseShape(): ShapeContour {
        //    return contour {
        //        moveTo(-50.0, -50.0)
        //        lineTo(50.0, -50.0)
        //        lineTo(50.0, 50.0)
        //        lineTo(-50.0, 50.0)
        //        lineTo(-50.0, -50.0)
        //    }
        //}
        val baseShape = computeBaseShape()

        val params = @Description("params") object {
            @DoubleParameter("noise magnitude", 0.0, 200.0)
            var magnitude = 25.0

            @DoubleParameter("noise scale", 0.0, 20.0)
            var scale = 7.0

            @DoubleParameter("noise radius", 0.0, 100.0)
            var radius = 15.0
        }

        fun getNoiseValue(
                seed: Int,
                center: Double,
                pathProgress: Double,
                time: Double
        ): Double {
            noise.seed = seed
            return params.magnitude * noise.getSimplex(
                    x = params.scale * (center + 100 * cos(2 * PI * pathProgress)),
                    y = params.scale * (center + 100 * sin(2 * PI * pathProgress)),
                    z = params.radius * cos(2 * PI * time),
                    w = params.radius * sin(2 * PI * time)
            )
        }

        fun addNoiseToShapeContour(
                contour: ShapeContour,
                layerProgress: Double,
                time: Double
        ): ShapeContour {
            //val z = 500 / layerProgress.rangeMap(0, 1, 20, 500)
            val inverseProgress = (1 - layerProgress)
            val scale = (1 - inverseProgress.pow(3.0)).rangeMap(0, 1, 25, 0.70)
            val points = contour.equidistantPositions(SHAPE_POINTS)
            val center = params.radius * sin(2 * PI * ((time + layerProgress)))

            val newPoints = points.mapIndexed { idx, point ->
            //val newPoints = (points + points.first()).mapIndexed { idx, point ->
                val pathProgress = idx / SHAPE_POINTS.toDouble()
                val noiseX = getNoiseValue(1234, center, pathProgress, time)
                val noiseY = getNoiseValue(4321, center, pathProgress, time)
                val (x, y) = point * scale
                //val (x, y) = point.rotate(Radians(2 * PI * inverseProgress)) * scale
                val dx = noiseX * inverseProgress
                val dy = noiseY * inverseProgress
                Vector2(x + dx, y + dy)
            }

            return ShapeContour.fromPoints(newPoints, false)
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
            }

            layer {
                draw {
                    drawer.lineCap = LineCap.ROUND
                    drawer.strokeWeight = 2.0

                    val time = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                    for (i in 1 .. LAYERS) {
                        val layerProgres = i / LAYERS.toDouble()
                        val tranformedShape = addNoiseToShapeContour(baseShape, layerProgres, time)

                        drawer.stroke = fgColor.opacify(layerProgres)
                        drawer.contour(tranformedShape)
                    }

                }
            }

            if (Configuration.Recording) {
                post(FrameBlur())
            }
        }

        extend(camera)
        //extend(OrbitalControls(camera))
        //extend(GUI()) {
        //    add(params)
        //}

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