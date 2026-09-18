package com.itmsg.device42.architecture;

import com.itmsg.device42.DiscoveryInterfaceApplication;
import com.itmsg.device42.pipeline.d42test.AlternativeTarget;
import com.itmsg.device42.pipeline.d42test.AlternativeTarget.CpuInventory;
import com.itmsg.device42.cli.JobRunner;
import com.itmsg.device42.runtime.TargetModule;
import com.itmsg.device42.pipeline.d42maximo.MaximoTargetModule;
import com.itmsg.device42.target.maximo.ci.ActCiWriter;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TargetSelectionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DiscoveryInterfaceApplication.class);

    private ApplicationContextRunner maximo() {
        return runner.withPropertyValues("device42.doql.jdbc.driver-class-name=com.device42.jdbc.D42Driver",
                "device42.doql.jdbc.url=jdbc:d42://localhost/", "device42.doql.jdbc.username=test",
                "device42.doql.jdbc.password=test",
                "spring.datasource.url=jdbc:h2:mem:target-selection", "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.hikari.minimum-idle=0", "spring.datasource.hikari.maximum-pool-size=1");
    }

    @Test
    void defaultsToMaximoWithEveryExistingCommand() {
        maximo().run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MaximoTargetModule.class).hasSingleBean(DataSource.class);
            assertThat(context.getBean(TargetModule.class).jobs()).containsOnlyKeys(
                    "asset", "ci", "ci-relation", "conversion", "software");
            assertThat(context).doesNotHaveBean(AlternativeTarget.class);
            context.getBean(JobRunner.class).run();
        });
    }

    @Test
    void explicitlySelectsMaximo() {
        maximo().withPropertyValues("integration.target=maximo").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(MaximoTargetModule.class);
        });
    }

    @Test
    void rejectsUnknownTargetBeforeRunningAnyJobsOrCreatingTargetConnections() {
        runner.withPropertyValues("integration.target=missing").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class)
                    .hasStackTraceContaining("target=missing");
        });
    }

    @Test
    void switchesToTestOnlyTargetWithoutMaximoCredentialsBeansOrDataSource() {
        runner.withPropertyValues("integration.target=alternative").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(AlternativeTarget.class)
                    .doesNotHaveBean(MaximoTargetModule.class).doesNotHaveBean(ActCiWriter.class)
                    .doesNotHaveBean(DataSource.class);
            var target = context.getBean(AlternativeTarget.class);
            assertThat(context.getBean(TargetModule.class).jobs()).containsOnlyKeys("inventory");
            context.getBean(JobRunner.class).run("unknown", "inventory", "asset", "inventory");
            assertThat(target.received).containsExactly(new CpuInventory(7, "Xeon"), new CpuInventory(7, "Xeon"));
            verify(target.doql, times(2)).preparedQuery(anyString(), any());
            verify(target.doql, times(2)).query(anyString(), any());
        });
    }

}
