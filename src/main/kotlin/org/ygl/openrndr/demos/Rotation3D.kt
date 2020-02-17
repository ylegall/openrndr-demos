package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.ffmpeg.VideoWriter
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.randomColorRGBa
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val WIDTH = 800
private const val WIDTH_2 = WIDTH / 2.0
private const val HEIGHT = WIDTH
private const val DT = 0.5

private data class Point(
        var x: Double,
        var y: Double,
        var z: Double,
        val color: ColorRGBa = randomColorRGBa(a = 1.0)
)

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    val points = List(64) {
        Point(
                Random.nextDouble(-WIDTH_2, WIDTH_2),
                Random.nextDouble(-HEIGHT/2.0, HEIGHT/2.0),
                Random.nextDouble(-WIDTH_2, WIDTH_2)
        )
    }

    program {
        val bloom = GaussianBloom().apply {
            sigma = 4.0
            window = 5
            gain = 3.0
        }

        val videoWriter = VideoWriter.create()
                .size(width, height)
                .output("output.mp4")
                .frameRate(60)
                .start()

        val drawTarget = renderTarget(width, height) {
            colorBuffer()
        }
        val bloomTarget = renderTarget(width, height) {
            colorBuffer()
        }

        //var elapsed = 0.0
//        extend(ScreenRecorder())

        extend {
            //elapsed += (deltaTime * DT)
            //angle = Math.PI * (elapsed % 360) / 180.0
            val angle = (Math.PI / 180.0) * (frameCount.toDouble() * DT)

            drawer.isolatedWithTarget(drawTarget) {
                stroke = null
                translate(width/2.0, height/2.0)
                background(ColorRGBa.BLACK)
                for (point in points.sortedBy { -it.z }) {
                    fill = point.color
                    val x = point.z * sin(angle) + point.x * cos(angle)
                    val z = point.z * cos(angle) - point.x * sin(angle)
                    val zDistance = (width/2.0 + z) / width
                    circle(x, point.y, 4.0 + 8 * zDistance)
                }
            }

            bloom.apply(drawTarget.colorBuffer(0), bloomTarget.colorBuffer(0))

            videoWriter.frame(bloomTarget.colorBuffer(0))
            drawer.image(bloomTarget.colorBuffer(0))

            if (frameCount >= (720 / DT)) {
                println("done")
                videoWriter.stop()
                application.exit()
            }
        }
    }
}
