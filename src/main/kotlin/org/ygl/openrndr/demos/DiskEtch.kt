package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.draw.isolated
import org.openrndr.draw.loadImage
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.shape.Circle
import org.ygl.kxa.ease.Ease
import org.ygl.openrndr.utils.color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val WIDTH = 640
private const val HEIGHT = WIDTH
private const val TOTAL_FRAMES = 360

private const val RINGS = 64
private const val RING_GAP = 10
private const val DISK_POINTS = 720

private const val RECORDING = false

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        val rings = List(RINGS) { ringIndex ->
            val radius = RING_GAP * (1.0 + ringIndex)
            Circle(0.0, 0.0, radius)
        }
        val ringSegments = List(RINGS) { mutableListOf<Pair<Double, Double>>() }

        fun computeSegments() {
            val image = loadImage("data/yin-yang.png")
            val shadow = image.shadow.also { it.download() }

            for (ring in 0 until RINGS) {
                var a1: Double? = null
                var a2: Double? = null
                val radius = rings[ring].radius
                for (i in 0 until DISK_POINTS) {
                    val ringProgress = i / DISK_POINTS.toDouble()
                    val radians = 2.0 * PI * ringProgress
                    val x = (WIDTH/2 + radius * cos(radians)).toInt()
                    val y = (HEIGHT/2 + radius * sin(radians)).toInt()
                    if (x !in 0 until WIDTH) continue
                    if (y !in 0 until HEIGHT) continue
                    val color = shadow[x, y]
                    if (color.r + color.g + color.b > 2.5) {
                        if (a1 == null) {
                            a1 = ringProgress
                        }
                        a2 = ringProgress
                    } else {
                        if (a1 != null && a2 != null) {
                            ringSegments[ring].add(a1 to a2)
                            a1 = null
                            a2 = null
                        }
                    }
                }
            }
        }

        computeSegments()

        fun easeTimeOffset(time: Double, ringIndex: Int): Double {
            val ringProgress = ringIndex / RINGS.toDouble()
            val progress = (time + ringProgress) % 1.0
            val delayedProgress = if (progress < 0.35) {
                0.0
            } else {
                (progress - 0.35) / 0.65
            }
            return Ease.EXP_INOUT(delayedProgress)
        }

        fun drawSegments(colorRGBa: ColorRGBa, flip: Boolean) {
            val timeProgress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()
            for (ringIndex in ringSegments.indices) {
                val segments = ringSegments[ringIndex]
                val ring = rings[ringIndex]
                for ((start, stop) in segments) {
                    val adjustedProgress = easeTimeOffset(timeProgress, ringIndex)
                    val arc = ring.shape.outline.sub(start, stop)
                    val angle = adjustedProgress * if (flip) -360 else 360
                    drawer.isolated {
                        lineCap = LineCap.ROUND
                        strokeWeight = 3.0
                        stroke = colorRGBa
                        rotate(angle)
                        contour(arc)
                    }
                }
            }
        }

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
            }
            if (RECORDING) {
                post(FrameBlur())
            }
            layer {
                draw {
                    drawSegments(ColorRGBa.RED, flip = false)
                }
            }
            layer {
                blend(Add())
                draw {
                    drawSegments(color(0, 255, 255), flip = true)
                }
            }
        }

         extend(ScreenRecorder()) {
             frameRate = 60
             frameClock = true
         }

        extend {
            drawer.translate(width/2.0, height/2.0)
            composite.draw(drawer)

            if (RECORDING) {
                if (frameCount >= TOTAL_FRAMES) {
                    application.exit()
                }
            }
        }
    }
}