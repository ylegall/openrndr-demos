package org.ygl.openrndr.demos.util

import org.openrndr.color.ColorRGBa
import org.openrndr.color.mix
import org.ygl.kxa.Interpolator

class ColorInterpolator: Interpolator<ColorRGBa> {
    override fun interpolate(from: ColorRGBa, to: ColorRGBa, progress: Double) = mix(from, to, progress)
}