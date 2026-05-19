package com.example.jt808rtvs.ingest;

import io.netty.buffer.ByteBuf;

public record Jt1078MediaFrame(
        String terminalId,
        int channel,
        int sequence,
        int dataType,
        int payloadType,
        int payloadLength,
        ByteBuf payload) {
}
