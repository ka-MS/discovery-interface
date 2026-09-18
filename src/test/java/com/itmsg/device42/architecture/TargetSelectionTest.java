package com.itmsg.device42.architecture;

import com.itmsg.device42.DiscoveryInterfaceApplication;
import com.itmsg.device42.cli.JobRunner;
import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.TargetModule;
import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.source.device42.asset.cpu.CpuQuery;
import com.itmsg.device42.source.device42.asset.cpu.ProcessorSource;
import com.itmsg.device42.source.device42.selection.DeviceSelection;
import com.itmsg.device42.pipeline.d42maximo.MaximoTargetModule;
import com.itmsg.device42.target.maximo.ci.ActCiWriter;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class TargetSelectionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DiscoveryInterfaceApplication.class, AlternativeTarget.class);

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

    record CpuInventory(long id, String model) {}

    /** 프로덕션 코어/Maximo 변경 없이 추가한 조립부. 원천 Query와 JDBC 행 변환도 실제로 거친다. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "integration.target", havingValue = "alternative")
    static class AlternativeTarget implements TargetModule {
        final List<CpuInventory> received = new ArrayList<>();
        final DoqlClient doql = mock(DoqlClient.class);
        final CpuQuery query;

        AlternativeTarget() throws Exception {
            query = new CpuQuery(DeviceSelection.all(), doql);
            when(doql.preparedQuery(anyString(), any())).thenAnswer(invocation -> {
                var rs = mock(ResultSet.class);
                when(rs.next()).thenReturn(true, false);
                when(rs.getLong(1)).thenReturn(1L);
                return invocation.getArgument(1, DoqlClient.ResultReader.class).read(rs);
            });
            when(doql.query(anyString(), any())).thenAnswer(invocation -> {
                assertThat(invocation.getArgument(0, String.class)).doesNotContain("Network Printer", "PDU", "{{")
                        .contains("TRUE");
                var rs = mock(ResultSet.class);
                when(rs.next()).thenReturn(true, false);
                when(rs.getLong("part_pk")).thenReturn(7L);
                when(rs.getString("model_name")).thenReturn("Xeon");
                when(rs.getBigDecimal("cores")).thenReturn(BigDecimal.ONE);
                return invocation.getArgument(1, DoqlClient.ResultReader.class).read(rs);
            });
        }

        public String id() { return "alternative"; }

        public Map<String, IntegrationJob> jobs() {
            return Map.of("inventory", () -> {
                if (query.getTotalCount() > 0) {
                    for (ProcessorSource row : query.getData(0, 10)) {
                        received.add(new CpuInventory(row.partPk(), row.modelName()));
                    }
                }
            });
        }
    }
}
