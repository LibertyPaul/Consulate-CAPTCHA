package utils

import data.HierarchyInfo
import org.opencv.core.Mat

object HierarchyHelper {
    fun List<Int>.filterChildContours(hierarchy: Mat) = filter { contourIdx ->
        HierarchyInfo(hierarchy.get(0, contourIdx).toList()).parent == -1
    }
}