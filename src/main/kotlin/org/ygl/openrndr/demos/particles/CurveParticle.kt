package org.ygl.openrndr.demos.particles

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle
import org.openrndr.shape.ShapeContour
import org.openrndr.shape.contour
import org.ygl.kxa.animateParallel
import org.ygl.kxa.ease.Ease
import org.ygl.kxa.util.millis
import org.ygl.openrndr.utils.isolated
import org.ygl.openrndr.utils.vector2


class CurveParticle(
        var pos: Vector2,
        var color: ColorRGBa,
        initialRadius: Number = 10.0
) {

    private var radius = initialRadius.toDouble()
    private var contour: ShapeContour = Circle(pos, radius).contour
    private var t1 = 0.0
    private var t2 = 0.0
    private val baseDuration = 1000

    fun moveTo(
            newPos: Vector2,
            newColor: ColorRGBa,
            initialDelay: Long = 0
    ) {
        t1 = 0.01
        t2 = 0.01

        contour = contour {
            moveTo(pos)
            val avgY = (pos.y + newPos.y) / 2
            curveTo(vector2(pos.x, avgY), vector2(newPos.x, avgY), newPos)
        }

        pos = newPos

        animateParallel {
            animation {
                from(this@CurveParticle::t1 to 1.0)
                easedBy(Ease.EXP_OUT)
                duration(baseDuration.millis)
                delay(initialDelay.millis)
            }
            animation {
                from(this@CurveParticle::t2 to 1.0)
                easedBy(Ease.EXP_OUT)
                duration(baseDuration.millis)
                delay((initialDelay + baseDuration/2).millis)
            }
            animation {
                from(this@CurveParticle::color to newColor)
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