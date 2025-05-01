package org.sunsetware.omio

import java.io.InputStream

internal class OggReader(private val stream: InputStream) {
    private var pageHeader = null as OggPageHeader?
    private var pageCursor = 0

    inner class OggPacketStream : InputStream() {
        val currentPageHeader
            get() = checkNotNull(pageHeader)

        private var segmentSize = 0
        private var segmentCursor = 0
        private var isLastSegment = false

        init {
            nextSegment()
        }

        override fun available(): Int {
            return segmentSize - segmentCursor
        }

        override fun read(): Int {
            var result = null as Int?
            while (result == null) {
                when {
                    segmentCursor < segmentSize -> {
                        segmentCursor++
                        result = stream.read()
                    }
                    isLastSegment -> result = -1
                    else -> {
                        nextSegment()
                    }
                }
            }
            return result
        }

        private fun nextSegment() {
            try {
                if (pageHeader == null || pageCursor >= pageHeader!!.segmentTable.size) {
                    pageHeader = stream.readOggPageHeader()
                    pageCursor = 0
                }
                segmentSize = pageHeader!!.segmentTable[pageCursor].toInt()
                segmentCursor = 0
                pageCursor++
                isLastSegment = segmentSize < 255
            } catch (ex: Exception) {
                throw OggException("Can't read ogg packet", ex)
            }
        }
    }

    inline fun readPacket(crossinline block: (OggPacketStream) -> Unit) {
        val packetStream = OggPacketStream()
        block(packetStream)
        @Suppress("ControlFlowWithEmptyBody") while (packetStream.skip(4096) >= 4096L) {}
    }
}
