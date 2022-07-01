package data

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint

data class ExtractedDigit(
    val bnwImage: Mat,
    val contour: MatOfPoint,
)
