package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.extra.fx.blur.BoxBlur
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.extra.fx.blur.HashBlur
import org.openrndr.extra.fx.shadow.DropShadow
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.ffmpeg.MP4Profile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.ffmpeg.VideoWriterProfile
import org.openrndr.math.Vector2
import org.ygl.openrndr.utils.Degrees
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.rangeMap
import org.ygl.openrndr.utils.rect
import org.ygl.openrndr.utils.rotate
import org.ygl.openrndr.utils.vector2
import kotlin.math.abs


private const val WIDTH = 800
private const val HEIGHT = 800
private const val SPEED = 1.0
private const val ROWS = 16
private const val ROWS_2 = ROWS / 2
private const val GAP = 42

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    val bgColor1 = ColorRGBa.fromHex(0x292e53)
    val bgColor2 = ColorRGBa.fromHex(0xc95d63)
    val fgColor1 = ColorRGBa.fromHex(0xc08497)
    val fgColor2 = ColorRGBa.fromHex(0xece2d0)
    val shadowColor = color(53, 31, 67)
    val baseShift = Vector2(0.0, 16.0)
    val frameOffset = 90
    val totalFrames = 360 + frameOffset
    var progress = 0.0

    class Dot(
            val pos: Vector2,
            val xMag: Double,
            val yMag: Double,
            var offset: Double
    ) {
        fun draw(drawer: Drawer, dt: Double) {
            val shift = baseShift.rotate(Degrees(offset + dt))
            val newX = pos.x + (xMag * shift.x)
            val newY = pos.y + (yMag * shift.y)
            drawer.circle(newX, newY, 14.0)
        }
    }

    program {

        val dots = mutableListOf<Dot>()

        for (row in -ROWS_2 .. ROWS_2) {
            val cols = 2 * (ROWS_2 - abs(row)) + 1
            var col = -cols/2
            repeat(cols) {
                val dot = Dot(
                        pos = vector2(col * GAP, row * GAP),
                        xMag = if (col == 0) 0.0 else col.rangeMap(-ROWS_2, ROWS_2, -0.9, 0.9),
                        yMag = row.rangeMap(-ROWS_2, ROWS_2, -1.5, 1.5),
                        offset = row.rangeMap(-ROWS_2/2, ROWS_2/2, 0, 180)
                )
                dots.add(dot)
                col++
            }
        }

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
            }
            layer {
                layer {
                    draw {
                        drawer.shadeStyle = linearGradient(bgColor1, bgColor2, rotation = 0.0)
                        drawer.fill = ColorRGBa.WHITE
                        drawer.rect(0, 0, width, height)
                    }
                }
            }
            layer {
                blend(Normal()) {
                    clip = true
                }
                draw {
                    with (drawer) {
                        translate(width/2.0, height/2.0)
                        scale(0.8)
                        shadeStyle = linearGradient(fgColor1, fgColor2, rotation = 180.0) //360 * progress)
                        stroke = null
                        fill = ColorRGBa.WHITE
                        for (dot in dots) {
                            dot.draw(drawer, 360 * 2 * progress)
                        }
                    }
                }
                post(DropShadow()) {
                    color = shadowColor
                    xShift = 0.0
                    yShift = -4.0
                }
            }
        }

        extend(ScreenRecorder()) {
            profile = MP4Profile().apply { mode(MP4Profile.WriterMode.Lossless) }
            frameClock = true
            frameRate = 60
            timeOffset = frameOffset.toDouble()
        }
        extend {
            progress = (frameCount * SPEED) / totalFrames
            composite.draw(drawer)
            if (frameCount >= totalFrames) { application.exit() }
        }
    }

}