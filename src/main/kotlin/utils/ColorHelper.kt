package utils

import org.opencv.core.Scalar
import kotlin.random.Random

object ColorHelper {
    val red = Scalar(200.0, 0.0, 0.0)
    val black = Scalar(0.0, 0.0, 0.0)
    val white = Scalar(255.0, 255.0, 255.0)
    val green = Scalar(0.0, 200.0, 0.0)
    val yellow = Scalar(0.0, 200.0, 200.0)

    private val rng = Random(0)
    val random get() = Scalar(
        rng.nextDouble(128.0, 255.0),
        rng.nextDouble(128.0, 255.0),
        rng.nextDouble(128.0, 255.0),
    )
}

