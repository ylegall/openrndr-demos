package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.ColorType
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.loadImage
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.layer
import org.openrndr.extra.noise.lerp
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.shape.Rectangle
import org.ygl.kxa.ease.Ease
import org.ygl.openrndr.utils.isolated
import kotlin.random.Random

private const val TILE_SIZE = 64
private const val TILE_STROKE = 16

private const val TOTAL_FRAMES = 360
private const val PAUSE_FRAMES = TOTAL_FRAMES / 4
private const val DELAY_FRAMES = TOTAL_FRAMES + PAUSE_FRAMES

fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    program {

        val fgColor = ColorRGBa.fromHex(0xE56B70)
        val bgColor = ColorRGBa.fromHex(0x353A47)

        val tileSize = TILE_SIZE.toDouble()
        val videoTarget = renderTarget(Configuration.Width, Configuration.Height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(Configuration.Width, Configuration.Height)
                .frameRate(60)
                .output("video/TruchetTiles.mp4")
                .start()

        // decorate initial tile
        val tile = loadImage("data/images/truchet-tile.png")
        val tileClip = tile.bounds

        class RotationSequence(
                val startRotation: Int,
                val rotations: Pair<Int, Int> = listOf(
                        0 to 0,
                        360 to 360,
                        -360 to -360,
                        180 to -180,
                        -180 to 180
                ).random()
        )

        val rotations = Array(Configuration.Height / TILE_SIZE) {
            Array(Configuration.Width / TILE_SIZE) {
                RotationSequence(Random.nextInt(4) * 90)
            }
        }

        val composite = compose {
            colorType = ColorType.FLOAT32
            draw {
                drawer.background(bgColor)
            }

            //if (Configuration.Recording) {
            //    post(FrameBlur())
            //}

            layer {
                draw {

                    val frameProgress = if (frameCount < PAUSE_FRAMES) {
                        0.0
                    } else {
                        ((frameCount - PAUSE_FRAMES) % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()
                    }

                    val time = if (frameProgress < 0.5) {
                        Ease.EXP_INOUT(frameProgress * 2) / 2
                    } else {
                        Ease.EXP_INOUT((frameProgress - 0.5) * 2) / 2 + 0.5
                    }

                    for (row in 0 until Configuration.Height / TILE_SIZE) {
                        for (col in 0 until Configuration.Width / TILE_SIZE) {

                            val x = col * tileSize
                            val y = row * tileSize

                            val rotationSequence = rotations[row][col]
                            val rotation = if (time < 0.5) {
                                val start = rotationSequence.startRotation
                                val stop = rotationSequence.startRotation + rotationSequence.rotations.first
                                lerp(start.toDouble(), stop.toDouble(), time)
                            } else {
                                val start = rotationSequence.startRotation + rotationSequence.rotations.first
                                val stop = rotationSequence.startRotation + rotationSequence.rotations.first +
                                        rotationSequence.rotations.second
                                lerp(start.toDouble(), stop.toDouble(), time)
                            }

                            drawer.isolated {
                                val dstClip = Rectangle(-tileSize / 2, -tileSize / 2, tileSize, tileSize)
                                translate(x + tileSize / 2, y + tileSize / 2)
                                rotate(rotation)
                                image(tile, tileClip, dstClip)
                            }
                        }
                    }
                }
            }
        }

        extend {
            if (Configuration.Recording) {
                if (frameCount >= TOTAL_FRAMES + DELAY_FRAMES) {
                    videoWriter.stop()
                    application.exit()
                }
                if (frameCount >= DELAY_FRAMES) {
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