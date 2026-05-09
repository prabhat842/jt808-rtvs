package com.example.jt808rtvs.web;

import com.example.jt808rtvs.media.MediaSessionRegistry;

public final class StudioRoutes {
    private final MediaSessionRegistry mediaSessions;

    public StudioRoutes(MediaSessionRegistry mediaSessions) {
        this.mediaSessions = mediaSessions;
    }

    public MediaSessionRegistry mediaSessions() {
        return mediaSessions;
    }
}

