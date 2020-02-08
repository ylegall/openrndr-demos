package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.shape.Rectangle
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.isolated

private const val SQUARES = 64
private const val SCREEN_WIDTH = 800
private const val SCREEN_HEIGHT = 800
private const val SPIN_RATE = 0.1

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

    val bgColor = color(255, 127, 0)

    val squares = (1 .. SQUARES).map {

        val color = color(
                255 * (it/SQUARES.toDouble()),
                64 + SQUARES - it,
                255 * (1 - it/SQUARES.toDouble())
        )

        val squareWidth = (SCREEN_WIDTH) * (it/SQUARES.toDouble())
        Square(
                squareWidth,
                (SCREEN_WIDTH / 2.0 - squareWidth / 2.0),// * (1 - SQUARES.toDouble() / it),
                color)
    }.reversed()

    var totalTime = 0.0

    program {
        extend {
            drawer.background(bgColor)
            totalTime = (SPIN_RATE * deltaTime + totalTime) % 360

            drawer.translate(width/2.0, width/2.0)

            squares.forEach {
                drawer.isolated {
                    fill = it.color
                    rotate(totalTime * it.spinSpeed)
                    translate(-it.width/2.0, -it.width/2.0)
                    shape(Rectangle(0.0, 0.0, it.width, it.width).shape)
                }
            }
        }
    }
}