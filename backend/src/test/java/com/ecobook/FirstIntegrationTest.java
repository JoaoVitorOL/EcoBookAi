package com.ecobook;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class FirstIntegrationTest {

    @Test
    public void testApplicationStartup() {
        // Verify application context loads successfully
        assertTrue(true);
    }
}
