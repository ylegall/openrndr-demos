package org.ygl.openrndr.demos

import org.openrndr.KeyModifier
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadFont
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.Rectangle
import org.openrndr.shape.contour
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.rect
import kotlin.random.Random

private const val WIDTH = 800
private const val HEIGHT = 800
private const val START_POINTS = 7
private const val START_SIZE = 27

private const val INITIAL_FEED = 0.0343 //
private const val INITIAL_KILL = 0.0653 //
private const val INITIAL_DA = 0.436 //
private const val INITIAL_DB = 0.156 //


fun main() = application {
    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        var feed = INITIAL_FEED
        var kill = INITIAL_KILL
        var dA = INITIAL_DA
        var dB = INITIAL_DB
        var dt = 1.0

        val colorA = ColorRGBa.fromHex(0x1BC6A1)
        val colorB = ColorRGBa.fromHex(0x1B2549)

        val buffer1 = renderTarget(width, height) { colorBuffer() }
        val buffer2 = renderTarget(width, height) { colorBuffer() }

        var prev = buffer2
        var curr = buffer1

        val font = loadFont("/usr/share/fonts/truetype/ubuntu/Ubuntu-B.ttf", 128.0)

        fun resetParams() {
            feed = INITIAL_FEED
            kill = INITIAL_KILL
            dA = INITIAL_DA
            dB = INITIAL_DB
            dt = 1.0
        }

        fun swapBuffers() {
            if (curr === buffer2) {
                prev = buffer2
                curr = buffer1
            } else {
                prev = buffer1
                curr = buffer2
            }
        }

        mouse.clicked.listen {
            drawer.isolatedWithTarget(prev) {
                drawer.stroke = null
                drawer.fill = ColorRGBa.GREEN
                rectangle(mouse.position, START_SIZE.toDouble(), START_SIZE.toDouble())
            }
        }

        mouse.dragged.listen {
            drawer.isolatedWithTarget(prev) {
                drawer.stroke = null
                drawer.fill = ColorRGBa.GREEN
                rectangle(mouse.position, START_SIZE.toDouble(), START_SIZE.toDouble())
//                circle(mouse.position, START_SIZE.toDouble())
            }
        }

        keyboard.keyDown.listen { keyEvent ->
            if (KeyModifier.SHIFT in keyEvent.modifiers) {
                when (keyEvent.name) {
                    "k" -> { kill -= 0.001; println("kill: $kill") }
                    "f" -> { feed -= 0.001; println("feed: $feed") }
                    "a" -> { dA -= 0.01; println("dA: $dA") }
                    "b" -> { dB -= 0.01; println("dB: $dB") }
                    "t" -> { dt -= 0.01; println("dt: $dt") }
                }
            } else {
                when (keyEvent.name) {
                    "k" -> { kill += 0.001; println("kill: $kill") }
                    "f" -> { feed += 0.001; println("feed: $feed") }
                    "a" -> { dA += 0.01; println("dA: $dA") }
                    "b" -> { dB += 0.01; println("dB: $dB") }
                    "t" -> { dt += 0.01; println("dt: $dt") }
                    "e" -> application.exit()
                    "r" -> resetParams()
                }
            }
        }

        // initialize everything to chemical A
        drawer.isolatedWithTarget(prev) {
            stroke = null
            fill = ColorRGBa.RED
            rect(0.0, 0.0, width, height)
        }

        // seed the grid with chemical B
        val dots = mutableListOf<Pair<Double, Double>>()
        repeat(START_POINTS) {
            val x = Random.nextDouble(width.toDouble())
            val y = Random.nextDouble(height.toDouble())
            dots.add(Pair(x, y))
            drawer.isolatedWithTarget(prev) {
                stroke = null
                fill = ColorRGBa.GREEN
                //rectangle(x, y, START_SIZE.toDouble(), START_SIZE.toDouble())
                rectangle(x, y, Random.nextDouble(5.0, 20.0), Random.nextDouble(5.0, 20.0))
//                circle(x, y, START_SIZE.toDouble())
            }
        }

        val composite = compose {
            draw {
                drawer.isolatedWithTarget(prev) {
                    drawer.fontMap = font
                    drawer.fill = ColorRGBa.RED
                    text("Indeed", width/2.0 - 164, height / 2.0 + 48)
                }

                drawer.isolatedWithTarget(curr) {
                    shadeStyle = shadeStyle {
                        fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    float dx = 1.0 / u_viewDimensions.x;
                    float dy = 1.0 / u_viewDimensions.y;
                    
                    vec2 tc0 = pos + vec2(-dx, -dy);
                    vec2 tc1 = pos + vec2(0.0, -dy);
                    vec2 tc2 = pos + vec2(+dx, -dy);
                    vec2 tc3 = pos + vec2(-dx, 0.0);
                    vec2 tc4 = pos + vec2(0.0, 0.0);
                    vec2 tc5 = pos + vec2(+dx, 0.0);
                    vec2 tc6 = pos + vec2(-dx, +dy);
                    vec2 tc7 = pos + vec2(0.0, +dy);
                    vec2 tc8 = pos + vec2(+dx, +dy);
                   
                    vec4 col0 = texture(p_prev, tc0);
                    vec4 col1 = texture(p_prev, tc1);
                    vec4 col2 = texture(p_prev, tc2);
                    vec4 col3 = texture(p_prev, tc3);
                    vec4 col4 = texture(p_prev, tc4);
                    vec4 col5 = texture(p_prev, tc5);
                    vec4 col6 = texture(p_prev, tc6);
                    vec4 col7 = texture(p_prev, tc7);
                    vec4 col8 = texture(p_prev, tc8);

                    vec2 uv = texture(p_prev, pos).rg;
                   
                    vec4 laplace = (
                        0.05 * col0 + 0.2 * col1 + 0.05 * col2 +
                        0.20 * col3 - 1.0 * col4 + 0.20 * col5 +
                        0.05 * col6 + 0.2 * col7 + 0.05 * col8
                    );
                   
                    float rate = uv.r * uv.g * uv.g;
                    float du = p_dA * laplace.r - rate + p_feed * (1.0 - uv.r);
                    float dv = p_dB * laplace.g + rate - (p_feed + p_kill) * uv.g;  
                   
                    float u = clamp(uv.r + du * p_dt, 0.0, 1.0); 
                    float v = clamp(uv.g + dv * p_dt, 0.0, 1.0);

                    x_fill = vec4(u, v, 0.0, 1.0);
                """.trimIndent()
                        parameter("prev", prev.colorBuffer(0))
                        parameter("kill", kill)
                        parameter("feed", feed)
                        parameter("dA", dA)
                        parameter("dB", dB)
                        parameter("dt", dt)
                    }
                    stroke = null
                    fill = ColorRGBa.GRAY
                    rect(0.0, 0.0, width, height)
                }

                drawer.shadeStyle = shadeStyle {
                    fragmentTransform = """
                    vec2 pos = vec2(c_boundsPosition.x, 1 - c_boundsPosition.y);
                    vec2 uv = texture(p_texture, pos).rg;
                    float c = smoothstep(0.1, 0.9, uv.r - uv.g);
                    float gradient_factor = 1 - step(0.6, uv.r);
                    float pct = c_boundsPosition.x;
                    vec4 overlay = mix(vec4(0, 1, 0, 0), vec4(0, 0, 1, 0), pct);
                    x_fill = (mix(p_colorA, p_colorB, c) + overlay * gradient_factor);
                """.trimIndent()
                    parameter("texture", curr.colorBuffer(0))
                    parameter("colorA", colorA)
                    parameter("colorB", colorB)
                }

                drawer.image(curr.colorBuffer(0))
                curr.colorBuffer(0).copyTo(prev.colorBuffer(0))
                swapBuffers()
            }
            // post(GaussianBloom())
        }

//        extend(ScreenRecorder()) {
//            frameRate = 60
//            frameClock = true
//        }
        extend {
            composite.draw(drawer)
        }
    }
}