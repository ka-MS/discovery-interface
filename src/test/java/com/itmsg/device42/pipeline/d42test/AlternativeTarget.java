package com.itmsg.device42.pipeline.d42test;

import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.TargetModule;
import com.itmsg.device42.source.device42.DoqlClient;
import com.itmsg.device42.source.device42.asset.cpu.CpuQuery;
import com.itmsg.device42.source.device42.asset.cpu.ProcessorSource;
import com.itmsg.device42.source.device42.selection.DeviceSelection;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** 프로덕션 코어/Maximo 변경 없이 추가한 조립부. 원천 Query와 JDBC 행 변환도 실제로 거친다. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "integration.target", havingValue = "alternative")
public class AlternativeTarget implements TargetModule {
    public final List<CpuInventory> received = new ArrayList<>();
    public final DoqlClient doql = mock(DoqlClient.class);
    final CpuQuery query;

    public record CpuInventory(long id, String model) {}

    public AlternativeTarget() throws Exception {
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
