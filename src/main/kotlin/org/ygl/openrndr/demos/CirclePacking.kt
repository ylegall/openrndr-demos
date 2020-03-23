package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.loadImage
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.openrndr.utils.ColorMap
import org.ygl.openrndr.utils.distanceFrom
import org.ygl.openrndr.utils.rangeMap
import java.util.BitSet
import kotlin.math.ceil
import kotlin.math.min


/*
https://coolors.co/47638e-648cb7-b4c4be-ccaf92-ea8760
47638E
648CB7
B4C4BE
CCAF92
EA8760
*/

private const val WIDTH = 800
private const val HEIGHT = 800
private const val CIRCLES_PER_FRAME = 5
private const val INITIAL_RADIUS = 0.0
private const val MAX_RADIUS = 72.0

private val bgColor = ColorRGBa.fromHex(0x47638E)

private val colorMap = ColorMap(listOf(
        ColorRGBa.fromHex(0x648CB7),
        ColorRGBa.fromHex(0xB4C4BE),
        ColorRGBa.fromHex(0xCCAF92),
        ColorRGBa.fromHex(0xEA8760)
))

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    data class MutableCircle(
            val pos: Vector2
    ) {
        var radius: Double = INITIAL_RADIUS
        var isGrowing: Boolean = true
    }

    fun <T: Number> mutableCircle(x: T, y: T) = MutableCircle(Vector2(x.toDouble(), y.toDouble()))

    class CircleGrid
    {
        val allCircles = mutableSetOf<MutableCircle>()
        val openTiles = mutableSetOf<Pair<Int, Int>>()
        val closedTiles = Array(HEIGHT) { BitSet(WIDTH) }

        var growingCircles = 0; private set
        var circleLimitReached = false; private set

        val pointCache = object: LinkedHashMap<Double, Collection<Pair<Int, Int>>>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Double, Collection<Pair<Int, Int>>>?): Boolean {
                return this.size > 64
            }
        }

        init {
            for (y in 0 until HEIGHT) {
                for (x in 0 until WIDTH) {
                    openTiles.add(x to y)
                }
            }
        }

        private fun canAddCircle(): Boolean {
            return !circleLimitReached && growingCircles < CIRCLES_PER_FRAME
        }

        private fun dynamicGrowthRate(): Double {
            return min(allCircles.size, 2048).rangeMap(1, 2048, 32, 2)
        }

        fun addRandomCircles() {
            val circlesToAdd = allCircles.size.coerceAtMost(4096).rangeMap(0, 4096, 2, 48).toInt()
            repeat(circlesToAdd) {
                if (canAddCircle()) {
                    if (openTiles.isNotEmpty()) {
                        val point = openTiles.random()
                        allCircles.add(mutableCircle(point.first, point.second))
                        removePoint(point)
                        growingCircles++
                    } else {
                        circleLimitReached = true
                        println("circle limit reached")
                    }
                }
            }
        }

        fun removePoint(point: Pair<Int, Int>) {
            openTiles.remove(point)
            closedTiles[point.second][point.first] = true
        }

        fun update(dt: Double) {
            allCircles.filter { it.isGrowing }.forEach { circle ->
                val beforePoints = getBoundingBoxPoints(circle)
                circle.radius += dynamicGrowthRate() * dt
                val coveredPoints = getBoundingBoxPoints(circle)
                (coveredPoints - beforePoints).forEach { point ->
                    if (closedTiles[point.second][point.first]) {
                        circle.isGrowing = false
                        growingCircles--
                        //check(growingCircles >= 0) { "growing circles was $growingCircles" }
                    }
                    removePoint(point)
                }
            }
        }

        fun draw(drawer: Drawer) {
            allCircles.groupBy { it.radius }.forEach { (radius, circles) ->
                drawer.fill = colorMap[radius / MAX_RADIUS]
                drawer.circles(circles.map { it.pos }, radius)
            }
        }

        private fun getBoundingBoxPoints(circle: MutableCircle): List<Pair<Int, Int>> {
            var points = pointCache[circle.radius]

            if (points == null) {
                val min = -circle.radius.toInt()
                val max = ceil(circle.radius).toInt()
                points = (min .. max).flatMap { y ->
                    (min .. max).map { x ->
                        x to y
                    }
                }.filter {
                    Vector2.ZERO.distanceFrom(it.first, it.second) <= circle.radius
                }
                pointCache[circle.radius] = points
            }

            return points.map { Pair(
                    (it.first + circle.pos.x.toInt()).coerceIn(0 until WIDTH),
                    (it.second + circle.pos.y.toInt()).coerceIn(0 until HEIGHT)
                )
            }
        }
    }

    program {

        val circleGrid = CircleGrid()

        // load logo mask:
        val image = loadImage("data/Indeed-logo-full.png")
        image.shadow.download()
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val color = image.shadow[x, y]
                if (color.r + color.g + color.b < 1.0) {
                    circleGrid.removePoint(x to y)
                }
            }
        }

        val composite = compose {
            draw {
                drawer.background(bgColor)
                drawer.stroke = null
                drawer.strokeWeight = 0.0
                circleGrid.addRandomCircles()
                circleGrid.update(deltaTime)
                circleGrid.draw(drawer)
            }
        }

        extend(ScreenRecorder())
        extend {
            composite.draw(drawer)
            if (frameCount % 100 == 0) {
                //println(circleGrid.openTiles.size)
                println(circleGrid.allCircles.size)
            }
        }
    }
}