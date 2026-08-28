package com.itmsg.device42.integration.software;

import com.itmsg.device42.integration.IntegrationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("software")
public class SoftwareIntegrationJob implements IntegrationJob {

    private static final Logger log = LoggerFactory.getLogger(SoftwareIntegrationJob.class);

    private final List<SoftwareIntegrationTask> tasks;

    public SoftwareIntegrationJob(List<SoftwareIntegrationTask> tasks) {
        this.tasks = tasks;
    }

    @Override
    public void run() {
        for (SoftwareIntegrationTask task : tasks) {
            String taskName = task.getClass().getSimpleName();

            try {
                log.info("{} 작업을 시작합니다.", taskName);
                task.integrate();
                log.info("{} 작업이 완료되었습니다.", taskName);
            } catch (Exception e) {
                log.error("{} 작업에 실패했습니다. 다음 작업을 계속합니다.", taskName, e);
            }
        }
    }
}
