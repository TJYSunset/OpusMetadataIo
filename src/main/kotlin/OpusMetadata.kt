package org.sunsetware.omio

import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Suppress("unused") const val VORBIS_COMMENT_TITLE = "title"
@Suppress("unused") const val VORBIS_COMMENT_VERSION = "version"
@Suppress("unused") const val VORBIS_COMMENT_ALBUM = "album"
@Suppress("unused") const val VORBIS_COMMENT_TRACKNUMBER = "tracknumber"
@Suppress("unused") const val VORBIS_COMMENT_ARTIST = "artist"
@Suppress("unused") const val VORBIS_COMMENT_PERFORMER = "performer"
@Suppress("unused") const val VORBIS_COMMENT_COPYRIGHT = "copyright"
@Suppress("unused") const val VORBIS_COMMENT_LICENSE = "license"
@Suppress("unused") const val VORBIS_COMMENT_ORGANIZATION = "organization"
@Suppress("unused") const val VORBIS_COMMENT_DESCRIPTION = "description"
@Suppress("unused") const val VORBIS_COMMENT_GENRE = "genre"
@Suppress("unused") const val VORBIS_COMMENT_DATE = "date"
@Suppress("unused") const val VORBIS_COMMENT_LOCATION = "location"
@Suppress("unused") const val VORBIS_COMMENT_CONTACT = "contact"
@Suppress("unused") const val VORBIS_COMMENT_ISRC = "isrc"

/** https://wiki.xiph.org/VorbisComment#METADATA_BLOCK_PICTURE */
@Suppress("unused") const val VORBIS_COMMENT_METADATA_BLOCK_PICTURE = "metadata_block_picture"

// Unofficial tags gathered from the following sources:
// https://www.jthink.net/jaudiotagger/tagmapping.html
// https://docs.mp3tag.de/mapping-table/
@Suppress("unused") const val VORBIS_COMMENT_UNOFFICIAL_ALBUMARTIST = "albumartist"
@Suppress("unused") const val VORBIS_COMMENT_UNOFFICIAL_DISCNUMBER = "discnumber"
@Suppress("unused")
val VORBIS_COMMENT_UNOFFICIAL_COMMENT = setOf(VORBIS_COMMENT_DESCRIPTION, "comment")
@Suppress("unused")
val VORBIS_COMMENT_UNOFFICIAL_LYRICS: Set<String> = setOf("lyrics", "unsyncedlyrics")

data class OpusMetadata(
    val vendorString: String,
    /**
     * Keys are converted to lowercase without advanced casefolding. As stated in
     * [vorbis comment specs](https://www.xiph.org/vorbis/doc/v-comment.html), keys should be
     * ASCII-only and case-insensitive.
     */
    val userComments: Map<String, List<String>>,
    val duration: Duration?,
)

/**
 * Duration reading is opt-in because it requires reading the whole file.
 *
 * @throws OpusException
 */
fun readOpusMetadata(
    stream: InputStream,
    readDuration: Boolean,
    headerSizeLimit: Int = 120 * 1024 * 1024,
): OpusMetadata {
    try {
        val oggReader = OggReader(stream)
        // Find first opus packet
        var identificationHeader = null as OpusIdentificationHeader?
        var serialNumber = null as UInt?
        while (serialNumber == null) {
            oggReader.readPacket { packet ->
                identificationHeader =
                    try {
                        packet.readOpusIdentificationHeader()
                    } catch (_: Exception) {
                        null
                    }
                if (identificationHeader != null) {
                    serialNumber = packet.currentPageHeader.bitstreamSerialNumber
                }
                false
            }
        }

        var commentHeader = null as OpusCommentHeader?
        while (commentHeader == null) {
            oggReader.readPacket { packet ->
                if (packet.currentPageHeader.bitstreamSerialNumber == serialNumber) {
                    commentHeader = packet.readOpusCommentHeader(headerSizeLimit)
                }
            }
        }
        val userComments = mutableMapOf<String, MutableList<String>>()
        for ((key, value) in commentHeader.userComments) {
            userComments.getOrPut(key.lowercase()) { mutableListOf() } += value
        }

        var firstPageSampleCount = 0
        var firstGranulePosition = null as ULong?
        var lastGranulePosition = null as ULong?
        var endOfStream = false
        if (readDuration) {
            try {
                while (!endOfStream) {
                    oggReader.readPacket { packet ->
                        if (packet.currentPageHeader.bitstreamSerialNumber == serialNumber) {
                            if (packet.currentPageHeader.granulePosition != ULong.MAX_VALUE) {
                                if (firstGranulePosition == null) {
                                    firstGranulePosition = packet.currentPageHeader.granulePosition
                                } else {
                                    lastGranulePosition = packet.currentPageHeader.granulePosition
                                }

                                if (
                                    packet.currentPageHeader.granulePosition == firstGranulePosition
                                ) {
                                    // https://github.com/xiph/opusfile/blob/d2535e62809079c81f9e5139c0daebe43af97be4/src/opusfile.c#L740
                                    val toc = packet.read()
                                    val secondByte = packet.read().takeIf { it >= 0 }
                                    firstPageSampleCount +=
                                        opusPacketGetNbFrames(toc, secondByte) *
                                            opusPacketGetSamplesPerFrame(toc)
                                }
                            }
                            endOfStream = packet.currentPageHeader.endOfStream
                        }
                    }
                }
            } catch (_: OggException) {
                // Early EOF reached
            }
        }

        return OpusMetadata(
            commentHeader.vendorString,
            userComments,
            if (readDuration) {
                (lastGranulePosition ?: firstGranulePosition)
                    ?.let {
                        (it - firstGranulePosition!! + firstPageSampleCount.toUInt() -
                            identificationHeader!!.preSkip) / 48u
                    }
                    ?.coerceAtLeast(0u)
                    ?.toLong()
                    ?.milliseconds ?: Duration.ZERO
            } else {
                null
            },
        )
    } catch (ex: Exception) {
        throw OpusException("Error reading opus metadata", ex)
    }
}

/**
 * https://github.com/xiph/opus/blob/08bcc6e46227fca01aa3de3f3512f8b692d8d36b/src/opus_decoder.c#L1173
 */
private fun opusPacketGetNbFrames(toc: Int, secondByte: Int?): Int {
    val count = toc and 0x3
    return when {
        count == 0 -> 1
        count != 3 -> 2
        secondByte == null -> throw OpusException("Invalid opus packet")
        else -> secondByte and 0x3F
    }
}

/** https://github.com/xiph/opus/blob/08bcc6e46227fca01aa3de3f3512f8b692d8d36b/src/opus.c#L203 */
private fun opusPacketGetSamplesPerFrame(toc: Int, fs: Int = 48000): Int {
    return when {
        toc and 0x80 != 0 -> {
            val a = ((toc shr 3) and 0x3)
            (fs shl a) / 400
        }
        toc and 0x60 == 0x60 -> {
            if (toc and 0x08 != 0) fs / 50 else fs / 100
        }
        else -> {
            val a = ((toc shr 3) and 0x3)
            if (a == 3) fs * 60 / 1000 else (fs shl a) / 100
        }
    }
}
