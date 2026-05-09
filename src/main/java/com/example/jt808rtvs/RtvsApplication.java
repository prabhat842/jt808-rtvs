package com.example.jt808rtvs;

import com.example.jt808rtvs.media.MediaSessionRegistry;
import com.example.jt808rtvs.web.StudioRoutes;

public final class RtvsApplication {
    private final MediaSessionRegistry mediaSessions = new MediaSessionRegistry();
    private final StudioRoutes studioRoutes = new StudioRoutes(mediaSessions);

    public static void main(String[] args) {
        System.out.println("JT808 RTVS scaffold is ready.");
        System.out.println("Next step: wire the Netty HTTP API and JT1078 ingest path.");
    }

    public MediaSessionRegistry mediaSessions() {
        return mediaSessions;
    }

    public StudioRoutes studioRoutes() {
        return studioRoutes;
    }
}

