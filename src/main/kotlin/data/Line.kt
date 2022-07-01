package data

import org.opencv.core.Point
import utils.PointExtensions.distanceTo
import kotlin.math.abs


data class Line(
    val vx: Double,
    val vy: Double,
    val x: Double,
    val y: Double,
) {
    constructor(lhs: Point, rhs: Point): this(
        1.0,
        (rhs.y - lhs.y) / (rhs.x - lhs.x),
        lhs.x,
        lhs.y,
    )

    fun getPointsProjected(cols: Int): Pair<Point, Point> {
        val lefty = (-x * vy / vx) + y
        val righty = ((cols - x) * vy / vx) + y
        val left = Point(0.0, lefty)
        val right = Point(cols - 1.0, righty)

        return Pair(left, right)
    }

    fun distanceTo(point: Point): Double {
        val point1 = Point(x, y)
        val point2 = Point(x + vx, y + vy)

        val distance = (
            abs((point2.x - point1.x) * (point1.y - point.y) - (point1.x - point.x) * (point2.y - point1.y))
            /
            point1.distanceTo(point2)
        )

        return distance
    }
}

