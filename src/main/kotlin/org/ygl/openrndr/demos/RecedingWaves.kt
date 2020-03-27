package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.noise.lerp
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.smoothstep
import org.openrndr.shape.contour
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.ColorMap
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


private const val LAYERS = 180
private const val POINTS_PER_LAYER = 200
private const val LAYER_BORDER = 30

private const val NOISE_MAGNITUDE = 331
private const val NOISE_SCALE = 1.8
private const val NOISE_RADIUS = 100.0

private const val TOTAL_FRAMES = 360
private const val FRAME_DELAY = 120

fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    val MIN_LAYER_WIDTH = Configuration.width / 2 - 2 * LAYER_BORDER
    val MAX_LAYER_WIDTH = Configuration.width - 2 * LAYER_BORDER
    val MIN_LAYER_HEIGHT = LAYER_BORDER
    val MAX_LAYER_HEIGHT = Configuration.height
    
    program {

        val noise = FastNoise()

        val bgColor = ColorRGBa.fromHex(0x0B132B)
        val colorMap = ColorMap(listOf(
                ColorRGBa.fromHex(0x1C2541),
                ColorRGBa.fromHex(0x3A506B),
                ColorRGBa.fromHex(0x5BC0BE),
                ColorRGBa.fromHex(0x6FFFE9)
        ))

        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .start()

        val lastCurveValues = MutableList(POINTS_PER_LAYER) { 0.0 }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.fill = bgColor

                val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                for (layer in 0 until LAYERS) {
                    val layerProgress = layer / LAYERS.toDouble()
                    val offsetLayerProgress = (layerProgress + timeProgress) % 1.0
                    val perspectiveProgress = layerProgress.pow(2)
                    val widthProgress = perspectiveProgress * (MAX_LAYER_WIDTH - MIN_LAYER_WIDTH)
                    val layerWidth = MIN_LAYER_WIDTH + widthProgress
                    val xOffset = (Configuration.width - MIN_LAYER_WIDTH) / 2.0
                    val startX = xOffset - widthProgress / 2.0

                    val y = MIN_LAYER_HEIGHT + (MAX_LAYER_HEIGHT - MIN_LAYER_HEIGHT) * perspectiveProgress
                    val yOffset = MIN_LAYER_HEIGHT + (MAX_LAYER_HEIGHT - MIN_LAYER_HEIGHT) * offsetLayerProgress

                    val curve = contour {
                        for (point in 0 until POINTS_PER_LAYER) {
                            val pathProgress = (point / POINTS_PER_LAYER.toDouble())
                            val x = startX + pathProgress * layerWidth

                            val xNoiseDamp = when {
                                pathProgress < 0.45 -> smoothstep(0.0, 0.45, pathProgress)
                                pathProgress >= 0.55 -> smoothstep(0.0, 0.45, 1 - pathProgress)
                                else -> 1.0
                            }
                            val yNoiseDamp = smoothstep(0.0, 1.0, layerProgress)
                            val noiseMagnitdue = NOISE_MAGNITUDE * xNoiseDamp * yNoiseDamp

                            val dy = noiseMagnitdue * (1 + noise.getSimplex(
                                    x = NOISE_SCALE * 0.5 * x,
                                    y = NOISE_SCALE * yOffset,
                                    z = NOISE_RADIUS * cos(2 * PI * timeProgress),
                                    w = NOISE_RADIUS * sin(2 * PI * timeProgress)
                            ))/2.0

                            lastCurveValues[point] = lerp(lastCurveValues[point], dy, 0.5) * yNoiseDamp

                            moveOrLineTo(x, y - lastCurveValues[point])
                        }
                        lineTo(cursor.x, cursor.y + LAYER_BORDER)
                        lineTo(startX, y + LAYER_BORDER)
                        close()
                    }

                    drawer.strokeWeight = layerProgress.rangeMap(0, 1, 0.5, 2.0)
                    //val alpha = layerProgress.rangeMap(0, 1, 0.4, 0.9)
                    drawer.stroke = colorMap[layerProgress]//.opacify(alpha)
                    drawer.contour(curve)
                }
            }
            if (Configuration.recording) { post(FrameBlur()) }
        }

//        extend(ScreenRecorder()) {
//            frameRate = 60
//            frameClock = true
//        }

        extend {
            if (Configuration.recording) {
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