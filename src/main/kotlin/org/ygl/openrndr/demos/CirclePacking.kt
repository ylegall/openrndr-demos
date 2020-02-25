package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.ygl.openrndr.utils.ColorMap
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random


/*
https://coolors.co/a7f2eb-97e5e2-8590a0-5d557f-4e2568
A6F1C5
96E5BB
849DA0
55627F
2D2568
*/

private const val CIRCLES_PER_FRAME = 3
private const val MAX_RADIUS = 88.0
private const val GROWTH_SPEED = 0.2
private const val MAX_ATTEMPTS = 17

fun main() = application {

    configure {
        width = 800
        height = 800
    }

    val colorMap = ColorMap(listOf(
            ColorRGBa.fromHex(0x2A9D8F),
            ColorRGBa.fromHex(0xE9C46A),
            ColorRGBa.fromHex(0xF4A261),
            ColorRGBa.fromHex(0xE76F51)
    ))

    class MutableCircle(
            val x: Double,
            val y: Double,
            var radius: Double,
            var growing: Boolean
    ) {
        fun overlaps(circle: MutableCircle): Boolean {
            val distance = distance(circle.x, circle.y)
            return distance > 0 && distance < (radius + circle.radius)
        }

        fun contains(x1: Double, y1: Double) = distance(x1, y1) < radius

        fun update(dt: Double) {
            if (growing) {
                radius = (radius + GROWTH_SPEED * dt).coerceAtMost(MAX_RADIUS)
                if (radius >= MAX_RADIUS) {
                    growing = false
                }
            }
        }

        fun draw(drawer: Drawer) {
            drawer.fill = colorMap[radius / MAX_RADIUS]
            drawer.circle(x, y, radius)
        }

        private fun distance(x: Double, y: Double) = sqrt((x - this.x).pow(2) + (y - this.y).pow(2))
    }

    program {

        val bgColor = ColorRGBa.fromHex(0x64653)
        val circles = HashSet<MutableCircle>()
        var circleLimitReached = false

        fun newCircle(): MutableCircle? {
            var attempts = 0
            while (attempts < MAX_ATTEMPTS) {
                val x = Random.nextDouble(width.toDouble())
                val y = Random.nextDouble(height.toDouble())
                if (circles.none { it.contains(x, y) }) {
                    return MutableCircle(x, y, 1.0, true)
                }
                attempts++
            }
            return null
        }

        fun addNewCircles(numCircles: Int) {
            repeat(numCircles) {
                val circle = newCircle()
                if (circle != null) {
                    circles.add(circle)
                } else {
                    circleLimitReached = true
                    println("circle limit reached")
                    return
                }
            }
        }

        val composite = compose {
            draw {

                if (!circleLimitReached) {
                    addNewCircles(CIRCLES_PER_FRAME)
                }

                drawer.background(bgColor)
                drawer.stroke = null
                circles.forEach { circle ->
                    if (circles.any { circle.overlaps(it) }) {
                        circle.growing = false
                    }
                    circle.update(seconds)
                    circle.draw(drawer)
                }
            }
        }

        extend {
            composite.draw(drawer)
        }
    }
}