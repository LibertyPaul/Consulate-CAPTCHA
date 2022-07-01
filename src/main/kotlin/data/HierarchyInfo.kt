package data

data class HierarchyInfo(
    val next: Int,
    val previous: Int,
    val firstChild: Int,
    val parent: Int,
) {
    constructor(raw: List<Double>): this(
        raw[0].toInt(),
        raw[1].toInt(),
        raw[2].toInt(),
        raw[3].toInt(),
    )
}

