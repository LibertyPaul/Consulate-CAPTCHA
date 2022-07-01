package utils

import data.Line
import org.opencv.core.Point
import kotlin.math.pow
import kotlin.math.sqrt

object PointExtensions {
    fun Point.distanceTo(other: Point) = sqrt((this.x - other.x).pow(2) + (this.y - other.y).pow(2))

    fun List<Point>.getAllLines(): List<Line> {
        val lines = mutableListOf<Line>()

        for (lhs in 0 until this.size - 1) {
            for (rhs in lhs + 1 until this.size) {
                val line = Line(this[lhs], this[rhs])
                lines.add(line)
            }
        }

        return lines
    }

    fun List<Point>.getCommonLineByDistance() = this.getAllLines().map { line ->
        line to this.sumOf { point -> line.distanceTo(point) }
    }.minByOrNull { it.second }?.first

    fun List<Point>.getCommonLineByAverage() = this.getAllLines().let { lines ->
        val vxAvg = lines.map { it.vx }.average()
        val vyAvg = lines.map { it.vy }.average()

        Line(
            vxAvg,
            vyAvg,
            lines.map { it.x }.average(),
            lines.map { it.y }.average(),
        )
    }

    fun List<Point>.getCommonLine() = this.getCommonLineByAverage()

}