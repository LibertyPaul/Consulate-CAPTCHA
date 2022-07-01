import data.DigitContourProperties
import data.ExtractedDigit
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import utils.ColorHelper
import utils.DebugImageSaver
import utils.DebugImageSaver.alsoSave
import utils.HierarchyHelper.filterChildContours
import utils.MatExtensions.blur
import utils.MatExtensions.contourArea
import utils.MatExtensions.downmostCoordinate
import utils.MatExtensions.invert
import utils.MatExtensions.leftmostCoordinate
import utils.MatExtensions.maxHeight
import utils.MatExtensions.maxWidth
import utils.MatExtensions.rightmostCoordinate
import utils.MatExtensions.scale
import utils.MatExtensions.toBnw
import utils.MatExtensions.topmostCoordinate

object DigitExtractor {
    object Params {
        object Upscale {
            const val rate = 10.0
            const val interpolation = 3
        }

        object Bnw {
            const val threshold = 215
        }

        object Blur {
            const val passes = 3
            const val blurRadius = 15.0
        }

        object FilterNoise {
            const val dropFactor = 5.0
        }
    }

    private fun prepareImage(srcImage: Mat): Mat {
        val upscale = srcImage.scale(Params.Upscale.rate, Params.Upscale.interpolation).alsoSave("upscale")
        val blurred = upscale.blur(Params.Blur.passes, Params.Blur.blurRadius).alsoSave("blurred")
        val bnw = blurred.toBnw(Params.Bnw.threshold).alsoSave("bnw")

        return bnw
    }

    private fun List<Int>.filterNoiseContours(contours: List<MatOfPoint>): List<Int> {
        val contourIdsDesc = this.map { it to contours[it].contourArea() }.sortedByDescending { it.second }
        for (i in contourIdsDesc.indices) {
            val pastMedianArea = contourIdsDesc[i / 2].second
            if (pastMedianArea > contourIdsDesc[i].second * Params.FilterNoise.dropFactor) {
                return contourIdsDesc.map { it.first }.take(i)
            }
        }

        return this
    }

    private fun findDigits(preparedImage: Mat): List<ExtractedDigit> {
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(preparedImage.invert(), contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE)
        val topContourIds = contours.indices.toList().filterChildContours(hierarchy)

        Mat.zeros(preparedImage.size(), CvType.CV_32FC3).let { image ->
            topContourIds.forEach {
                Imgproc.drawContours(image, contours, it, ColorHelper.random, Imgproc.FILLED, Imgproc.LINE_8, hierarchy, 0)
            }

            DebugImageSaver.save(image, "all-top-contours")
            println("Find TL Contours: Done.")
        }


        val digitContourIds = topContourIds.filterNoiseContours(contours).sortedBy { contours[it].leftmostCoordinate()!!.x }

        val digitContourProperties = digitContourIds.map { contourIdx ->
            val contour = contours[contourIdx]

            val width = contour.maxWidth()
            val height = contour.maxHeight()

            DigitContourProperties(
                contourIdx,
                ColorHelper.random,
                contour.topmostCoordinate()!!,
                contour.downmostCoordinate()!!,
                contour.leftmostCoordinate()!!,
                contour.rightmostCoordinate()!!,
                width,
                height,
            )
        }

        Mat.zeros(preparedImage.size(), CvType.CV_32FC3).let { image ->
            digitContourProperties.forEach { contour ->
                Imgproc.drawContours(image, contours, contour.contourIdx, contour.color, Imgproc.FILLED, Imgproc.LINE_8, hierarchy, 1)
            }
            DebugImageSaver.save(image, "digit-contours")
        }

        val extractedDigits = digitContourProperties.map { contour ->
            val contourImage = Mat.zeros(contour.boundingSize(contours), CvType.CV_8UC1).also {
                val offset = Point(
                    -contours[contour.contourIdx].leftmostCoordinate()!!.x,
                    -contours[contour.contourIdx].topmostCoordinate()!!.y
                )

                Imgproc.drawContours(it, contours, contour.contourIdx, ColorHelper.white, Imgproc.FILLED, Imgproc.LINE_8, hierarchy, 1, offset)
            }

            ExtractedDigit(contourImage, contours[contour.contourIdx])
        }

        extractedDigits.forEachIndexed { index, digit ->
            DebugImageSaver.save(digit.bnwImage, "digit-$index")
        }

        return extractedDigits
    }

    fun extract(image: Mat): List<ExtractedDigit> {
        val preparedImage = prepareImage(image)
        return findDigits(preparedImage)
    }


    /*
    fun experimentalAlignment(contourImage: Mat, digitContourProperties: List<DigitContourProperties>) {
        val distanceTransform = Mat().also {
            val contourImageBnw = contourImage.toBnw()//.alsoSave("bnw2")
            Imgproc.distanceTransform(contourImageBnw, it, Imgproc.CV_DIST_L2, 3)
        }//.alsoSave("distance")


        val topmostPoints = digitContourProperties.map { it.topMost }
        val topmostLine = topmostPoints.getCommonLine()
        val topmostLineProjected = topmostLine.getPointsProjected(contourImage.cols())
        Imgproc.line(contourImage, topmostLineProjected.first, topmostLineProjected.second, ColorHelper.random, 3, Imgproc.LINE_AA)

        val downmostPoints = digitContourProperties.map { it.downMost }
        val downmostLine = downmostPoints.getCommonLine()
        val downmostLineProjected = downmostLine.getPointsProjected(contourImage.cols())
        Imgproc.line(contourImage, downmostLineProjected.first, downmostLineProjected.second, ColorHelper.random, 3, Imgproc.LINE_AA)


        DebugImageSaver.save(contourImage, "most_lines")


        val leftMostContour = digitContourProperties.minByOrNull { it.leftMost.x }!!
        val rightMostContour = digitContourProperties.maxByOrNull { it.rightMost.x }!!

        val topLeft = leftMostContour.topMost.let { Point(it.x - topmostLine.vx * leftMostContour.width, it.y - topmostLine.vy * leftMostContour.width) }
        val topRight = rightMostContour.topMost.let { Point(it.x + topmostLine.vx * rightMostContour.width, it.y + topmostLine.vy * rightMostContour.width) }
        val downLeft = leftMostContour.downMost.let { Point(it.x - downmostLine.vx * leftMostContour.width, it.y - downmostLine.vy * leftMostContour.width) }
        val downRight = rightMostContour.downMost.let { Point(it.x + downmostLine.vx * rightMostContour.width, it.y + downmostLine.vy * rightMostContour.width) }

        listOf(topLeft, topRight, downLeft, downRight).forEach {
            Imgproc.drawMarker(contourImage, it, ColorHelper.yellow, Imgproc.MARKER_DIAMOND, 20, 5)
        }

        DebugImageSaver.save(contourImage, "perspective_points")

        println("Draw digit contours done.")
    }

     */
}