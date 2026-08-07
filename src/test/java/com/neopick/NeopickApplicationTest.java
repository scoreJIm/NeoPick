package com.neopick;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class NeopickApplicationTest {

    @Test
    void contextLoads() {
        // Verify Spring context starts with local profile (H2, mock services)
    }
}
