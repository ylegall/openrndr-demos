package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.isolated

private const val SQUARES = 64
private const val SCREEN_WIDTH = 800
private const val SCREEN_HEIGHT = 800
private const val SPIN_RATE = 0.2

fun main() = application {

    configure {
        width = SCREEN_WIDTH
        height = SCREEN_HEIGHT
    }

    class Square(
            val width: Double,
            val spinSpeed: Double,
            val color: ColorRGBa
    )

    val squares = (1 .. SQUARES).map {
        val percent = it / SQUARES.toDouble()
        val color = color(255 * percent, 43 + SQUARES - it, 250 * (1 - percent))
        val squareWidth = SCREEN_WIDTH * percent

        Square(squareWidth, (SCREEN_WIDTH / 2.0 - squareWidth / 2.0), color)
    }.reversed()

    val bgColor = color(255, 107, 0)
    val strokeColor = color(16, 4, 222)
    var totalTime = 0.0

    program {

        extend {
            totalTime = (SPIN_RATE * deltaTime + totalTime) % 360
            drawer.background(bgColor)
            drawer.stroke = strokeColor
            drawer.translate(width / 2.0, width / 2.0)

            squares.forEach {
                drawer.isolated {
                    fill = it.color
                    rotate(totalTime * it.spinSpeed)
                    translate(-it.width / 2.0, -it.width / 2.0)
                    drawer.rectangle(0.0, 0.0, it.width, it.width)
                }
            }
        }
    }
}