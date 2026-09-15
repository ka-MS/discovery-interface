package com.itmsg.device42.integration.ci;

import com.itmsg.device42.integration.IntegrationJob;
import com.itmsg.device42.integration.ci.relation.CiRelationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("ci")
public class CiIntegrationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(CiIntegrationJob.class);

    private final List<CiIntegrationTask> tasks;
    private final CiDefinitionLoader definitionLoader;
    private final CiRelationJob relationJob;

    public CiIntegrationJob(List<CiIntegrationTask> tasks, CiDefinitionLoader definitionLoader,
                            CiRelationJob relationJob) {
        this.tasks = tasks;
        this.definitionLoader = definitionLoader;
        this.relationJob = relationJob;
    }

    @Override
    public void run() {
        CiDefinitionCache definitions = definitionLoader.load();
        List<Exception> failures = new ArrayList<>();

        for (CiIntegrationTask task : tasks) {
            String taskName = task.getClass().getSimpleName();

            try {
                log.info("{} 작업을 시작합니다.", taskName);
                task.integrate(definitions);
                log.info("{} 작업이 완료되었습니다.", taskName);
            } catch (Exception e) {
                failures.add(e);
                log.error("{} 작업에 실패했습니다. 다음 작업을 계속합니다.",
                        taskName, e);
            }
        }

        relationJob.run();
    }
}
