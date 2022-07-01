package utils

import org.opencv.core.Size
import kotlin.math.max

object MiscExtensions {
    fun Size.with(other: Size) = Size(max(this.width, other.width), max(this.height, other.height))
    fun Int.nearbyRange(deviation: Int, steps: Int) = this-deviation..this+deviation step max((deviation * 2 + 1) / steps, 1)
}