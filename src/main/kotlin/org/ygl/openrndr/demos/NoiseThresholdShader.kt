package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.ScreenRecorder
import org.ygl.openrndr.utils.rect
import java.io.File


private const val TOTAL_FRAMES = 360 * 1

private val params = @Description("params") object {
    @DoubleParameter("noise scale", 0.0, 4.0)
    var noiseScale = 5.0

    @DoubleParameter("noise radius", 0.0, 100.0)
    var noiseRadius = 0.5
}

// https://github.com/ashima/webgl-noise/blob/master/src/noise4D.glsl
private val noiseFragShader = File("data/shaders/simplexNoise4D.glsl").readText()

fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    program {

        val composite = compose {
            draw {
                drawer.fill = ColorRGBa.WHITE
                drawer.stroke = null

                val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                drawer.shadeStyle = shadeStyle {
                    fragmentPreamble = noiseFragShader

                    fragmentTransform = """
                        const float PI_2 = 6.283185307179586;
                        
                        vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                        
                        vec4 noiseInput = vec4(
                            p_scale * pos.x,
                            p_scale * pos.y,
                            p_radius * cos(PI_2 * p_time),
                            p_radius * sin(PI_2 * p_time)
                        );
                        
                        float noiseValue = 4.5 * snoise(noiseInput);
                        
                        vec4 black = vec4(0.1686, 0.176, 0.2588, 1);
                        vec4 red = vec4(0.937, 0.137, 0.235, 1);
                        vec4 green = vec4(0.349, 0.788,  0.647, 1);

                        vec4 color = vec4(0.93, 0.91, 0.82, 1); 
                        if (noiseValue > 0) {
                            if (noiseValue > 0.3) {
                                color = black;
                            }
                            if (noiseValue > 1.5) {
                                color = red;
                            }
                            if (noiseValue > 2.7) {
                                color = black;
                            }
                        } else {
                            if (noiseValue < -0.3) {
                                color = black;
                            }
                            if (noiseValue < -1.5) {
                                color = green;
                            }
                            if (noiseValue < -2.7) {
                                color = black;
                            }
                        }

                        x_fill = color;
                        
                    """.trimIndent()

                    parameter("scale", params.noiseScale)
                    parameter("radius", params.noiseRadius)
                    parameter("time", timeProgress)
                }

                drawer.rect(0, 0, Configuration.Width, Configuration.Height)
            }
            if (Configuration.Recording) {
                post(FrameBlur())
            }
        }

        if (Configuration.Recording) {
            extend(ScreenRecorder()) {
                frameRate = 60
                frameClock = true
            }
        }

        extend {
            composite.draw(drawer)

            if (Configuration.Recording) {
                if (frameCount >= TOTAL_FRAMES) {
                    application.exit()
                }
            }
        }
    }
}