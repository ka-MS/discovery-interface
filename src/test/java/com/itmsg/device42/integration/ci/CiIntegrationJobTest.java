package com.itmsg.device42.integration.ci;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CiIntegrationJobTest {
    @Test
    void loadsOncePerRunSharesSnapshotAndContinuesAfterTaskFailure() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache first = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        CiDefinitionCache second = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load()).thenReturn(first, second);
        CiIntegrationTask computer = mock(CiIntegrationTask.class);
        CiIntegrationTask next = mock(CiIntegrationTask.class);
        doThrow(new IllegalStateException("source failure")).when(computer).integrate(first);
        CiIntegrationJob job = new CiIntegrationJob(List.of(computer, next), loader);

        job.run();
        job.run();

        var order = inOrder(loader, computer, next);
        order.verify(loader).load();
        order.verify(computer).integrate(first);
        order.verify(next).integrate(first);
        order.verify(loader).load();
        order.verify(computer).integrate(second);
        order.verify(next).integrate(second);
        order.verifyNoMoreInteractions();
    }

    @Test
    void failedReloadDoesNotReusePreviousSnapshot() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache first = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load()).thenReturn(first).thenThrow(new IllegalStateException("definitions unavailable"));
        CiIntegrationTask computer = mock(CiIntegrationTask.class);
        CiIntegrationJob job = new CiIntegrationJob(List.of(computer), loader);
        job.run();

        assertThatThrownBy(job::run).hasMessageContaining("definitions unavailable");
        verify(computer, times(1)).integrate(first);
        verifyNoMoreInteractions(computer);
    }
}
