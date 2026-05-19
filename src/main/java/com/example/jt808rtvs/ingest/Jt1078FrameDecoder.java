package com.example.jt808rtvs.ingest;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

/**
 * Decodes a raw TCP stream into {@link Jt1078MediaFrame} objects.
 *
 * JT1078 packet layout (30-byte fixed header + variable payload):
 *   [0..3]   magic 0x30316364
 *   [4]      RTP flags (V/P/X/CC)
 *   [5]      PT (payload type, bit7 = marker)
 *   [6..7]   sequence number
 *   [8..13]  terminal ID (6 BCD bytes = 12 digits)
 *   [14]     channel ID
 *   [15]     data-type nibble (hi) + subpackage marker (lo)
 *   [16..23] timestamp (8 bytes)
 *   [24..25] I-frame interval
 *   [26..27] frame interval
 *   [28..29] payload length
 *   [30..]   payload
 */
public class Jt1078FrameDecoder extends ByteToMessageDecoder {
    private static final int MAGIC = 0x30316364;
    private static final int HEADER_LEN = 30;
    private static final int MAX_PAYLOAD = 65_535;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() >= HEADER_LEN) {
            int magicAt = findMagic(in);
            if (magicAt < 0) {
                in.skipBytes(in.readableBytes() - 3);
                return;
            }
            in.readerIndex(magicAt);
            if (in.readableBytes() < HEADER_LEN) {
                return;
            }

            int payloadLength = in.getUnsignedShort(magicAt + 28);
            if (payloadLength > MAX_PAYLOAD) {
                throw new CorruptedFrameException("JT1078 payload too large: " + payloadLength);
            }
            if (in.readableBytes() < HEADER_LEN + payloadLength) {
                return;
            }

            in.skipBytes(4);                           // magic
            in.skipBytes(1);                           // RTP flags
            int payloadType = in.readUnsignedByte() & 0x7F;
            int sequence = in.readUnsignedShort();
            String terminalId = readBcd(in, 6);
            int channel = in.readUnsignedByte();
            int dataTypeByte = in.readUnsignedByte();
            int dataType = (dataTypeByte >>> 4) & 0x0F;
            in.skipBytes(8);                           // timestamp
            in.skipBytes(2);                           // I-frame interval
            in.skipBytes(2);                           // frame interval
            in.skipBytes(2);                           // payload length already read
            ByteBuf payload = in.readRetainedSlice(payloadLength);
            out.add(new Jt1078MediaFrame(terminalId, channel, sequence, dataType, payloadType, payloadLength, payload));
        }
    }

    private static int findMagic(ByteBuf buf) {
        for (int i = buf.readerIndex(); i <= buf.writerIndex() - 4; i++) {
            if (buf.getInt(i) == MAGIC) return i;
        }
        return -1;
    }

    private static String readBcd(ByteBuf buf, int bytes) {
        StringBuilder sb = new StringBuilder(bytes * 2);
        for (int i = 0; i < bytes; i++) {
            int b = buf.readUnsignedByte();
            sb.append((b >> 4) & 0x0F);
            sb.append(b & 0x0F);
        }
        return sb.toString();
    }
}
