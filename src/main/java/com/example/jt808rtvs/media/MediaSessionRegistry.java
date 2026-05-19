package com.example.jt808rtvs.media;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MediaSessionRegistry {
    private final Map<String, MediaSession> sessions = new LinkedHashMap<>();

    public synchronized void open(String terminalId, int channelId) {
        sessions.put(key(terminalId, channelId), new MediaSession(
                terminalId,
                channelId,
                true,
                0,
                0,
                0,
                Instant.now(),
                Instant.now(),
                0));
    }

    public synchronized void record(String terminalId, int channelId, int payloadLength, int sequence) {
        String key = key(terminalId, channelId);
        MediaSession current = sessions.get(key);
        if (current == null) {
            current = new MediaSession(terminalId, channelId, true, 0, 0, 0, Instant.now(), Instant.now(), 0);
        }
        sessions.put(key, new MediaSession(
                terminalId,
                channelId,
                true,
                current.frames + 1,
                current.bytes + Math.max(payloadLength, 0),
                payloadLength,
                current.startedAt,
                Instant.now(),
                sequence));
    }

    public synchronized void close(String terminalId, int channelId) {
        String key = key(terminalId, channelId);
        MediaSession current = sessions.get(key);
        if (current != null) {
            sessions.put(key, current.withActive(false));
        }
    }

    public synchronized List<MediaSession> snapshot() {
        return new ArrayList<>(sessions.values());
    }

    public synchronized long frames(String terminalId, int channelId) {
        MediaSession s = sessions.get(key(terminalId, channelId));
        return s != null ? s.frames() : 0;
    }

    public synchronized long bytes(String terminalId, int channelId) {
        MediaSession s = sessions.get(key(terminalId, channelId));
        return s != null ? s.bytes() : 0;
    }

    private static String key(String terminalId, int channelId) {
        return terminalId + "#" + channelId;
    }

    public record MediaSession(
            String terminalId,
            int channelId,
            boolean active,
            long frames,
            long bytes,
            int lastFrameLength,
            Instant startedAt,
            Instant lastSeen,
            int lastSequence) {
        private MediaSession withActive(boolean active) {
            return new MediaSession(terminalId, channelId, active, frames, bytes, lastFrameLength, startedAt, lastSeen, lastSequence);
        }
    }
}
