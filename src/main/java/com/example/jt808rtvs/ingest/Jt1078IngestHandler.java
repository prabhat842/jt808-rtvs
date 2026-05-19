package com.example.jt808rtvs.ingest;

import com.example.jt808rtvs.media.MediaSessionRegistry;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.LongAdder;

@ChannelHandler.Sharable
public class Jt1078IngestHandler extends SimpleChannelInboundHandler<Jt1078MediaFrame> {
    private static final Logger log = LoggerFactory.getLogger(Jt1078IngestHandler.class);
    private static final LongAdder TOTAL_FRAMES = new LongAdder();
    private static final LongAdder TOTAL_BYTES = new LongAdder();

    private final MediaSessionRegistry registry;

    public Jt1078IngestHandler(MediaSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("JT1078 connection from {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Jt1078MediaFrame frame) {
        try {
            registry.record(frame.terminalId(), frame.channel(), frame.payloadLength(), frame.sequence());
            TOTAL_FRAMES.increment();
            TOTAL_BYTES.add(frame.payloadLength());

            long sessionFrames = registry.frames(frame.terminalId(), frame.channel());
            if (sessionFrames == 1 || sessionFrames % 250 == 0) {
                log.info("JT1078 {} ch={} frames={} bytes={} totalFrames={} totalBytes={}",
                        frame.terminalId(), frame.channel(), sessionFrames,
                        registry.bytes(frame.terminalId(), frame.channel()),
                        TOTAL_FRAMES.sum(), TOTAL_BYTES.sum());
            }
        } finally {
            frame.payload().release();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("JT1078 connection closed {}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("JT1078 ingest error", cause);
        ctx.close();
    }
}
