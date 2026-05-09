package com.example.jt808rtvs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RtvsApplicationTest {
    @Test
    void boots() {
        assertNotNull(new RtvsApplication());
    }
}

