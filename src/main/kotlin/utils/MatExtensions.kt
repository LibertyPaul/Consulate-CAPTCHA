package utils

import data.Transformation
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import utils.MiscExtensions.nearbyRange
import utils.MiscExtensions.with
import utils.PointExtensions.distanceTo
import kotlin.math.roundToInt

object MatExtensions {
    fun Mat.toBits(): List<List<Boolean>> {
        return (0 until rows()).map { row ->
            (0 until cols()).map { col ->
                val vector = this.get(row, col)
                require(vector.size == 1) { "Invalid vector size at [$row:$col]: [${vector.size}]."}

                val value = vector[0]
                require(value == 0.0 || value == 255.0) { "Invalid binary value: [$value]." }

                value == 255.0
            }
        }
    }

    fun Mat.getPrintable() = this.toBits().joinToString("\n") { row -> row.joinToString("") { value -> if (value) "+" else " " } }

    fun Mat.scale(factor: Double, interpolation: Int) = Mat().also {
        Imgproc.resize(this, it, Size(), factor, factor, interpolation)
    }

    fun Mat.blur(passes: Int, blurRadius: Double): Mat {
        var current = this

        for (pass in 0 until passes) {
            current = Mat().also {
                Imgproc.blur(current, it, Size(blurRadius, blurRadius))
            }
        }

        return current
    }

    fun Mat.toBnw(threshold: Int = 1) = Mat().also { bnw ->
        val prepared = if (this.type() != CvType.CV_8UC1) {
            val greyscale = Mat().also { greyscale ->
                try {
                    Imgproc.cvtColor(this, greyscale, Imgproc.COLOR_BGR2GRAY, CvType.CV_8UC1)
                } catch (ex: CvException) {
                    throw ex
                }
            }

            val downgrade = Mat().also {
                greyscale.convertTo(it, CvType.CV_8UC1)
            }

            downgrade
        } else {
            this
        }

        Imgproc.threshold(prepared, bnw, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)
    }

    fun Mat.invert() = Mat().also { Core.bitwise_not(this, it) }

    fun Mat.erode(erosionSize: Double) = Mat().also {
        val kernel = Imgproc.getStructuringElement(
            Imgproc.MORPH_ELLIPSE,
            Size(2 * erosionSize + 1, 2 * erosionSize + 1),
            Point(erosionSize, erosionSize)
        )

        Imgproc.erode(this.invert(), it, kernel)
    }.invert()

    fun MatOfPoint2f.minAreaRectBox(): MatOfPoint {
        val rect = Imgproc.minAreaRect(this)
        val box = Array<Point?>(4) { null }
        rect.points(box)

        return MatOfPoint(box[0], box[1], box[2], box[3])
    }

    fun MatOfPoint.maxWidth()  = this.toMatOfPoint2F().minAreaRectBox().toList().let { it[0].distanceTo(it[1]) }
    fun MatOfPoint.maxHeight() = this.toMatOfPoint2F().minAreaRectBox().toList().let { it[2].distanceTo(it[1]) }

    fun Mat.extendWithZeros(toSize: Size): Mat = Mat.zeros(this.size().with(toSize), this.type()).also {
        for (row in 0 until rows()) {
            for (col in 0 until cols()) {
                val value = this.get(row, col)[0]
                it.put(row, col, value)
            }
        }
    }

    private fun Mat.binaryOperation(other: Mat, operation: (lhs: Boolean, rhs: Boolean) -> Boolean): Mat {
        require(this.type() == CvType.CV_8UC1)
        require(other.type() == CvType.CV_8UC1)
        require(this.size() == other.size())

        return Mat.zeros(this.size(), CvType.CV_8UC1).also {
            for (row in 0 until rows()) {
                for (col in 0 until cols()) {
                    val lhs = this.get(row, col)[0]
                    require(lhs == 0.0 || lhs == 255.0) { "Invalid value [$lhs]." }

                    val rhs = other.get(row, col)[0]
                    require(rhs == 0.0 || rhs == 255.0) { "Invalid value [$rhs]." }

                    val res = if (operation(lhs == 255.0, rhs == 255.0)) 255.0 else 0.0
                    it.put(row, col, res)
                }
            }
        }
    }

    fun Mat.binaryXor(other: Mat) = this.binaryOperation(other) { lhs, rhs -> lhs != rhs }
    fun Mat.binaryAnd(other: Mat) = this.binaryOperation(other) { lhs, rhs -> lhs && rhs }

    fun Mat.binarySum() = this.toBits().fold(0) { acc, rows -> acc + rows.fold(0) { acc2, bit -> acc2 + if (bit) 1 else 0 } }

    fun Mat.merge(other: Mat) = Mat.zeros(this.size(), this.type()).also {
        require(this.size() == other.size())
        require(this.type() == other.type())
        Core.add(this, other, it)
    }

    fun Mat.subtract(subtrahend: Mat) = Mat.zeros(this.size(), CvType.CV_8U).also {
        require(this.size() == subtrahend.size())
        Core.subtract(this, subtrahend, it)
    }

    fun MatOfPoint.contourArea() = Imgproc.contourArea(this)
    fun MatOfPoint.toMatOfPoint2F() = MatOfPoint2f().also { this.convertTo(it, CvType.CV_32F) }

    fun MatOfPoint.topmostCoordinate() = toList().minByOrNull { it.y }
    fun MatOfPoint.downmostCoordinate() = toList().maxByOrNull { it.y }
    fun MatOfPoint.rightmostCoordinate() = toList().maxByOrNull { it.x }
    fun MatOfPoint.leftmostCoordinate() = toList().minByOrNull { it.x }

    fun Mat.compare(other: Mat): Double {
        val lhs = this.extendWithZeros(other.size())
        val rhs = other.extendWithZeros(this.size())

        val diff = lhs.binaryXor(rhs)
        val same = lhs.binaryAnd(rhs)

        val diffPixels = diff.binarySum()
        val samePixels = same.binarySum()

        return diffPixels.toDouble() / samePixels.toDouble()
    }

    fun Mat.transform(transformation: Transformation): Mat {
        val matFrom = MatOfPoint2f(transformation.from.first, transformation.from.second, transformation.from.third)
        val matTo = MatOfPoint2f(transformation.to.first, transformation.to.second, transformation.to.third)
        val matTransform = Imgproc.getAffineTransform(matFrom, matTo)
        val dstSize = this.size()
        val transformed = Mat.zeros(dstSize, this.type()).also {
            Imgproc.warpAffine(this, it, matTransform, dstSize, Imgproc.INTER_NEAREST)
        }

        return transformed
    }

}