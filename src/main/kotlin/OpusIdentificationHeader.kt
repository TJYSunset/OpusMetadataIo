package org.sunsetware.omio

import java.io.InputStream

internal data class OpusIdentificationHeader(
    val version: UByte,
    val outputChannelCount: UByte,
    val preSkip: UShort,
    val inputSampleRate: UInt,
    val outputGain: UShort,
    val channelMappingFamily: UByte,
    // channel mapping table is currently ignored
)

internal fun InputStream.readOpusIdentificationHeader(): OpusIdentificationHeader {
    // Magic Signature ("OpusHead")
    require(readUByte() == 0x4fu.toUByte())
    require(readUByte() == 0x70u.toUByte())
    require(readUByte() == 0x75u.toUByte())
    require(readUByte() == 0x73u.toUByte())
    require(readUByte() == 0x48u.toUByte())
    require(readUByte() == 0x65u.toUByte())
    require(readUByte() == 0x61u.toUByte())
    require(readUByte() == 0x64u.toUByte())

    val version = requireNotNull(readUByte())
    require(version < 16u)

    val outputChannelCount = requireNotNull(readUByte())
    val preSkip = requireNotNull(readUShort())
    val inputSampleRate = requireNotNull(readUIntBe())
    val outputGain = requireNotNull(readUShort())
    val channelMappingFamily = requireNotNull(readUByte())

    return OpusIdentificationHeader(
        version,
        outputChannelCount,
        preSkip,
        inputSampleRate,
        outputGain,
        channelMappingFamily,
    )
}
