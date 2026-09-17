package com.itmsg.device42.integration.d42maximo.software;

import com.itmsg.device42.integration.d42maximo.software.catalog.TloamSoftwareImport;
import com.itmsg.device42.integration.d42maximo.software.installed.DpaSoftwareImport;
import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.NamedTask;
import com.itmsg.device42.runtime.TaskSequence;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("software")
public class SoftwareIntegrationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(SoftwareIntegrationJob.class);

    private final List<NamedTask> tasks;

    public SoftwareIntegrationJob(TloamSoftwareImport tloamSoftwareImport, DpaSoftwareImport dpaSoftwareImport) {
        this.tasks = List.of(
                new NamedTask("TloamSoftwareIntegrate", tloamSoftwareImport::integrate),
                new NamedTask("DpaSoftwareIntegrate", dpaSoftwareImport::integrate)
        );
    }

    @Override
    public void run() {
        TaskSequence.run(log, tasks);
    }
}
