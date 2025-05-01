package org.sunsetware.omio

import java.io.InputStream

internal data class OggPageHeader(
    val continuedPacket: Boolean,
    val beginOfStream: Boolean,
    val endOfStream: Boolean,
    val granulePosition: ULong,
    val bitstreamSerialNumber: UInt,
    val pageSequenceNumber: UInt,
    val crcChecksum: UInt,
    val pageSegments: UByte,
    val segmentTable: List<UByte>,
)

internal fun InputStream.readOggPageHeader(): OggPageHeader {
    try {
        // capture_pattern ("OggS")
        require(readUByte() == 0x4fu.toUByte())
        require(readUByte() == 0x67u.toUByte())
        require(readUByte() == 0x67u.toUByte())
        require(readUByte() == 0x53u.toUByte())

        // stream_structure_version
        require(readUByte() == 0x00u.toUByte())

        val headerTypeFlag = requireNotNull(readUByte())
        val continuedPacket = headerTypeFlag.hasFlag(0x01u)
        val beginOfStream = headerTypeFlag.hasFlag(0x02u)
        val endOfStream = headerTypeFlag.hasFlag(0x04u)

        val granulePosition = requireNotNull(readULong())
        val bitstreamSerialNumber = requireNotNull(readUInt())
        val pageSequenceNumber = requireNotNull(readUInt())
        val crcChecksum = requireNotNull(readUInt())

        val pageSegments = requireNotNull(readUByte())
        val segmentTable = (0..<pageSegments.toInt()).map { requireNotNull(readUByte()) }

        return OggPageHeader(
            continuedPacket,
            beginOfStream,
            endOfStream,
            granulePosition,
            bitstreamSerialNumber,
            pageSequenceNumber,
            crcChecksum,
            pageSegments,
            segmentTable,
        )
    } catch (ex: Exception) {
        throw OggException("Malformed ogg page or EOF reached", ex)
    }
}
