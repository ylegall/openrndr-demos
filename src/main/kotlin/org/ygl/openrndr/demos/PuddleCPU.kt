package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extras.camera.AxisHelper
import org.openrndr.extras.camera.GridHelper
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.extras.camera.OrbitalControls
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.math.Vector3
import org.openrndr.math.smoothstep
import org.ygl.openrndr.utils.distanceFrom
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.vector2
import kotlin.math.sqrt
import kotlin.random.Random

private const val WIDTH = 800
private const val HEIGHT = WIDTH
private const val INSET = 640
private const val TOTAL_FRAMES = 180
private const val DELAY_FRAMES = TOTAL_FRAMES
private const val RIPPLE_WIDTH = 40
private const val MAX_REFRACT = 30.0
private const val MAX_RADIUS = 250
private const val RECORDING = true

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val background = loadImage("data/images/branches-2.png")
        val renderTarget = renderTarget(INSET, INSET) { colorBuffer() }
        val shadow = renderTarget.colorBuffer(0).shadow

        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create().frameRate(60).size(width, height).start()

        class Ripple(
                var pos: Vector2,
                var offset: Double
        ) {
            fun radius(progress: Double): Double {
                return MAX_RADIUS * sqrt((progress + offset) % 1.0)
            }
        }

        val ripples = List(11) {
            Ripple(
                    vector2(Random.nextInt(WIDTH), Random.nextInt(HEIGHT)),
                    Random.nextDouble()
            )
        }

        fun getPixelValueForRipple(
                row: Int,
                col: Int,
                ripple: Ripple,
                progress: Double
        ): Double {
            val radius = ripple.radius(progress)
            val dist = ripple.pos.distanceFrom(col, row)
            val strength = 1.0 - radius / MAX_RADIUS.toDouble()
            val refraction = when (dist) {
                in radius .. radius + RIPPLE_WIDTH/2 -> {
                    if (dist > radius + RIPPLE_WIDTH/4) {
                        1 - smoothstep(radius + RIPPLE_WIDTH/4, radius + RIPPLE_WIDTH/2, dist)
                    } else {
                        smoothstep(radius, radius + RIPPLE_WIDTH/4, dist)
                    }
                }
                in radius - RIPPLE_WIDTH/2 .. radius -> {
                    if (dist > radius - RIPPLE_WIDTH/4) {
                        -1 + smoothstep(radius - RIPPLE_WIDTH/4, radius, dist)
                    } else {
                        -smoothstep(radius - RIPPLE_WIDTH/2, radius - RIPPLE_WIDTH/4, dist)
                    }
                }
                else -> 0.0
            }
            return refraction * strength
        }

        fun colorPixels() {
            val progress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

            for (row in 0 until INSET) {
                for (col in 0 until INSET) {
                    var totalRefract = 0.0
                    for (ripple in ripples) {
                        totalRefract += getPixelValueForRipple(row, col, ripple, progress)
                    }
                    totalRefract = (1.0 + totalRefract)/2.0
                    totalRefract = totalRefract.coerceIn(0.0, 1.0)
                    shadow[row, col] = ColorRGBa(totalRefract, totalRefract, totalRefract, 1.0)
                }
            }
            shadow.upload()
        }

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
            }

            layer {
                draw {
                    drawer.shadeStyle = shadeStyle {
                        fragmentTransform = """
                            vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                            vec2 coord = c_screenPosition;
                            
                            vec2 offset = vec2(p_maxRefract, p_maxRefract);
                            float amplitude = (0.5 - texture(p_distortion, pos).r) * 2;
                            
                            offset *= amplitude;
                            coord += offset;
                            coord.x /= u_viewDimensions.x;
                            coord.y /= u_viewDimensions.y;
                            x_fill = texture(p_bg, coord);
                            
                        """.trimIndent()
                        parameter("maxRefract", MAX_REFRACT)
                        parameter("distortion", renderTarget.colorBuffer(0))
                        parameter("bg", background)
                    }

                    drawer.fill = ColorRGBa.WHITE
                    drawer.stroke = null
                    drawer.rectangle((WIDTH - INSET)/2.0, (HEIGHT - INSET)/2.0, INSET.toDouble(), INSET.toDouble())
                }
            }
        }

        // extend(camera)
        // extend(controls)
        // extend(AxisHelper())
        // extend(GridHelper())

        extend {
            colorPixels()
            if (RECORDING) {
                if (frameCount >= TOTAL_FRAMES + DELAY_FRAMES) {
                    videoWriter.stop()
                    application.exit()
                } else if (frameCount >= DELAY_FRAMES) {
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