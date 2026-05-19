package com.example.jt808rtvs.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

public class RtvsConfig {
    private Endpoint ingest = new Endpoint("0.0.0.0", 1078);
    private Endpoint studio = new Endpoint("0.0.0.0", 8089);

    public static RtvsConfig load(Path path) throws IOException {
        return new ObjectMapper().findAndRegisterModules().readValue(path.toFile(), RtvsConfig.class);
    }

    public Endpoint getIngest() { return ingest; }
    public void setIngest(Endpoint ingest) { this.ingest = ingest; }

    public Endpoint getStudio() { return studio; }
    public void setStudio(Endpoint studio) { this.studio = studio; }

    public static class Endpoint {
        private String host;
        private int port;

        public Endpoint() {}
        public Endpoint(String host, int port) { this.host = host; this.port = port; }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }

        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }
}
