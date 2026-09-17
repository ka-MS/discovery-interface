package com.itmsg.device42.cli;

import com.itmsg.device42.runtime.IntegrationJob;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class JobRunner implements CommandLineRunner {

    private Map<String, IntegrationJob> integratorMap;

    public JobRunner(Map<String, IntegrationJob> integratorMap) {
        this.integratorMap = integratorMap;
    }

    @Override
    public void run(String... args) throws Exception {
        for (String arg : args) {
            IntegrationJob integreator = integratorMap.get(arg);
            if (integreator != null) {
                integreator.run();
            }
        }
    }
}
