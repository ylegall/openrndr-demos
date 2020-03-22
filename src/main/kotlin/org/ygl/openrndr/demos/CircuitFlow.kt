package org.ygl.openrndr.demos.org.ygl.openrndr.demos

import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.compositor.compose
import org.openrndr.extra.compositor.draw
import org.openrndr.extra.compositor.post
import org.openrndr.extra.fx.blur.GaussianBloom
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.ffmpeg.VideoWriter
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import org.ygl.fastnoise.FastNoise
import org.ygl.openrndr.demos.util.RingBuffer
import org.ygl.openrndr.utils.ColorMap
import org.ygl.openrndr.utils.mvector
import org.ygl.openrndr.utils.vector2
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.random.Random

private const val WIDTH = 640
private const val HEIGHT = 640
private const val PARTICLES = 100
private const val TOTAL_FRAMES = 360
//private const val DELAY_FRAMES = 180

private const val RECORDING = true

fun main() = application {

    configure {
        width = WIDTH
        height = HEIGHT
    }

    program {

        var frames = 0
        val noise = FastNoise()
        val videoTarget = renderTarget(width, height) { colorBuffer() }
        val videoWriter = VideoWriter.create()
                .size(width, height)
                .frameRate(60)
                .output("video/circuit.mp4")
                .start()

        val colors = ColorMap(listOf(
                ColorRGBa.fromHex(0x59A5F2),
                ColorRGBa.fromHex(0x2892D7),
                ColorRGBa.fromHex(0xA779FC),
                ColorRGBa.fromHex(0x7BEDBB),
                ColorRGBa.fromHex(0x80F7E5)
                //ColorRGBa.fromHex(0xAA23BC),
                //ColorRGBa.fromHex(0xE2291F),
                //ColorRGBa.fromHex(0xFC8300),
                //ColorRGBa.fromHex(0xFCB83D),
                //ColorRGBa.fromHex(0xF5E3B4)
        ))

        val params = @Description("params") object {
            @DoubleParameter("noise magnitude", 1.0, 200.0)
            var magnitude = 86.0

            @DoubleParameter("noise scale", 1.0, 60.0)
            var scale = 0.33
        }

        class Particle (
                val startPosition: Vector2
        ) {
            val color = colors[Random.nextDouble()]
            var positions = RingBuffer<Vector2>(TOTAL_FRAMES /2).apply { add(startPosition) }

            fun reset() {
                positions = RingBuffer(TOTAL_FRAMES /2)
                positions.add(startPosition)
            }

            fun update(dt: Double, time: Double) {
                val (x, y) = positions.last()

                if (time < 0.5) {

                    //noise.seed = 6324
                    val noiseX = noise.getSimplex(params.scale * x, params.scale * y)
                    noise.seed = 1982
                    val noiseY = noise.getSimplex(params.scale * x, params.scale * y)

                    val outwardForce = mvector(x - WIDTH /2, y - HEIGHT /2)
                    outwardForce.normalize()
                    outwardForce *= 0.4

                    val angle = atan2(noiseY + outwardForce.y, noiseX + outwardForce.x)
                    //val angle = atan2(noiseY, noiseX)
                    val roundedAngle = PI/4 * round(angle / (PI/4))
                    val newX = x + time * params.magnitude * dt * cos(roundedAngle)
                    val newY = y + time * params.magnitude * dt * sin(roundedAngle)
                    //val newX = x + params.magnitude * dt * cos(roundedAngle)
                    //val newY = y + params.magnitude * dt * sin(roundedAngle)

                    positions.add(Vector2(newX, newY))
                } else {
                    positions.add(Vector2(x, y))
                }
            }

            fun draw(drawer: Drawer, time: Double) {
                if (time < 0.5) {
                    drawer.stroke = color
                } else {
                    drawer.stroke = color.opacify(1 - 2 * (time - 0.5))
                }
                drawer.strokeWeight = 2.0
                drawer.lineSegments(positions.asSequence().windowed(2).flatten().toList())
                //drawer.lineSegments(positions.windowed(2).flatten())
            }
        }

        //fun getParticleStartPositions(): List<Particle> {
        //    val catOutline = loadImage("data/images/cat-outline.png")
        //    val particles = ArrayList<Particle>()
        //    val shadow = catOutline.shadow.also { it.download() }
        //    for (j in 0 until HEIGHT step 3) {
        //        for (i in 0 until WIDTH step 3) {
        //            val color = shadow[i, j]
        //            if (color.r != 1.0) println(color)
        //            if (color.r + color.g + color.b < 0.2) {
        //                particles.add(Particle(i, j))
        //            }
        //        }
        //    }
        //    return particles
        //}

        val particles = Rectangle(0.0, 0.0, 200.0, 200.0).shape.outline
                .equidistantPositions(PARTICLES)
                .map { Particle(vector2(WIDTH /2 - 100 + it.x, HEIGHT /2 - 100 + it.y)) }

        //val particles = List(PARTICLES) { idx ->
        //    val angle = 2 * PI * (idx / PARTICLES.toDouble())
        //    val startX = WIDTH/2 + 128 * cos(angle)
        //    val startY = HEIGHT/2 + 128 * sin(angle)
        //    Particle(vector2(startX, startY))
        //}

        val composite = compose {
            draw {
                drawer.background(ColorRGBa.BLACK)
                //val time = (frameCount % TOTAL_FRAMES) / TOTAL_FRAMES.toDouble()
                val time = frames / TOTAL_FRAMES.toDouble()

                particles.forEach {
                    it.update(0.1, time)
                    it.draw(drawer, time)
                }

                frames++
                if (frames >= TOTAL_FRAMES) {
                    frames = 0
                    particles.forEach { it.reset() }
                }
            }
            if (RECORDING) {
                post(GaussianBloom())
            //    post(FrameBlur())
            }
        }

        //extend(GUI()) {
        //    add(params)
        //}

        extend {
            if (RECORDING) {
                if (frameCount > TOTAL_FRAMES) {
                    videoWriter.stop()
                    application.exit()
                }
                drawer.isolatedWithTarget(videoTarget) {
                    composite.draw(this)
                }
                videoWriter.frame(videoTarget.colorBuffer(0))
            } else {
                composite.draw(drawer)
            }
        }
    }
}