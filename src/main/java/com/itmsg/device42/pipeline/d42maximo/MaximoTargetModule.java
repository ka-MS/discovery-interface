package com.itmsg.device42.pipeline.d42maximo;

import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.TargetModule;
import com.itmsg.device42.source.device42.Device42DatabaseConfig;
import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.pipeline.d42maximo.asset.AssetIntegrationJob;
import com.itmsg.device42.pipeline.d42maximo.ci.CiIntegrationJob;
import com.itmsg.device42.pipeline.d42maximo.ci.relation.CiRelationJob;
import com.itmsg.device42.pipeline.d42maximo.conversion.ConversionIntegrationJob;
import com.itmsg.device42.pipeline.d42maximo.software.SoftwareIntegrationJob;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "integration.target", havingValue = "maximo", matchIfMissing = true)
@ComponentScan(basePackages = {"com.itmsg.device42.pipeline.d42maximo.asset",
        "com.itmsg.device42.pipeline.d42maximo.ci", "com.itmsg.device42.pipeline.d42maximo.conversion",
        "com.itmsg.device42.pipeline.d42maximo.software", "com.itmsg.device42.target.maximo"})
@Import({MaximoQueries.class, Device42DatabaseConfig.class, DoqlClient.class, DataSourceAutoConfiguration.class})
public class MaximoTargetModule implements TargetModule {
    private final Map<String, IntegrationJob> jobs;

    public MaximoTargetModule(AssetIntegrationJob asset, CiIntegrationJob ci, CiRelationJob relation,
                              ConversionIntegrationJob conversion, SoftwareIntegrationJob software) {
        jobs = Map.of("asset", asset, "ci", ci, "ci-relation", relation,
                "conversion", conversion, "software", software);
    }

    @Override
    public String id() { return "maximo"; }

    @Override
    public Map<String, IntegrationJob> jobs() { return jobs; }
}
