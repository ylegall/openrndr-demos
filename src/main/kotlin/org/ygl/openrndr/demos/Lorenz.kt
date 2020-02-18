package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.renderTarget
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extras.camera.OrbitalCamera
import org.openrndr.math.Vector3
import org.ygl.openrndr.utils.RingBuffer
import org.ygl.openrndr.utils.color
import org.ygl.openrndr.utils.isolated
import org.ygl.openrndr.utils.isolatedWithTarget
import org.ygl.openrndr.utils.randomColorRGBa
import kotlin.random.Random

private const val A = 10.0
private const val B = 28.0
private const val C = 8.0/3

private const val DT = 0.1

private const val WIDTH = 800
private const val HEIGHT = WIDTH

private data class MutablePoint3D(
        var x: Double,
        var y: Double,
        var z: Double
)

private class Streak(
        var pos: MutablePoint3D,
        var color: ColorRGBa
) {
    private val points = RingBuffer<MutablePoint3D>(128).apply { add(pos.copy()) }

    fun update(dt: Double) {
        pos.x += A * (pos.y - pos.x) * dt
        pos.y += (pos.x * (B - pos.z)) * dt
        pos.z += (pos.x * pos.y - C * pos.z) * dt
        points.add(pos.copy())
    }

    fun draw(drawer: Drawer) {
        drawer.isolated {
            stroke = color
            strokeWeight = 4.0
            points.zipWithNext().forEach { (first, second) ->
                lineSegment(
                        Vector3(first.x, first.y, first.z),
                        Vector3(second.x, second.y, second.z)
                )
            }
        }
    }
}

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {
        val camera = OrbitalCamera(Vector3.UNIT_Z * 1000.0, Vector3.ZERO, 90.0, 0.1, 2000.0)
//        val controls = OrbitalControls(camera, keySpeed = 10.0)

        val bloom = GaussianBloom().apply {
            sigma = 4.0
            window = 5
            gain = 3.0
        }
        val drawTarget = renderTarget(width, height) { colorBuffer() }
        val bloomBuffer = colorBuffer(width, height)

        var elapsed = 0.0
        val streaks = List(32) {
            Streak(
                    MutablePoint3D(
                            Random.nextDouble(-20.0, 20.0),
                            Random.nextDouble(-20.0, 20.0),
                            Random.nextDouble(-20.0, 20.0)
                    ),
                    randomColorRGBa(a = 1.0)
            )
        }

//        extend(camera)
//        extend(controls) // adds both mouse and keyboard bindings

        extend {

            elapsed += deltaTime

            drawer.isolatedWithTarget(drawTarget) {
                perspective(90.0, width*1.0 / height, 0.1, 5000.0)
                scale(15.0)

                shadeStyle = shadeStyle {
                    vertexTransform = """x_viewMatrix = p_view;"""
                    parameter("view", camera.viewMatrix())
                }

                stroke = null
                background(ColorRGBa.BLACK)
                fill = color(0, 16, 32, 32)

                streaks.forEach {
                    it.update(deltaTime * DT)
                    it.draw(drawer)
                }
            }

            bloom.apply(drawTarget.colorBuffer(0), bloomBuffer)
            drawer.image(bloomBuffer)

        }
    }
}