package data

import org.opencv.core.Point


data class Transformation(
    val from: Triple<Point, Point, Point>,
    val to: Triple<Point, Point, Point>,
) {
    fun toString1() = "{[${from.first}][${from.second}][${from.third}]}-{[${to.first}][${to.second}][${to.third}]}"
    override fun toString() = toString1()
}

