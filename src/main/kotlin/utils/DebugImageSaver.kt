package utils

import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

object DebugImageSaver {
    private const val debugDirPath = "debug"
    private var idx: Int? = null
    private var label: String? = null

    fun reset(label: String) {
        idx = 0
        this.label = label
    }

    fun save(image: Mat, name: String, subDir: String = "") {
        File("$debugDirPath/$subDir").mkdirs()
        Imgcodecs.imwrite("$debugDirPath/$subDir/${label!!}.${idx!!}.$name.png", image)
        idx = idx!! + 1
    }

    fun Mat.alsoSave(name: String, subDir: String = "") = this.also { save(it, name, subDir) }

    fun cleanUp() {
        File(debugDirPath).listFiles()?.forEach { it.delete() }
    }

}

