package com.example.animepahedl

/** One RFC byte range; multipart ranges are explicitly rejected. */
internal fun parseByteRange(header: String?, length: Long): LongRange? {
    if (header == null) return null
    require(length > 0)
    val match = Regex("bytes=(\\d*)-(\\d*)").matchEntire(header) ?: throw IllegalArgumentException("Invalid range")
    val first = match.groupValues[1]
    val last = match.groupValues[2]
    if (first.isEmpty()) {
        val suffix = last.toLongOrNull() ?: throw IllegalArgumentException("Invalid suffix")
        require(suffix > 0)
        return maxOf(0, length - suffix)..(length - 1)
    }
    val start = first.toLongOrNull() ?: throw IllegalArgumentException("Invalid start")
    val end = if (last.isEmpty()) length - 1 else last.toLongOrNull() ?: throw IllegalArgumentException("Invalid end")
    require(start < length && end >= start)
    return start..minOf(end, length - 1)
}
