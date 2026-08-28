package com.itmsg.device42.integration.asset;

import com.itmsg.device42.integration.IntegrationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component("asset")
public class AssetIntegrationJob implements IntegrationJob {

    private static final Logger log =
            LoggerFactory.getLogger(AssetIntegrationJob.class);

    private final List<AssetIntegrationTask> tasks;

    public AssetIntegrationJob(List<AssetIntegrationTask> tasks) {
        this.tasks = tasks;
    }

    @Override
    public void run() {
        List<Exception> failures = new ArrayList<>();

        for (AssetIntegrationTask task : tasks) {
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
