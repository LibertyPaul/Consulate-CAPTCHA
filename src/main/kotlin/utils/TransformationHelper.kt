package utils

import data.Transformation
import org.opencv.core.Point
import org.opencv.core.Size
import utils.MiscExtensions.nearbyRange
import kotlin.math.pow
import kotlin.math.roundToInt

object TransformationHelper {
    fun randomTransformations(sampleSize: Size, count: Int): List<Transformation> {
        val from = Triple(
            Point(0.0, 0.0),
            Point(0.0, sampleSize.height),
            Point(sampleSize.width, 0.0)
        )

        val steps = count.toDouble().pow(-6.0).roundToInt() + 1

        val randomDraft = (0 until count).map {
            val to = Triple(
                Point(
                    from.first.x.roundToInt().nearbyRange(sampleSize.width.toInt() / 5, steps).shuffled().first().toDouble(),
                    from.first.y.roundToInt().nearbyRange(sampleSize.height.toInt() / 5, steps).shuffled().first().toDouble(),
                ),
                Point(
                    from.second.x.roundToInt().nearbyRange(sampleSize.width.toInt() / 2, steps).shuffled().first().toDouble(),
                    from.second.y.roundToInt().nearbyRange(sampleSize.height.toInt() / 2, steps).shuffled().first().toDouble(),
                ),
                Point(
                    from.second.x.roundToInt().nearbyRange(sampleSize.width.toInt() / 2, steps).shuffled().first().toDouble(),
                    from.second.y.roundToInt().nearbyRange(sampleSize.height.toInt() / 2, steps).shuffled().first().toDouble(),
                ),
            )

            Transformation(from, to)
        }

        return randomDraft.take(count)
    }
}