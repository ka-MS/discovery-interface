package com.itmsg.device42.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.itmsg.device42.runtime.IntegrationJob;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DiscoveryInterfaceApplicationTests {

    @Autowired
    Map<String, IntegrationJob> jobs;

    @Test
    void contextLoads() {
        assertThat(jobs.keySet()).containsExactlyInAnyOrder("asset", "ci", "ci-relation", "conversion", "software");
    }

}
