package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.math.Vector2
import org.ygl.openrndr.utils.MutableVector2
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.mvector

private const val SPRING = 0.07
private const val DAMPING = 0.93
private const val FORCE_FACTOR = 87.0
private const val PARTICLE_SIZE = 16
private const val RADIUS = PARTICLE_SIZE / 2.0 + 3.0


fun main() = application {

    configure {
        title = "Spring Particles"
        width = Configuration.width
        height = Configuration.height
    }
    val WIDTH_IN_PARTICLES = Configuration.width / PARTICLE_SIZE

    val mouseForce = mvector(0, 0)
    val anchorForce = mvector(0, 0)

    class Particle(x: Number, y: Number) {
        val position = MutableVector2(x, y)
        val anchor = position.copy()
        val velocity = MutableVector2(0, 0)

        fun update(mousePosition: Vector2?) {
            val mouseForce = if (mousePosition != null) {
                mouseForce.set(mousePosition.x - position.x, mousePosition.y - position.y)
                val distanceSquared = mouseForce.squaredLength()
                if (distanceSquared > 5000f || distanceSquared < 0.001f) {
                    mvector(0, 0)
                } else {
                    mouseForce /= distanceSquared
                    mouseForce *= FORCE_FACTOR
                    mouseForce
                }
            } else {
                mvector(0, 0)
            }
            anchorForce.set(anchor.x - position.x, anchor.y - position.y)
            anchorForce *= SPRING
            anchorForce -= mouseForce
            velocity += anchorForce
            velocity *= DAMPING
            position += velocity
            position.x = position.x.coerceIn(0.0, Configuration.width.toDouble())
            position.y = position.y.coerceIn(0.0, Configuration.height.toDouble())
        }
    }

    program {
        val bgColor = color(32, 64, 127)
        val particleColor = color(32, 213, 127)
        var mousePosition: Vector2? = null
        val particles = Array(WIDTH_IN_PARTICLES * Configuration.height / PARTICLE_SIZE) {
            Particle(
                    (it % WIDTH_IN_PARTICLES) * PARTICLE_SIZE + PARTICLE_SIZE/2,
                    (it / WIDTH_IN_PARTICLES) * PARTICLE_SIZE + PARTICLE_SIZE/2
            )
        }

        mouse.moved.listen {
            mousePosition = mouse.position
        }

        drawer.stroke = null
        extend {
            val positions = particles.map {
                it.update(mousePosition)
                it.position.toVector2()
            }
            drawer.background(bgColor)
            drawer.fill = particleColor
            drawer.circles(positions, RADIUS)
        }
    }
}