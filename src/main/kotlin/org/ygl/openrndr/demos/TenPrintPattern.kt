package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.LineCap
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.shadestyles.linearGradient
import org.ygl.kxa.Animatable
import org.ygl.kxa.EmptyAnimation
import org.ygl.kxa.ManualUpdateAnimationManager
import org.ygl.kxa.parallelAnimation
import org.ygl.kxa.util.millis
import org.ygl.openrndr.utils.color
import kotlin.random.Random


private const val SPACING = 40
private const val SPEED = 1.0

fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    class MutableLineSegment(
            var x1: Double,
            var y1: Double,
            var x2: Double,
            var y2: Double
    ) {
        constructor(
                x1: Number, y1: Number, x2: Number, y2: Number
        ): this(
                x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble()
        )
    }

    val bgColor = color(95, 57, 126)
    val animationManager = ManualUpdateAnimationManager()
    var currentAnimations: Animatable = EmptyAnimation
    val lines = mutableListOf<MutableLineSegment>()
    val newLines = mutableListOf<MutableLineSegment>()
    var done = false
    var y = 0.0

    program {

        val composite = compose {
            draw {
                with (drawer) {
                    lineCap = LineCap.ROUND
                    stroke = ColorRGBa.PINK
                    strokeWeight = 6.0
                    background(bgColor)
                    //fill = ColorRGBa.PINK
                }

                for (segment in lines) {
                    drawer.lineSegment(segment.x1, segment.y1, segment.x2, segment.y2)
                }
                for (segment in newLines) {
                    drawer.lineSegment(segment.x1, segment.y1, segment.x2, segment.y2)
                }
            }
//            post(ChromaticAberration()) {
//                aberrationFactor = 4.0// * sin(seconds)
//            }
            //post(BoxBlur())
        }

        extend {

            if (!done && !currentAnimations.isRunning()) {
                lines.addAll(newLines)
                newLines.clear()

                for (x in 0 .. width step SPACING) {
//                    newLines.add(MutableLineSegment(x, y, x + SPACING, y))
                    newLines.add(MutableLineSegment(x, y, x, y))
                }
                currentAnimations = parallelAnimation {
                    for (segment in newLines) {
                        animation {
                            if (Random.nextBoolean()) {
                                from(segment::y2 to y + SPACING)
                            } else {
                                from(segment::y1 to y + SPACING)
                            }
                            from(segment::x2 to (segment.x1 + SPACING))
                            duration(Random.nextLong(256, 512).millis)
                        }
                    }
                }
                animationManager.addAnimation(currentAnimations)
                y += SPACING
                if (y >= height) {
                    done = true
                    println("finished tiling")
                }
            }

            animationManager.update((seconds * SPEED).toLong())

            drawer.shadeStyle = linearGradient(color(255, 127, 0, 1), color(0, 255, 255, 1))
            composite.draw(drawer)
        }
    }
}
