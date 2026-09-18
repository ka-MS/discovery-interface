package com.itmsg.device42.cli;

import com.itmsg.device42.runtime.IntegrationJob;
import com.itmsg.device42.runtime.TargetModule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JobRunnerTest {
    private static JobRunner runner(Map<String, IntegrationJob> jobs) {
        return new JobRunner("maximo", List.of(new TargetModule() {
            public String id() { return "maximo"; }
            public Map<String, IntegrationJob> jobs() { return jobs; }
        }));
    }

    @Test
    void preservesArgumentOrderRepeatedCommandsAndUnknownCommandHandling() throws Exception {
        var asset = mock(IntegrationJob.class);
        var relations = mock(IntegrationJob.class);
        var runner = runner(Map.of("asset", asset, "ci-relation", relations));
        runner.run();
        verifyNoInteractions(asset, relations);
        runner.run("unknown", "ci-relation", "asset", "asset", "ASSET");
        var order = inOrder(asset, relations);
        order.verify(relations).run();
        order.verify(asset, times(2)).run();
        order.verifyNoMoreInteractions();
    }

    @Test
    void jobPreparationFailureStillStopsLaterCommands() {
        var ci = mock(IntegrationJob.class);
        var asset = mock(IntegrationJob.class);
        doThrow(new IllegalStateException("definitions unavailable")).when(ci).run();
        assertThatThrownBy(() -> runner(Map.of("ci", ci, "asset", asset)).run("ci", "asset"))
                .hasMessage("definitions unavailable");
        verifyNoInteractions(asset);
    }
}
