package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.ScreenRecorder
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.rangeMap
import kotlin.random.Random

private const val SPEED = 200

fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    val MAX_DEPTH = Configuration.width / 2.0

    class Star(
            var x: Double,
            var y: Double,
            var z: Double
    ) {
        var z1 = z
    }

    program {

        val stars = List(512) {
            Star(
                    Random.nextDouble(-width/2.0, width/2.0),
                    Random.nextDouble(-height/2.0, height/2.0),
                    Random.nextDouble(MAX_DEPTH)
            )
        }

        val composite = compose {
            draw {
                drawer.lineCap = LineCap.ROUND
                drawer.stroke = ColorRGBa.RED
                drawer.strokeWeight = 2.0
                drawer.fill = ColorRGBa.WHITE

                for (star in stars) {
                    star.z1 = star.z + 1
                    star.z -= (SPEED * deltaTime)
                    if (star.z < 1) {
                        star.z = MAX_DEPTH
                        star.x = Random.nextDouble(-width/2.0, width/2.0)
                        star.y = Random.nextDouble(-height/2.0, height/2.0)
                        star.z1 = star.z
                    }

                    val x1 = (star.x/star.z).rangeMap(0.0, 1.0, 0, width/2.0)
                    val y1 = (star.y/star.z).rangeMap(0.0, 1.0, 0, height/2.0)
                    val x2 = (star.x/star.z1).rangeMap(0.0, 1.0, 0, width/2.0)
                    val y2 = (star.y/star.z1).rangeMap(0.0, 1.0, 0, height/2.0)

                    val blue = star.z.rangeMap(0, MAX_DEPTH, 255, 64)
                    drawer.stroke = color(255 - blue/1.6, blue/1.3, blue)
                    drawer.strokeWeight = star.z.rangeMap(0, MAX_DEPTH, 8, 0)
                    drawer.lineSegment(x1, y1, x2, y2)
                }
            }
            post(GaussianBloom()) {
                sigma = 4.0
                window = 2
                gain = 15.0
            }
        }

//        extend(NoClear())
        extend(ScreenRecorder())
        extend {
//            drawer.fill = color(0, 1, 2, 128)
//            drawer.rect(0.0, 0.0, Configuration.Width, Configuration.Height)
            drawer.translate(width/2.0, height/2.0)
            drawer.rotate(seconds * 21)
            drawer.scale(0.7)
            composite.draw(drawer)

            if (frameCount > 300) {
                application.exit()
            }
        }
    }

}