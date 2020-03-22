package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.lerp
import org.openrndr.extra.noise.simplex
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.smoothstep
import org.openrndr.shape.contour
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.ColorMap
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val WIDTH = 800
private const val HEIGHT = 800

private const val LAYERS = 180
private const val POINTS_PER_LAYER = 200

private const val BORDER = 40
private const val FRONT_X = -WIDTH/2 + BORDER
private const val FRONT_Y = HEIGHT/2 - BORDER
private const val MAX_DIST = 20

private const val TOTAL_FRAMES = 360
private const val RECORDING = false
private const val FRAME_DELAY = 120

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val noise = FastNoise()
        val bgColor = ColorRGBa.fromHex(0x081D30)
        val colorMap = ColorMap(listOf(
                ColorRGBa.fromHex(0x7D3E9B),
                ColorRGBa.fromHex(0xD367AF),
                ColorRGBa.fromHex(0xF49FC7)
        ))
        val seeds = Array(LAYERS) { Random.nextInt(10, 10000) }
        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .start()

        val parameters = @Description("settings") object {
            @DoubleParameter("noise radius x", 0.0, 200.0)
            var radiusX = 49.447

            @DoubleParameter("noise radius y", 0.0, 200.0)
            var radiusY = 57.627

            @DoubleParameter("noise magnitude", 0.0, 400.0)
            var magnitude = 243.0

            @DoubleParameter("noise speed", 0.0, 10.0)
            var speed = 2.0
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.fill = bgColor

                val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (layer in 0 until LAYERS) {
                    val layerProgress = layer / (LAYERS - 1).toDouble()
                    val perspectiveFactor = layerProgress.rangeMap(0, 1, 1, MAX_DIST) / MAX_DIST
                    val startX = FRONT_X * perspectiveFactor - (LAYERS - layer)
                    val y = FRONT_Y * perspectiveFactor - 2 * (LAYERS - layer)
                    val layerWidth = 2 * abs(FRONT_X) * perspectiveFactor + 2 * (LAYERS - layer)

                    val curve = contour {
                        for (point in 0 until POINTS_PER_LAYER) {
                            val pathProgress = (point / POINTS_PER_LAYER.toDouble())
                            val x = startX + pathProgress * layerWidth

                            val xNoiseDamp = when {
                                pathProgress < 0.45 -> smoothstep(0.0, 0.45, pathProgress)
                                pathProgress >= 0.55 -> smoothstep(0.0, 0.45, 1 - pathProgress)
                                else -> 1.0
                            }

                            val yNoiseDamp = when {
                                layerProgress < 0.40 -> layerProgress.rangeMap(0, 0.4, 0, 1)
                                layerProgress >= 0.60 -> layerProgress.rangeMap(0.6, 1, 1, 0)
                                else -> 1.0
                            }

                            val angle = 2 * PI * (pathProgress - parameters.speed * timeProgress)

                            noise.seed = seeds[layer]
                            val noiseValue = noise.getSimplex(
                                    x = parameters.radiusX * cos(angle),
                                    y = y + parameters.radiusY * sin(angle)
                            )

                            val noiseMagnitdue = parameters.magnitude * xNoiseDamp * yNoiseDamp
                            val dy = noiseMagnitdue * (1 + noiseValue)/2

                            moveOrLineTo(x, y - dy)
                        }
                        lineTo(cursor.x, cursor.y + BORDER)
                        lineTo(startX, y + BORDER)
                        close()
                    }

                    drawer.strokeWeight = layerProgress.rangeMap(0, 1, 0.5, 1.8)
                    drawer.stroke = colorMap[layerProgress]//.opacify(alpha)
                    drawer.contour(curve)
                }
            }
            if (RECORDING) { post(FrameBlur()) }
        }

//        extend(GUI()) {
//            compartmentsCollapsedByDefault = false
//            add(parameters)
//        }

        extend {
            drawer.translate(width/2.0, height/2.0)
            if (RECORDING) {
                drawer.isolatedWithTarget(videoTarget) {
                    composite.draw(drawer)
                }

                if (frameCount >= TOTAL_FRAMES + FRAME_DELAY) {
                    videoWriter.stop()
                    application.exit()
                }
                if (frameCount >= FRAME_DELAY) {
                    videoWriter.frame(videoTarget.colorBuffer(0))
                }

            } else {
                composite.draw(drawer)
            }

        }
    }
}