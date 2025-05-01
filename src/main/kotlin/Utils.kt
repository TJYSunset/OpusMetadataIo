package org.sunsetware.omio

import java.io.InputStream

internal fun InputStream.readUByte(): UByte? {
    return read().takeIf { it >= 0 }?.toUByte()
}

internal fun InputStream.readUShort(): UShort? {
    var value = read().apply { if (this < 0) return null }.toUShort()
    value = read().apply { if (this < 0) return null }.times(256).toUShort().plus(value).toUShort()
    return value
}

internal fun InputStream.readUInt(): UInt? {
    var value = read().apply { if (this < 0) return null }.toUInt()
    value += read().apply { if (this < 0) return null }.toUInt() * 256u
    value += read().apply { if (this < 0) return null }.toUInt() * 256u * 256u
    value += read().apply { if (this < 0) return null }.toUInt() * 256u * 256u * 256u
    return value
}

internal fun InputStream.readULong(): ULong? {
    var value = read().apply { if (this < 0) return null }.toULong()
    value += read().apply { if (this < 0) return null }.toULong() * 0x0000000000000100uL
    value += read().apply { if (this < 0) return null }.toULong() * 0x0000000000010000uL
    value += read().apply { if (this < 0) return null }.toULong() * 0x0000000001000000uL
    value += read().apply { if (this < 0) return null }.toULong() * 0x0000000100000000uL
    value += read().apply { if (this < 0) return null }.toULong() * 0x0000010000000000uL
    value += read().apply { if (this < 0) return null }.toULong() * 0x0001000000000000uL
    value += read().apply { if (this < 0) return null }.toULong() * 0x0100000000000000uL
    return value
}

internal fun InputStream.readUIntBe(): UInt? {
    var value = read().apply { if (this < 0) return null }.toUInt() * 256u * 256u * 256u
    value += read().apply { if (this < 0) return null }.toUInt() * 256u * 256u
    value += read().apply { if (this < 0) return null }.toUInt() * 256u
    value += read().apply { if (this < 0) return null }.toUInt()
    return value
}

internal fun UByte.hasFlag(flag: UByte): Boolean {
    return and(flag) == flag
}
