package com.itmsg.device42.integration.conversion;

import com.itmsg.device42.integration.IntegrationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("conversion")
public class ConversionIntegrationJob implements IntegrationJob {

    private static final Logger log =
            LoggerFactory.getLogger(ConversionIntegrationJob.class);

    private final List<ConversionIntegrationTask> tasks;

    public ConversionIntegrationJob(List<ConversionIntegrationTask> tasks) {
        this.tasks = tasks;
    }

    @Override
    public void run() {
        List<Exception> failures = new ArrayList<>();

        for (ConversionIntegrationTask task : tasks) {
            String taskName = task.getClass().getSimpleName();

            try {
                log.info("{} 작업을 시작합니다.", taskName);

                task.integrate();

                log.info("{} 작업이 완료되었습니다.", taskName);
            } catch (Exception e) {
                failures.add(e);
                log.error("{} 작업에 실패했습니다. 다음 작업을 계속합니다.",
                        taskName, e);
            }
        }
    }
}
