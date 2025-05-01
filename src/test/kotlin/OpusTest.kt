import com.jayway.jsonpath.JsonPath
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.opentest4j.TestAbortedException
import org.sunsetware.omio.OpusException
import org.sunsetware.omio.VORBIS_COMMENT_DESCRIPTION
import org.sunsetware.omio.VORBIS_COMMENT_METADATA_BLOCK_PICTURE
import org.sunsetware.omio.VORBIS_COMMENT_TRACKNUMBER
import org.sunsetware.omio.VORBIS_COMMENT_UNOFFICIAL_ALBUMARTIST
import org.sunsetware.omio.VORBIS_COMMENT_UNOFFICIAL_DISCNUMBER
import org.sunsetware.omio.decodeMetadataBlockPicture
import org.sunsetware.omio.readOpusMetadata

class OpusTest {
    val testDataDir = File("testdata")

    @TestFactory
    fun testNormal(): Iterable<DynamicTest> {
        val ffprobeTagMapping =
            mapOf(
                VORBIS_COMMENT_DESCRIPTION to "comment",
                VORBIS_COMMENT_UNOFFICIAL_ALBUMARTIST to "album_artist",
                VORBIS_COMMENT_TRACKNUMBER to "track",
                VORBIS_COMMENT_UNOFFICIAL_DISCNUMBER to "disc",
            )

        val files =
            testDataDir
                .listFiles { it.extension.equals("ogg", true) || it.extension.equals("opus", true) }
                .let(::requireNotNull)
        if (files.isEmpty()) fail("No test data")
        return files.map { file ->
            DynamicTest.dynamicTest(file.name) {
                BufferedInputStream(FileInputStream(file)).use { stream ->
                    val metadata = readOpusMetadata(stream, true)
                    println(metadata)
                    val ffprobe =
                        Runtime.getRuntime()
                            .exec(
                                arrayOf(
                                    "ffprobe",
                                    "-v",
                                    "quiet",
                                    "-print_format",
                                    "json",
                                    "-show_format",
                                    "-show_streams",
                                    file.absolutePath,
                                ),
                                null,
                            )
                    val ffprobeResult =
                        ffprobe.inputStream
                            .let { IOUtils.toString(it, Charsets.UTF_8) }
                            .let(JsonPath::parse)
                    assertThat(ffprobe.waitFor()).isEqualTo(0)
                    assertThat(
                            metadata.userComments
                                .map {
                                    ffprobeTagMapping.getOrDefault(it.key, it.key) to
                                        it.value.joinToString(";")
                                }
                                .filter {
                                    it.first != "encoder" &&
                                        it.first != VORBIS_COMMENT_METADATA_BLOCK_PICTURE
                                }
                                // hack for when both "comment" and "description" exist
                                .distinct()
                        )
                        .containsExactlyInAnyOrderElementsOf(
                            ffprobeResult
                                .read<List<Map<String, String>>>(
                                    "$.streams[?(@.codec_name == 'opus')].tags"
                                )
                                .single()
                                .map { it.key.lowercase() to it.value }
                                .filter { it.first != "encoder" }
                        )

                    val ffmpeg =
                        Runtime.getRuntime()
                            .exec(
                                arrayOf(
                                    "ffmpeg",
                                    "-i",
                                    file.absolutePath,
                                    "-vn",
                                    "-f",
                                    "null",
                                    "-",
                                ),
                                null,
                            )
                    val ffmpegDuration =
                        ffmpeg.errorStream
                            .let { IOUtils.toString(it, Charsets.UTF_8) }
                            .trim()
                            .lines()
                            .last()
                            .let { Regex("(?<=time=)[^ ]+").find(it)!!.value }
                            .let { LocalTime.parse(it, DateTimeFormatter.ofPattern("HH:mm:ss.SS")) }
                            .toNanoOfDay()
                            .nanoseconds
                    assertThat(ffmpeg.waitFor()).isEqualTo(0)
                    assertThat(metadata.duration!!)
                        .isBetween(ffmpegDuration - 5.milliseconds, ffmpegDuration + 5.milliseconds)

                    val pictureCount =
                        ffprobeResult
                            .read<List<Any>>(
                                "$.streams[?(@.codec_type == 'video' && @.disposition.attached_pic == 1)]"
                            )
                            .size
                    val metadataBlockPictures =
                        metadata.userComments[VORBIS_COMMENT_METADATA_BLOCK_PICTURE]?.mapNotNull(
                            ::decodeMetadataBlockPicture
                        ) ?: emptyList()
                    assertThat(pictureCount).isEqualTo(metadataBlockPictures.size)
                    for (picture in metadataBlockPictures) {
                        println("## picture")
                        println(picture)
                        val pictureProbe =
                            Runtime.getRuntime()
                                .exec(
                                    arrayOf(
                                        "ffprobe",
                                        "-v",
                                        "quiet",
                                        "-print_format",
                                        "json",
                                        "-show_format",
                                        "-show_streams",
                                        "-",
                                    ),
                                    null,
                                )
                        pictureProbe.outputStream.write(picture.data)
                        pictureProbe.outputStream.close()
                        val pictureResult =
                            pictureProbe.inputStream
                                .let { IOUtils.toString(it, Charsets.UTF_8) }
                                .let(JsonPath::parse)
                        println(pictureResult.jsonString())
                        assertThat(pictureProbe.waitFor()).isEqualTo(0)
                        assertThat(
                                pictureResult
                                    .read<List<Int>>("$.streams[?(@.codec_type == 'video')].width")
                                    .single()
                            )
                            .isGreaterThan(0)
                    }
                }
            }
        }
    }

    @TestFactory
    fun testBadInput(): Iterable<DynamicTest> {
        val files =
            testDataDir.listFiles { it.extension.equals("notopus", true) }.let(::requireNotNull)
        if (files.isEmpty()) fail("No test data")
        return files.map { file ->
            DynamicTest.dynamicTest(file.name) {
                assertThatThrownBy {
                        BufferedInputStream(FileInputStream(file)).use { stream ->
                            readOpusMetadata(stream, true)
                        }
                    }
                    .isExactlyInstanceOf(OpusException::class.java)
            }
        }
    }

    @TestFactory
    fun testSizeLimit(): Iterable<DynamicTest> {
        val files =
            testDataDir
                .listFiles { it.extension.equals("ogg", true) || it.extension.equals("opus", true) }
                .let(::requireNotNull)
        if (files.isEmpty()) fail("No test data")
        return files.map { file ->
            DynamicTest.dynamicTest(file.name) {
                fun read() {
                    BufferedInputStream(FileInputStream(file)).use { stream ->
                        readOpusMetadata(stream, true, 64)
                    }
                }
                if (file.nameWithoutExtension.endsWith(".small")) {
                    read()
                } else {
                    assertThatThrownBy { read() }.isExactlyInstanceOf(OpusException::class.java)
                }
            }
        }
    }

    /** Not exactly accurate, but I can't setup JMH */
    @Test
    fun testPerformance() {
        val file = File("testdata/performance.opus").readBytes()
        val repeat = 99

        run {
            val results = mutableListOf<Duration>()
            repeat(repeat) {
                val start = System.nanoTime().nanoseconds
                ByteArrayInputStream(file).use { stream -> readOpusMetadata(stream, true) }

                val finish = System.nanoTime().nanoseconds
                results += finish - start
            }
            println(
                "With duration: mean ${results.sumOf { it.inWholeNanoseconds }.div(repeat).nanoseconds}, median ${results.sorted()[repeat / 2]}"
            )
        }

        run {
            val results = mutableListOf<Duration>()
            repeat(repeat) {
                val start = System.nanoTime().nanoseconds
                ByteArrayInputStream(file).use { stream -> readOpusMetadata(stream, false) }

                val finish = System.nanoTime().nanoseconds
                results += finish - start
            }
            println(
                "Without duration: mean ${results.sumOf { it.inWholeNanoseconds }.div(repeat).nanoseconds}, median ${results.sorted()[repeat / 2]}"
            )
        }

        throw TestAbortedException("Not a test")
    }
}
