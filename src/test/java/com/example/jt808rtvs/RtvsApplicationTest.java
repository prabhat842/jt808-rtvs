package com.example.jt808rtvs;

import com.example.jt808rtvs.config.RtvsConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RtvsApplicationTest {
    @Test
    void boots() {
        RtvsConfig config = new RtvsConfig();
        assertNotNull(new RtvsApplication(config));
    }
}

