package org.ygl.openrndr.demos.particles

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.contour
import org.ygl.kxa.animateParallel
import org.ygl.kxa.ease.Ease
import org.ygl.kxa.util.millis
import org.ygl.openrndr.utils.isolated

class StretchParticle(
        var pos: Vector2,
        var color: ColorRGBa,
        initialRadius: Number = 10.0
) {
    private var radius = initialRadius.toDouble()
    private var contour = Circle(pos, radius).contour
    private var t1 = 0.0
    private var t2 = 0.0
    private val baseDuration = 1000

    fun moveTo(newPos: Vector2, newColor: ColorRGBa, initialDelay: Long = 0) {
        t1 = 0.01
        t2 = 0.01
        contour = contour {
            moveTo(pos.x, pos.y)
            lineTo(newPos.x, newPos.y)
        }
        pos = newPos
        animateParallel {
            animation {
                from(::t1 to 1.0)
                easedBy(Ease.EXP_OUT)
                duration(baseDuration.millis)
                delay(initialDelay.millis)
            }
            animation {
                from(::t2 to 1.0)
                easedBy(Ease.EXP_OUT)
                duration(baseDuration.millis)
                delay((initialDelay + baseDuration/2).millis)
            }
            animation {
                from(::color to newColor)
                duration((baseDuration/2 + baseDuration).millis)
                delay(initialDelay.millis)
            }
        }
    }

    fun draw(drawer: Drawer) {
        drawer.isolated {
            stroke = color
            lineCap = LineCap.BUTT
            strokeWeight = radius
            contour(contour.sub(t1, t2))
            circle(contour.position(t1), radius)
            circle(contour.position(t2), radius)
        }
    }

}