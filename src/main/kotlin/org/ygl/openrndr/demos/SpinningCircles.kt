package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.compositor.blend
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.fx.blend.Normal
import org.openrndr.extra.shadestyles.linearGradient
import org.openrndr.ffmpeg.MP4Profile
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.ygl.openrndr.utils.isolated
import org.ygl.openrndr.utils.rect
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin


private const val WIDTH = 800
private const val HEIGHT = 800
private const val RINGS = 12
private const val TOTAL_FRAMES = 420

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    val bgColor = ColorRGBa.fromHex(0x375267)
    val fgColor = ColorRGBa.fromHex(0xff8781)

    val rings = List(RINGS) { idx ->
        Circle(Vector2.ZERO, (idx + 1) * 32.0).shape.outline
    }

    program {

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.stroke = null
                drawer.fill = fgColor

                rings.forEachIndexed { index, ring ->
                    drawer.isolated {
                        val numPoints = 4 + 4 * index
                        val rotationSpeed = 0
                        val progress = frameCount / TOTAL_FRAMES.toDouble()
                        val offset = 16 * (RINGS - index)
                        translate(offset * cos(2 * PI * progress), offset * sin(2 * PI * progress))
                        rotate(360.0 * progress * rotationSpeed)
                        circles(ring.equidistantPositions(numPoints), 10.0)
                    }
                }
            }
        }

//        extend(ScreenRecorder()) {
//            profile = MP4Profile().apply { mode(MP4Profile.WriterMode.Lossless) }
//            frameClock = true
//            frameRate = 60
//            //timeOffset = FRAME_DELAY.toDouble()
//        }
        extend {
            drawer.translate(width/2.0, height/2.0)
            composite.draw(drawer)
//            if (frameCount >= TOTAL_FRAMES) { application.exit() }
        }
    }

}