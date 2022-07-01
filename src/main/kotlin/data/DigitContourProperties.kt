package data

import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc


data class DigitContourProperties(
    val contourIdx: Int,
    val color: Scalar,

    val topMost: Point,
    val downMost: Point,
    val leftMost: Point,
    val rightMost: Point,

    val width: Double,
    val height: Double,
) {
    fun boundingRect(contours: List<MatOfPoint>) = Imgproc.boundingRect(contours[this.contourIdx])
    fun boundingSize(contours: List<MatOfPoint>) = boundingRect(contours).size()
}

