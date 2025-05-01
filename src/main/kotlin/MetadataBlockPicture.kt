@file:OptIn(ExperimentalEncodingApi::class)

package org.sunsetware.omio

import java.io.ByteArrayInputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Suppress("unused") const val METADATA_BLOCK_PICTURE_TYPE_FRONT_COVER: UInt = 3u
@Suppress("unused") const val METADATA_BLOCK_PICTURE_TYPE_BACK_COVER: UInt = 4u
@Suppress("unused") const val METADATA_BLOCK_PICTURE_MIME_TYPE_URI = "-->"

data class MetadataBlockPicture(
    val type: UInt,
    /** Converted to lowercase. */
    val mimeType: String,
    val description: String,
    val width: UInt,
    val height: UInt,
    val colorDepth: UInt,
    val paletteSize: UInt,
    val data: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MetadataBlockPicture

        if (type != other.type) return false
        if (mimeType != other.mimeType) return false
        if (description != other.description) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (colorDepth != other.colorDepth) return false
        if (paletteSize != other.paletteSize) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + colorDepth.hashCode()
        result = 31 * result + paletteSize.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

fun decodeMetadataBlockPicture(string: String): MetadataBlockPicture? {
    try {
        val stream = ByteArrayInputStream(Base64.decode(string))
        fun readString(): String {
            val length = requireNotNull(stream.readUIntBe())
            val buffer = ByteArray(length.toInt())
            require(stream.read(buffer) == buffer.size)
            return buffer.toString(Charsets.UTF_8)
        }

        val type = requireNotNull(stream.readUIntBe())
        val mimeType = readString().lowercase()
        val description = readString()
        val width = requireNotNull(stream.readUIntBe())
        val height = requireNotNull(stream.readUIntBe())
        val colorDepth = requireNotNull(stream.readUIntBe())
        val paletteSize = requireNotNull(stream.readUIntBe())
        val dataLength = requireNotNull(stream.readUIntBe())
        val data = ByteArray(dataLength.toInt())
        require(stream.read(data) == data.size)

        return MetadataBlockPicture(
            type,
            mimeType,
            description,
            width,
            height,
            colorDepth,
            paletteSize,
            data,
        )
    } catch (_: Exception) {
        return null
    }
}
