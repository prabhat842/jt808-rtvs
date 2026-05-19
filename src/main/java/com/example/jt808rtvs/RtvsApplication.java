package com.example.jt808rtvs;

import com.example.jt808rtvs.config.RtvsConfig;
import com.example.jt808rtvs.ingest.Jt1078FrameDecoder;
import com.example.jt808rtvs.ingest.Jt1078IngestHandler;
import com.example.jt808rtvs.media.MediaSessionRegistry;
import com.example.jt808rtvs.web.StudioRoutes;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public final class RtvsApplication implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(RtvsApplication.class);

    private final RtvsConfig config;
    private final MediaSessionRegistry mediaSessions = new MediaSessionRegistry();
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel ingestChannel;
    private Channel studioChannel;

    public RtvsApplication(RtvsConfig config) {
        this.config = config;
    }

    public void start() throws InterruptedException {
        ingestChannel = bindIngest();
        studioChannel = bindStudio();
    }

    private Channel bindIngest() throws InterruptedException {
        Jt1078IngestHandler handler = new Jt1078IngestHandler(mediaSessions);
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 4096)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new Jt1078FrameDecoder())
                                .addLast(handler);
                    }
                });
        Channel ch = bootstrap.bind(new InetSocketAddress(config.getIngest().getHost(), config.getIngest().getPort()))
                .sync().channel();
        log.info("JT1078 ingest listening on {}:{}", config.getIngest().getHost(), config.getIngest().getPort());
        return ch;
    }

    private Channel bindStudio() throws InterruptedException {
        StudioRoutes routes = new StudioRoutes(mediaSessions);
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 256)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(65_536))
                                .addLast(routes);
                    }
                });
        Channel ch = bootstrap.bind(new InetSocketAddress(config.getStudio().getHost(), config.getStudio().getPort()))
                .sync().channel();
        log.info("RTVS studio listening on {}:{}", config.getStudio().getHost(), config.getStudio().getPort());
        return ch;
    }

    @Override
    public void close() {
        if (ingestChannel != null) ingestChannel.close();
        if (studioChannel != null) studioChannel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    public MediaSessionRegistry mediaSessions() {
        return mediaSessions;
    }

    public static void main(String[] args) throws Exception {
        Path configPath = parseConfigPath(args);
        RtvsConfig config = RtvsConfig.load(configPath);
        RtvsApplication app = new RtvsApplication(config);
        Runtime.getRuntime().addShutdownHook(new Thread(app::close, "rtvs-shutdown"));
        log.info("starting jt808-rtvs with config {}", configPath);
        app.start();
        new CountDownLatch(1).await();
    }

    private static Path parseConfigPath(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--config".equals(args[i])) return Path.of(args[i + 1]);
        }
        if (args.length == 1 && !args[0].startsWith("--")) return Path.of(args[0]);
        if (Arrays.asList(args).contains("--help")) {
            System.out.println("Usage: java -jar jt808-rtvs.jar --config config/rtvs.json");
            System.exit(0);
        }
        return Path.of("config/rtvs.json");
    }
}
