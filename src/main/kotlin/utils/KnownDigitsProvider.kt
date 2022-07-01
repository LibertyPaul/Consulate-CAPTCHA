package utils

import data.ExtractedDigit
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import utils.HierarchyHelper.filterChildContours
import utils.MatExtensions.toBnw

object KnownDigitsProvider {
    fun load(path: String) = ('0'..'9').associateWith { digit ->
        val digitImage = Imgcodecs.imread("$path/$digit.png").toBnw()

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(digitImage, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)

        val topContours = contours.indices.toList().filterChildContours(hierarchy)
        require(topContours.size == 1)

        ExtractedDigit(digitImage, contours[topContours.first()])
    }
}