package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.ygl.kxa.Interpolators
import org.ygl.openrndr.demos.particles.CurveParticle
import org.ygl.openrndr.demos.particles.StretchParticle
import org.ygl.openrndr.demos.util.ColorInterpolator
import org.ygl.openrndr.utils.randomColorRGBa
import org.ygl.openrndr.utils.vector2

fun main() = application {

    configure {
        height = Configuration.Height
        width = Configuration.Width
    }

    Interpolators.registerInterpolator(ColorRGBa::class, ColorInterpolator())

    fun randomVector() = vector2((0..Configuration.Width).random(), (0..Configuration.Height).random())

    val bgColor  = ColorRGBa(0.23, 0.25, 0.24, 1.0)
    val particles = List(128) {
        StretchParticle(randomVector(), randomColorRGBa(a = 1.0), (12..18).random())
    }
    var clickCount = 0

    program {

        mouse.clicked.listen { event ->
            clickCount++
            if (clickCount % 2 == 0) {
                particles.forEach {
                    it.moveTo(randomVector(), randomColorRGBa(a = 1.0), (0L..300L).random())
                }
            } else {
                particles.forEach {
                    it.moveTo(event.position, ColorRGBa.WHITE, (0L..300L).random())
                }
            }
        }

        extend {
            drawer.background(bgColor)
            particles.forEach { it.draw(drawer) }
        }
    }
}