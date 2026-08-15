package com.pozit.pozitserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "pozing.edit.worker.enabled=false",
        "pozing.thumbnail.worker.enabled=false",
        "pozing.edit.cleanup.enabled=false"
})
class PozitServerApplicationTests {

    @Test
    void contextLoads() {
    }

}
