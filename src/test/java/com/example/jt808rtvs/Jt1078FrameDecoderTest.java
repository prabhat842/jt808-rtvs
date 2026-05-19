package com.example.jt808rtvs;

import com.example.jt808rtvs.ingest.Jt1078FrameDecoder;
import com.example.jt808rtvs.ingest.Jt1078MediaFrame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Jt1078FrameDecoderTest {
    private EmbeddedChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmbeddedChannel(new Jt1078FrameDecoder());
    }

    @AfterEach
    void tearDown() {
        channel.finishAndReleaseAll();
    }

    @Test
    void decodesWellFormedFrame() {
        ByteBuf buf = buildFrame("000000000001", 1, 42, 1, new byte[]{0x01, 0x02, 0x03, 0x04});
        channel.writeInbound(buf);

        Jt1078MediaFrame frame = channel.readInbound();
        assertNotNull(frame);
        assertEquals("000000000001", frame.terminalId());
        assertEquals(1, frame.channel());
        assertEquals(42, frame.sequence());
        assertEquals(1, frame.dataType());
        assertEquals(4, frame.payloadLength());
        frame.payload().release();
    }

    @Test
    void decodesTwoConsecutiveFrames() {
        byte[] payload = {0x11, 0x22};
        ByteBuf two = Unpooled.buffer();
        two.writeBytes(buildFrame("000000000002", 2, 1, 0, payload));
        two.writeBytes(buildFrame("000000000002", 2, 2, 0, payload));
        channel.writeInbound(two);

        Jt1078MediaFrame f1 = channel.readInbound();
        Jt1078MediaFrame f2 = channel.readInbound();
        assertNotNull(f1);
        assertNotNull(f2);
        assertEquals(1, f1.sequence());
        assertEquals(2, f2.sequence());
        f1.payload().release();
        f2.payload().release();
    }

    @Test
    void partialFrameBufferedUntilComplete() {
        ByteBuf full = buildFrame("000000000003", 1, 7, 0, new byte[]{0x55});
        // send header only
        ByteBuf part1 = full.readRetainedSlice(15);
        ByteBuf part2 = full;
        channel.writeInbound(part1);
        assertNull(channel.readInbound());
        channel.writeInbound(part2);
        Jt1078MediaFrame frame = channel.readInbound();
        assertNotNull(frame);
        frame.payload().release();
    }

    @Test
    void skipsGarbageBeforeMagic() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(new byte[]{0x00, 0x01, 0x02, 0x03, 0x04}); // garbage
        buf.writeBytes(buildFrame("000000000004", 3, 99, 2, new byte[]{(byte)0xAB, (byte)0xCD}));
        channel.writeInbound(buf);

        Jt1078MediaFrame frame = channel.readInbound();
        assertNotNull(frame);
        assertEquals(99, frame.sequence());
        frame.payload().release();
    }

    /** Builds a minimal but valid JT1078 30-byte header + payload. */
    private static ByteBuf buildFrame(String terminalId, int channel, int sequence, int dataType, byte[] payload) {
        ByteBuf buf = Unpooled.buffer(30 + payload.length);
        buf.writeInt(0x30316364);           // magic
        buf.writeByte(0x80);                // RTP flags
        buf.writeByte(0x60);                // payload type (H.264)
        buf.writeShort(sequence);           // sequence
        writeBcd(buf, terminalId, 6);       // terminal ID
        buf.writeByte(channel);             // channel
        buf.writeByte((dataType << 4));     // data-type nibble
        buf.writeLong(System.currentTimeMillis()); // timestamp
        buf.writeShort(0);                  // I-frame interval
        buf.writeShort(0);                  // frame interval
        buf.writeShort(payload.length);     // payload length
        buf.writeBytes(payload);
        return buf;
    }

    private static void writeBcd(ByteBuf buf, String digits, int bytes) {
        // left-pad with zeros to reach bytes*2 digits
        String padded = String.format("%" + (bytes * 2) + "s", digits).replace(' ', '0');
        for (int i = 0; i < bytes; i++) {
            int hi = Character.digit(padded.charAt(i * 2), 10);
            int lo = Character.digit(padded.charAt(i * 2 + 1), 10);
            buf.writeByte((byte) ((hi << 4) | lo));
        }
    }
}
