package com.itmsg.device42.pipeline.d42maximo.conversion;

import com.itmsg.device42.pipeline.d42maximo.conversion.adapter.DpamAdapterImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.adapter.DpamAdptVariantImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.manufacturer.DpamManuVariantImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.manufacturer.DpamManufacturerImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.os.DpamOsImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.os.DpamOsVariantImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.processor.DpamProcVariantImport;
import com.itmsg.device42.pipeline.d42maximo.conversion.processor.DpamProcessorImport;
import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.NamedTask;
import com.itmsg.device42.runtime.TaskSequence;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("conversion")
public class ConversionIntegrationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(ConversionIntegrationJob.class);

    private final List<NamedTask> tasks;

    public ConversionIntegrationJob(
            DpamManufacturerImport dpamManufacturerImport,
            DpamManuVariantImport dpamManuVariantImport,
            DpamOsImport dpamOsImport,
            DpamOsVariantImport dpamOsVariantImport,
            DpamProcessorImport dpamProcessorImport,
            DpamProcVariantImport dpamProcVariantImport,
            DpamAdapterImport dpamAdapterImport,
            DpamAdptVariantImport dpamAdptVariantImport) {
        this.tasks = List.of(
                new NamedTask("DpamManufacturerIntegrate", dpamManufacturerImport::integrate),
                new NamedTask("DpamManuVariantIntegrate", dpamManuVariantImport::integrate),
                new NamedTask("DpamOsIntegrate", dpamOsImport::integrate),
                new NamedTask("DpamOsVariantIntegrate", dpamOsVariantImport::integrate),
                new NamedTask("DpamProcessorIntegrate", dpamProcessorImport::integrate),
                new NamedTask("DpamProcVariantIntegrate", dpamProcVariantImport::integrate),
                new NamedTask("DpamAdapterIntegrate", dpamAdapterImport::integrate),
                new NamedTask("DpamAdptVariantIntegrate", dpamAdptVariantImport::integrate)
        );
    }

    @Override
    public void run() {
        TaskSequence.run(log, tasks);
    }
}
