import java.io.ByteArrayInputStream
import kotlin.test.Test
import org.assertj.core.api.Assertions.*
import org.sunsetware.omio.hasFlag
import org.sunsetware.omio.readUByte
import org.sunsetware.omio.readUInt
import org.sunsetware.omio.readUIntBe
import org.sunsetware.omio.readULong
import org.sunsetware.omio.readUShort

@OptIn(ExperimentalUnsignedTypes::class)
class UtilsTest {
    @Test
    fun testReadUByte() {
        val stream = ByteArrayInputStream(ubyteArrayOf(0xffu, 0xaau, 0x00u, 0xffu).toByteArray())
        assertThat(stream.readUByte()).isEqualTo(0xffu.toUByte())
        assertThat(stream.readUByte()).isEqualTo(0xaau.toUByte())
        assertThat(stream.readUByte()).isEqualTo(0x00u.toUByte())
        assertThat(stream.readUByte()).isEqualTo(0xffu.toUByte())
        assertThat(stream.readUByte()).isNull()
    }

    @Test
    fun testReadUShort() {
        val stream = ByteArrayInputStream(ubyteArrayOf(0xffu, 0xaau, 0x00u, 0xffu).toByteArray())
        assertThat(stream.readUShort()).isEqualTo(0xaaffu.toUShort())
        assertThat(stream.readUShort()).isEqualTo(0xff00u.toUShort())
        assertThat(stream.readUShort()).isNull()

        val shortStream = ByteArrayInputStream(ubyteArrayOf(0xffu).toByteArray())
        assertThat(shortStream.readUShort()).isNull()
    }

    @Test
    fun testReadUInt() {
        val stream = ByteArrayInputStream(ubyteArrayOf(0xffu, 0xaau, 0x00u, 0xffu).toByteArray())
        assertThat(stream.readUInt()).isEqualTo(0xff00aaffu)
        assertThat(stream.readUInt()).isNull()

        val shortStream = ByteArrayInputStream(ubyteArrayOf(0xffu).toByteArray())
        assertThat(shortStream.readUInt()).isNull()
    }

    @Test
    fun testReadULong() {
        val stream =
            ByteArrayInputStream(
                ubyteArrayOf(0xffu, 0xaau, 0x00u, 0xffu, 0xffu, 0xaau, 0x00u, 0xffu).toByteArray()
            )
        assertThat(stream.readULong()).isEqualTo(0xff00aaffff00aaffuL)
        assertThat(stream.readULong()).isNull()

        val shortStream = ByteArrayInputStream(ubyteArrayOf(0xffu).toByteArray())
        assertThat(shortStream.readULong()).isNull()
    }

    @Test
    fun testReadUIntBe() {
        val stream = ByteArrayInputStream(ubyteArrayOf(0xffu, 0xaau, 0x00u, 0xffu).toByteArray())
        assertThat(stream.readUIntBe()).isEqualTo(0xffaa00ffu)
        assertThat(stream.readUIntBe()).isNull()

        val shortStream = ByteArrayInputStream(ubyteArrayOf(0xffu).toByteArray())
        assertThat(shortStream.readUIntBe()).isNull()
    }

    @Test
    fun testHasFlag() {
        assertThat(0x00u.toUByte().hasFlag(0x01u)).isFalse()
        assertThat(0x00u.toUByte().hasFlag(0x02u)).isFalse()
        assertThat(0x01u.toUByte().hasFlag(0x01u)).isTrue()
        assertThat(0x01u.toUByte().hasFlag(0x02u)).isFalse()
        assertThat(0x02u.toUByte().hasFlag(0x01u)).isFalse()
        assertThat(0x02u.toUByte().hasFlag(0x02u)).isTrue()
        assertThat(0x03u.toUByte().hasFlag(0x01u)).isTrue()
        assertThat(0x03u.toUByte().hasFlag(0x02u)).isTrue()
    }
}
