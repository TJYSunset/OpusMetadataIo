package org.sunsetware.omio

import java.io.InputStream

internal data class OpusCommentHeader(
    val vendorString: String,
    val userComments: List<Pair<String, String>>,
)

internal fun InputStream.readOpusCommentHeader(sizeLimit: Int?): OpusCommentHeader {
    var sizeCounter = 0u

    fun readString(): String {
        val length = requireNotNull(readUInt())
        sizeCounter += length
        if (sizeLimit != null && sizeCounter > sizeLimit.toUInt()) {
            throw OpusException("Opus comment header size exceeding limit $sizeLimit")
        }
        val buffer = ByteArray(length.toInt())
        require(read(buffer) == buffer.size)
        return buffer.toString(Charsets.UTF_8)
    }

    try {
        // Magic Signature ("OpusTags")
        require(readUByte() == 0x4fu.toUByte())
        require(readUByte() == 0x70u.toUByte())
        require(readUByte() == 0x75u.toUByte())
        require(readUByte() == 0x73u.toUByte())
        require(readUByte() == 0x54u.toUByte())
        require(readUByte() == 0x61u.toUByte())
        require(readUByte() == 0x67u.toUByte())
        require(readUByte() == 0x73u.toUByte())

        val vendorString = readString()

        val userCommentListLength = requireNotNull(readUInt())
        val userComments =
            (0..<userCommentListLength.toInt()).map {
                val string = readString()
                val boundary = string.indexOf('=')
                string.substring(0, boundary) to string.substring(boundary + 1, string.length)
            }

        return OpusCommentHeader(vendorString, userComments)
    } catch (ex: Exception) {
        throw OpusException("Error reading opus comment header", ex)
    }
}
