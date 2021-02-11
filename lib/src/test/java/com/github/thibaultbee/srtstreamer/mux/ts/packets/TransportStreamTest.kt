package com.github.thibaultbee.srtstreamer.mux.ts.packets

import com.github.thibaultbee.srtstreamer.mux.IMuxListener
import com.github.thibaultbee.srtstreamer.mux.ts.descriptors.AdaptationField
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import kotlin.math.min
import kotlin.random.Random

class TransportStreamTest {
    class MockMuxListener(val expectedBuffer: ByteBuffer) : IMuxListener {
        var nBuffer = 0
        override fun onOutputFrame(buffer: ByteBuffer) {
            assertEquals(TS.PACKET_SIZE, buffer.capacity())
            assertEquals(TS.SYNC_BYTE, buffer[0])
            for (i in 0 until min(buffer.limit() - 4, expectedBuffer.remaining())) {
                assertEquals(expectedBuffer.get(), buffer[i + 4])  /* Drop header */
            }
            nBuffer++
        }
    }

    class MockTransportStream(muxListener: IMuxListener, pid: Short = Random.nextInt().toShort()) :
        TS(muxListener, pid) {
        fun mockWrite(
            payload: ByteBuffer? = null,
            adaptationField: AdaptationField? = null,
            specificHeader: ByteBuffer? = null
        ) = write(payload, adaptationField, specificHeader)
    }

    /**
     * Generates a randomized ByteBuffer
     * @param size size of buffer to generates
     * @return random ByteBuffer
     */
    private fun generateRandomBuffer(size: Int): ByteBuffer {
        val buffer = ByteBuffer.allocate(size)
        buffer.put(Random.nextBytes(size))
        buffer.rewind()
        return buffer
    }

    @Test
    fun oneSmallPayloadTest() {
        val payload = generateRandomBuffer(10)
        val listener = MockMuxListener(payload.duplicate())

        val transportStream =
            MockTransportStream(muxListener = listener)
        transportStream.mockWrite(payload)
    }

    @Test
    fun oneLargePayloadTest() {
        val payload = generateRandomBuffer(TS.PACKET_SIZE - 4) // 4 - header size
        val listener = MockMuxListener(payload.duplicate())

        val transportStream =
            MockTransportStream(muxListener = listener)
        transportStream.mockWrite(payload)
    }

    @Test
    fun twoPayloadTest() {
        val payload = generateRandomBuffer(TS.PACKET_SIZE - 3) // 4 - header size
        val listener = MockMuxListener(payload.duplicate())

        val transportStream =
            MockTransportStream(muxListener = listener)
        transportStream.mockWrite(payload)
    }

    @Test
    fun adaptationHeaderAndPayloadTest() {
        val payload = generateRandomBuffer(TS.PACKET_SIZE)
        val adaptationField = AdaptationField()
        val header = generateRandomBuffer(20)

        val expectedBuffer =
            ByteBuffer.allocate(payload.limit() + header.limit() + adaptationField.size)
        expectedBuffer.put(adaptationField.asByteBuffer())
        expectedBuffer.put(header)
        expectedBuffer.put(payload)
        val listener = MockMuxListener(expectedBuffer)

        payload.rewind()
        header.rewind()

        val transportStream =
            MockTransportStream(muxListener = listener)
        transportStream.mockWrite(payload, adaptationField, header)
    }
}