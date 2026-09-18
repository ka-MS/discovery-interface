package com.itmsg.device42.pipeline.d42maximo.ci;

import com.itmsg.device42.pipeline.d42maximo.ci.databaseinstance.DatabaseInstanceCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.device.DeviceCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.disk.DiskCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.filesystem.FilesystemCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.ip.IpCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.pipeline.d42maximo.ci.os.OsCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.relation.CiRelationJob;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionLoader;
import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.NamedTask;
import com.itmsg.device42.runtime.TaskSequence;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("ci")
public class CiIntegrationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(CiIntegrationJob.class);

    private final DatabaseInstanceCiImport databaseInstanceCiImport;
    private final DeviceCiImport deviceCiImport;
    private final DiskCiImport diskCiImport;
    private final FilesystemCiImport filesystemCiImport;
    private final IpCiImport ipCiImport;
    private final OsCiImport osCiImport;
    private final CiDefinitionLoader definitionLoader;
    private final CiRelationJob relationJob;

    public CiIntegrationJob(
            DatabaseInstanceCiImport databaseInstanceCiImport,
            DeviceCiImport deviceCiImport,
            DiskCiImport diskCiImport,
            FilesystemCiImport filesystemCiImport,
            IpCiImport ipCiImport,
            OsCiImport osCiImport,
            CiDefinitionLoader definitionLoader,
            CiRelationJob relationJob) {
        this.databaseInstanceCiImport = databaseInstanceCiImport;
        this.deviceCiImport = deviceCiImport;
        this.diskCiImport = diskCiImport;
        this.filesystemCiImport = filesystemCiImport;
        this.ipCiImport = ipCiImport;
        this.osCiImport = osCiImport;
        this.definitionLoader = definitionLoader;
        this.relationJob = relationJob;
    }

    @Override
    public void run() {
        CiDefinitionCache definitions = definitionLoader.load(CiClassification.ids());
        TaskSequence.run(log, List.of(
                new NamedTask("DatabaseInstanceCiIntegrate", () -> databaseInstanceCiImport.integrate(definitions)),
                new NamedTask("DeviceCiIntegrate", () -> deviceCiImport.integrate(definitions)),
                new NamedTask("DiskCiIntegrate", () -> diskCiImport.integrate(definitions)),
                new NamedTask("FilesystemCiIntegrate", () -> filesystemCiImport.integrate(definitions)),
                new NamedTask("IpCiIntegrate", () -> ipCiImport.integrate(definitions)),
                new NamedTask("OsCiIntegrate", () -> osCiImport.integrate(definitions))
        ));
        relationJob.run();
    }
}
