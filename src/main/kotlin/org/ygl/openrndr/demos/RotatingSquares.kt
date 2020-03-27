package org.ygl.openrndr.demos

import org.openrndr.application
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.isolated
import kotlin.math.PI
import kotlin.math.cos

private const val SQUARES = 64
private const val SPIN_RATE = 0.2

fun main() = application {

    configure {
        width = Configuration.Width
        height = Configuration.Height
    }

    class Square(
            val width: Double,
            val spinSpeed: Double
    )

    val squares = (1 .. SQUARES).map {
        val percent = it / SQUARES.toDouble()
        val squareWidth = Configuration.Width * percent
        Square(squareWidth, (Configuration.Width / 2.0 - squareWidth / 2.0))

    }.reversed()

    val bgColor = color(255, 107, 0)
    var totalTime = 0.0

    program {

        extend {
            totalTime = (SPIN_RATE * deltaTime + totalTime) % 360
            drawer.background(bgColor)
            drawer.translate(width / 2.0, width / 2.0)

            squares.forEachIndexed { idx, it ->
                drawer.isolated {
                    val offset = (PI * 360 * (idx / SQUARES.toDouble())) / 180
                    val colorOffset = 255 * (1 + cos(totalTime + offset)) / 2
                    fill = color(colorOffset, 43 + SQUARES - idx, 255 - colorOffset)
                    stroke = color(255 - colorOffset, 43 + SQUARES - idx, colorOffset)
                    rotate(totalTime * it.spinSpeed)
                    translate(-it.width / 2.0, -it.width / 2.0)
                    drawer.rectangle(0.0, 0.0, it.width, it.width)
                }
            }
        }
    }
}