package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.draw.LineCap
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.fx.color.ChromaticAberration
import org.openrndr.shape.LineSegment
import org.ygl.openrndr.demos.util.RingBuffer
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.rangeMap
import kotlin.math.cos
import kotlin.math.sin

private const val WIDTH = 800
private const val HEIGHT = WIDTH
private const val SEGMENTS = 32
private const val GAP = 0.1

private fun x1(dt: Double) = WIDTH/2.3 * cos(dt) + 47 * sin(dt * 2.3)

private fun y1(dt: Double) = HEIGHT/2.7 * sin(dt) - 53 * cos(dt * 1.7)

private fun x2(dt: Double) = WIDTH/2.5 * sin(dt / 1.3) + 59 * cos(dt * 0.5)

private fun y2(dt: Double) = HEIGHT/2.4 * cos(dt * 1.7) - 41 * sin(dt * 2.1)

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val segments = RingBuffer<LineSegment>(SEGMENTS)

        val composite = compose {
            draw {
                for (i in 0 until SEGMENTS) {
                    val x1 = x1(seconds + (i * GAP))
                    val y1 = y1(seconds + (i * GAP))
                    val x2 = x2(seconds + (i * GAP))
                    val y2 = y2(seconds + (i * GAP))
                    segments.add(LineSegment(x1, y1, x2, y2))
                }

                drawer.translate(width/2.0, height/2.0)
                drawer.background(color(0, 0, 0))

                for (i in 0 until segments.size) {
                    val segment = segments[i]
                    val blue = (i.rangeMap(0, segments.size, 0, 255)).toInt()
                    val red = 255 - blue

                    drawer.lineCap = LineCap.ROUND
                    drawer.stroke = color(red, blue / 1.5, blue)
                    drawer.strokeWeight = 6.0
                    drawer.lineSegment(segment)
                }
            }
            post(BoxBlur()) {
            }
            post(ChromaticAberration()) {
                aberrationFactor = 0.0 + 50.0 * sin(seconds * 0.2)
            }
        }

        extend {
            composite.draw(drawer)
        }
    }
}