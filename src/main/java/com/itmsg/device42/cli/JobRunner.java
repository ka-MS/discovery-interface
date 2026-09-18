package com.itmsg.device42.cli;

import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.TargetModule;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.List;

@Component
public class JobRunner implements CommandLineRunner {

    private final Map<String, IntegrationJob> integratorMap;

    public JobRunner(@Value("${integration.target:maximo}") String target, List<TargetModule> modules) {
        var selected = modules.stream().filter(module -> module.id().equals(target)).toList();
        if (selected.size() != 1 || modules.size() != 1) {
            throw new IllegalArgumentException("타겟은 정확히 하나 등록·활성화되어야 합니다. target=" + target
                    + ", active=" + modules.stream().map(TargetModule::id).toList());
        }
        this.integratorMap = Map.copyOf(selected.getFirst().jobs());
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
