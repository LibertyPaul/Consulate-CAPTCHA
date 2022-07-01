package matching

import data.ExtractedDigit
import data.RecognitionOutput
import org.opencv.imgproc.Imgproc

class DigitRecognizerMatchShapes(
    private val sampleDigits: Map<Char, ExtractedDigit>
) : DigitRecognizerInterface {
    override fun recognize(digit: ExtractedDigit): RecognitionOutput {
        val score = sampleDigits.map { sample ->
            val sampleScore = Imgproc.matchShapes(digit.contour, sample.value.contour, Imgproc.CONTOURS_MATCH_I3, 0.0)
            sample.key to sampleScore
        }.toMap()

        return RecognitionOutput(score)
    }
}