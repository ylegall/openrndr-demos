package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.shape.contour
import org.ygl.openrndr.utils.isolated
import org.ygl.openrndr.utils.isolatedWithTarget
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin


private const val BORDER = 40.0
private const val TOTAL_FRAMES = 420
private const val DELAY_FRAMES = TOTAL_FRAMES/2

private const val MAX_AMPLITUDE = 26.0
private const val PERIODS = 2.5


fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    program {

        val params = @Description("params") object {
            @DoubleParameter("lineGap", 0.1, 20.0)
            var lineGap = 8.0
            @DoubleParameter("xShift", -20.0, 20.0)
            var xShift = 8.0
        }

        fun computeCurve(frameProgress: Double) = contour {
            val time = sin(2 * PI * frameProgress)
            for (x in -100 until Configuration.width + 100) {
                val pathProgress = x / Configuration.width.toDouble()
                val angle = PERIODS * 2 * PI * (pathProgress - frameProgress/2)
                val y = time * MAX_AMPLITUDE * sin(angle)
                moveOrLineTo(x.toDouble(), y)
            }
        }

        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .output("video/beach-vibes.mp4")
                .start()

        val composite = compose {
            draw {
                drawer.shadeStyle = linearGradient(
                        ColorRGBa.fromHex(0xC7EFCF),
                        ColorRGBa.fromHex(0xF4F1BB)
                )
                drawer.rectangle(0.0, 0.0, Configuration.width.toDouble(), Configuration.height.toDouble())
            }
            if (Configuration.recording) {
                //post(FrameBlur())
            }

            layer {
                layer {
                    draw {
                        drawer.stroke = null
                        drawer.fill = ColorRGBa.WHITE
                        drawer.shadeStyle = linearGradient(
                                ColorRGBa.fromHex(0x9BC1BC),
                                ColorRGBa.fromHex(0x2D7DD2)
                        )
                        drawer.rectangle(
                                BORDER,
                                BORDER,
                                Configuration.width - 2 * BORDER,
                                Configuration.height - 2 * BORDER
                        )
                    }
                }
                layer {
                    blend(Normal()) {
                        clip = true
                    }
                    draw {
                        drawer.strokeWeight = 2.0
                        drawer.stroke = ColorRGBa.WHITE

                        val frameProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()
                        val curve1 = computeCurve(frameProgress)
                        val xOffsets = (-6..6).map { abs(it) * params.xShift }

                        var xShiftIndex = 0
                        var baseY = -20.0
                        while (baseY <= Configuration.height + 20) {
                            val dx = xOffsets[xShiftIndex]
                            drawer.isolated {
                                translate(dx, baseY)
                                contour(curve1)
                            }
                            xShiftIndex = (xShiftIndex + 1) % xOffsets.size
                            baseY += params.lineGap
                        }
                    }
                }
            }
        }

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
                        videoWriter.frame(videoTarget.colorBuffer(0))
                    }
                }
            } else {
                composite.draw(drawer)
            }
        }
    }
}