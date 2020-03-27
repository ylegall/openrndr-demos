package org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.noise.lerp
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.ScreenRecorder
import org.openrndr.math.Vector2
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.utils.rangeMap
import org.ygl.openrndr.utils.vector2
import java.io.File
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private val TOTAL_FRAMES = if (Configuration.recording) 36 else 18
private val NUM_PATHS = if (Configuration.recording) 3000 else 2000
private val PATH_PARTICLES = if (Configuration.recording) 32 else 10
private val PATH_SNAPSHOTS = if (Configuration.recording) 200 else 100
private const val PATH_DELTA = 0.6
private const val MAX_MAGNITUDE = 54

private val params = @Description("settings") object {
    @DoubleParameter("noise scale x", 0.0, 4.0)
    var scaleX = 0.946

    @DoubleParameter("noise scale y", 0.0, 4.0)
    var scaleY = 0.871

    @DoubleParameter("noise magnitude x", 0.0, 100.0)
    var magX = 9.143

    @DoubleParameter("noise magnitude y", 0.0, 100.0)
    var magY = 7.932
}

private val noise = FastNoise()
private val fgColor = ColorRGBa.WHITE

private fun getSimplexNoise(x: Double, y: Double): Vector2 {
    val noiseX = noise.getSimplex(
            params.scaleX * x,
            params.scaleY * y
    )
    val noiseY = noise.getSimplex(
            params.scaleX * x,
            params.scaleY * y
    )
    return vector2(
            params.magX * noiseX + 5,
            params.magY * noiseY + 5
    )
}

private fun getFieldVector(x: Double, y: Double): Vector2 {
    return (
            getSimplexNoise(x, y)
    ).let {
        Vector2(
                5 + it.x * (x / Configuration.width.toDouble()),
                5 + it.y * (y / Configuration.height.toDouble())
        )
    }
}

fun main() = application {

    configure {
        width = Configuration.width
        height = Configuration.height
    }

    class Path {
        val timeOffset = Random.nextDouble(0.0, 1.0)
        val pointSnapshots = computePathSnapshots()

        private fun computePathSnapshots(): List<Vector2> {
            val snapshots = ArrayList<Vector2>(PATH_SNAPSHOTS)
            var x = Random.nextDouble(Configuration.width.toDouble())
            var y = Random.nextDouble(Configuration.width.toDouble())
            repeat(PATH_SNAPSHOTS) {
                snapshots.add(vector2(x, y))
                val fieldVector = getFieldVector(x, y)
                x += PATH_DELTA * fieldVector.x
                y += PATH_DELTA * fieldVector.y
            }
            return snapshots
        }

        fun draw(drawer: Drawer, progress: Double) {
            val adjustedProgress = (progress - timeOffset) % 1.0
            for (i in 0 until PATH_PARTICLES) {
                val pathProgress = (i + adjustedProgress)
                        .rangeMap(0, PATH_PARTICLES, 0, pointSnapshots.size - 1)
                        .coerceIn(0.0, pointSnapshots.size - 1 - 0.001)
                val startPoint = pathProgress.toInt()
                val endPoint = startPoint + 1
                val interpolationTime = pathProgress - startPoint
                val newX = lerp(pointSnapshots[startPoint].x, pointSnapshots[endPoint].x, interpolationTime)
                val newY = lerp(pointSnapshots[startPoint].y, pointSnapshots[endPoint].y, interpolationTime)

                val alpha = sin(PI * pathProgress / (pointSnapshots.size - 1)).pow(0.25)
                drawer.fill = fgColor.opacify(alpha)
                drawer.circle(newX, newY, 2.5)
            }
        }
    }

    val paths = MutableList(NUM_PATHS) { Path() }
    fun computePaths() {
        for (i in paths.indices) {
            paths[i] = Path()
        }
    }

    program {

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
                val progress = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()

                drawer.stroke = null
                for (path in paths) {
                    path.draw(drawer, progress)
                }
            }
            //if (Configuration.Recording) { post(FrameBlur()) }
        }

        extend(GUI()) {
            add(params)
            onChange { _, _ -> computePaths() }
            loadParameters(File("data/flow-params.json"))
        }

        if (Configuration.recording) {
            extend(ScreenRecorder()) {
                frameRate = 30
                frameClock = true
            }
        }

        extend {
            composite.draw(drawer)
            if (Configuration.recording) {
                if (frameCount >= TOTAL_FRAMES) {
                    application.exit()
                }
            }
        }
    }
}