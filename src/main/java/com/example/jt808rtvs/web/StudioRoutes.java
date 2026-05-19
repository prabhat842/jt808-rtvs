package com.example.jt808rtvs.web;

import com.example.jt808rtvs.media.MediaSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class StudioRoutes extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MediaSessionRegistry mediaSessions;

    public StudioRoutes(MediaSessionRegistry mediaSessions) {
        this.mediaSessions = mediaSessions;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri().split("\\?")[0];
        FullHttpResponse response = switch (uri) {
            case "/", "/index.html" -> serveResource("rtvs/index.html", "text/html; charset=UTF-8");
            case "/app.js"          -> serveResource("rtvs/app.js", "application/javascript");
            case "/styles.css"      -> serveResource("rtvs/styles.css", "text/css");
            case "/api/health"      -> json("{\"status\":\"ok\"}");
            case "/api/sessions"    -> json(MAPPER.writeValueAsString(mediaSessions.snapshot()));
            default                 -> notFound(uri);
        };
        boolean keepAlive = !req.headers().get(HttpHeaderNames.CONNECTION, "").equalsIgnoreCase("close");
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private static FullHttpResponse serveResource(String path, String contentType) {
        try (InputStream in = StudioRoutes.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return notFound(path);
            byte[] bytes = in.readAllBytes();
            ByteBuf body = Unpooled.wrappedBuffer(bytes);
            FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, body);
            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
            return resp;
        } catch (Exception e) {
            return error(e.getMessage());
        }
    }

    private static FullHttpResponse json(String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return resp;
    }

    private static FullHttpResponse notFound(String path) {
        byte[] bytes = ("Not found: " + path).getBytes(StandardCharsets.UTF_8);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                Unpooled.wrappedBuffer(bytes));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return resp;
    }

    private static FullHttpResponse error(String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                Unpooled.wrappedBuffer(bytes));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return resp;
    }

    public MediaSessionRegistry mediaSessions() {
        return mediaSessions;
    }
}
