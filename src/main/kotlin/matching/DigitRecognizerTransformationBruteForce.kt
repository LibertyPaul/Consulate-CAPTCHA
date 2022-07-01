package matching

import data.ExtractedDigit
import data.RecognitionOutput
import data.Transformation
import org.opencv.core.Mat
import org.opencv.core.Point
import utils.MatExtensions.compare
import utils.MatExtensions.transform
import utils.MiscExtensions.nearbyRange
import utils.TransformationHelper
import kotlin.math.roundToInt

class DigitRecognizerTransformationBruteForce(
    private val sampleDigits: Map<Char, ExtractedDigit>
) : DigitRecognizerInterface {
    private fun Mat.compareTransformed(
        other: Mat,
        from: Triple<Point, Point, Point>,
        seed: Triple<Point, Point, Point>,
        offsetDeviationFactor: Int = 5,
        cornerDeviationFactor: Int = 2,
        steps: Int = 10,
    ): Double {
        var closestMatchScore: Double? = null
        var closestMatchTransformation: Transformation? = null

        val offsetDeviationX = this.width() / offsetDeviationFactor
        val offsetDeviationY = this.height() / offsetDeviationFactor

        val cornerDeviationX = this.width() / cornerDeviationFactor
        val cornerDeviationY = this.height() / cornerDeviationFactor

        val offsetXrange = seed.first.x.roundToInt().nearbyRange(offsetDeviationX, steps)
        val offsetYrange = seed.first.y.roundToInt().nearbyRange(offsetDeviationY, steps)
        val ldXrange = seed.second.x.roundToInt().nearbyRange(cornerDeviationX, steps)
        val ldYrange = seed.second.y.roundToInt().nearbyRange(cornerDeviationY, steps)
        val trXrange = seed.third.x.roundToInt().nearbyRange(cornerDeviationX, steps)
        val trYrange = seed.third.y.roundToInt().nearbyRange(cornerDeviationY, steps)

        var iteration = 0
        val iterations =
            offsetXrange.count() * offsetYrange.count() * ldXrange.count() * ldYrange.count() * trXrange.count() * trYrange.count()

        println()

        for (offsetX in offsetXrange) {
            for (offsetY in offsetYrange) {
                for (ldX in ldXrange) {
                    for (ldY in ldYrange) {
                        for (trX in trXrange) {
                            for (trY in trYrange) {
                                val to = Triple(
                                    Point(offsetX.toDouble(), offsetY.toDouble()),
                                    Point(ldX.toDouble(), ldY.toDouble()),
                                    Point(trX.toDouble(), trY.toDouble()),
                                )

                                val transformation = Transformation(from, to)

                                val otherTransformed = other.transform(transformation)

                                val score = this.compare(otherTransformed)
                                if (closestMatchScore == null || closestMatchScore > score) {
                                    closestMatchScore = score
                                    closestMatchTransformation = transformation
                                }

                                iteration += 1
                                if (iteration % 100 == 0) {
                                    println("\r[$iteration / $iterations]       ")
                                }
                            }
                        }
                    }
                }
            }
        }

        println()
        println("[$iteration] Winning transformation: $closestMatchTransformation")
        return closestMatchScore!!
    }

    override fun recognize(digit: ExtractedDigit) = sampleDigits.map { sample ->
        val randomDraft = TransformationHelper.randomTransformations(digit.bnwImage.size(), 5)

        sample.key to randomDraft.mapIndexed { index, seed ->
            val res = sample.value.bnwImage.compareTransformed(digit.bnwImage, seed.from, seed.to, 10, 5, 5)
            println("Seed $index [$seed]: $res")

            index to res
        }.minByOrNull { it.second }!!.second
    }.toMap().let { RecognitionOutput(it) }
}