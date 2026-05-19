package com.example.jt808rtvs;

import com.example.jt808rtvs.media.MediaSessionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MediaSessionRegistryTest {
    private MediaSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MediaSessionRegistry();
    }

    @Test
    void recordCreatesSessionOnFirstFrame() {
        registry.record("00000000000001", 1, 512, 1);
        List<MediaSessionRegistry.MediaSession> sessions = registry.snapshot();
        assertEquals(1, sessions.size());
        assertEquals("00000000000001", sessions.get(0).terminalId());
        assertEquals(1, sessions.get(0).channelId());
        assertEquals(1, sessions.get(0).frames());
        assertEquals(512, sessions.get(0).bytes());
        assertTrue(sessions.get(0).active());
    }

    @Test
    void recordAccumulatesFramesAndBytes() {
        registry.record("00000000000001", 1, 500, 1);
        registry.record("00000000000001", 1, 300, 2);
        registry.record("00000000000001", 1, 200, 3);
        assertEquals(3, registry.frames("00000000000001", 1));
        assertEquals(1000, registry.bytes("00000000000001", 1));
    }

    @Test
    void closeMarksSessionInactive() {
        registry.record("00000000000002", 2, 100, 1);
        registry.close("00000000000002", 2);
        assertFalse(registry.snapshot().get(0).active());
    }

    @Test
    void multipleTerminalsTrackedIndependently() {
        registry.record("00000000000001", 1, 100, 1);
        registry.record("00000000000002", 1, 200, 1);
        registry.record("00000000000001", 2, 300, 1);
        assertEquals(3, registry.snapshot().size());
        assertEquals(1, registry.frames("00000000000001", 1));
        assertEquals(1, registry.frames("00000000000002", 1));
        assertEquals(1, registry.frames("00000000000001", 2));
    }

    @Test
    void framesAndBytesReturnZeroForUnknownSession() {
        assertEquals(0, registry.frames("unknown", 1));
        assertEquals(0, registry.bytes("unknown", 1));
    }
}
