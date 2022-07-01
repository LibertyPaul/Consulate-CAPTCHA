import matching.DigitRecognizerMatchShapes
import nu.pattern.OpenCV
import org.opencv.imgcodecs.Imgcodecs
import utils.DebugImageSaver
import utils.KnownDigitsProvider
import java.io.File

fun bootstrap() {
    DebugImageSaver.cleanUp()
    OpenCV.loadLocally()
}

fun main(args: Array<String>) {
    val workingSetDir = File(args[0] + "/captchas")
    require(workingSetDir.isDirectory) { "Working Set dir is not a dir [${workingSetDir.absolutePath}]" }

    bootstrap()

    val samples = KnownDigitsProvider.load(args[0] + "/known_digits")
    val recognizer = DigitRecognizerMatchShapes(samples)

    var correctGuesses = 0
    var attempts = 0

    workingSetDir.listFiles()?.forEach { file ->
        val label = file.nameWithoutExtension
        DebugImageSaver.reset(label)
        val captchaImage = Imgcodecs.imread(file.absolutePath)
        val digits = DigitExtractor.extract(captchaImage)
        println("Recognizing the digits...")
        val results = digits.map { digit -> recognizer.recognize(digit)  }
        results.forEachIndexed { index, recognitionOutput ->
            val best = recognitionOutput.score.minByOrNull { it.value }?.toPair()
            val correct = label[index] to recognitionOutput.score[label[index]]
            if (best == correct) {
                println("Correct: [$best]")
                correctGuesses += 1
            } else {
                println("Error: [$correct] but calculated as [$best]. Deviation: [${correct.second!! - best!!.second}].")
            }

            attempts += 1
        }
    }

    println("$correctGuesses correct out of $attempts attempts.")
}