package matching

import data.ExtractedDigit
import data.RecognitionOutput

sealed interface DigitRecognizerInterface {
    fun recognize(digit: ExtractedDigit): RecognitionOutput
}