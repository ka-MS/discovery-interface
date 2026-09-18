package com.itmsg.device42.pipeline.d42maximo.ci;

import com.itmsg.device42.pipeline.d42maximo.ci.os.OsCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.ip.IpCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.filesystem.FilesystemCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.disk.DiskCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.device.DeviceCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.databaseinstance.DatabaseInstanceCiImport;
import com.itmsg.device42.pipeline.d42maximo.ci.mapping.CiClassification;
import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionCache;

import com.itmsg.device42.target.maximo.ci.definition.CiDefinitionLoader;

import com.itmsg.device42.pipeline.d42maximo.ci.relation.CiRelationJob;
import org.junit.jupiter.api.Test;

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
        when(loader.load(CiClassification.ids())).thenReturn(first, second);
        DeviceCiImport computer = mock(DeviceCiImport.class);
        DiskCiImport next = mock(DiskCiImport.class);
        doThrow(new IllegalStateException("source failure")).when(computer).integrate(first);
        CiIntegrationJob job = job(computer, next, loader, mock(CiRelationJob.class));

        job.run();
        job.run();

        var order = inOrder(loader, computer, next);
        order.verify(loader).load(CiClassification.ids());
        order.verify(computer).integrate(first);
        order.verify(next).integrate(first);
        order.verify(loader).load(CiClassification.ids());
        order.verify(computer).integrate(second);
        order.verify(next).integrate(second);
        order.verifyNoMoreInteractions();
    }

    @Test
    void failedReloadDoesNotReusePreviousSnapshot() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache first = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load(CiClassification.ids())).thenReturn(first).thenThrow(new IllegalStateException("definitions unavailable"));
        DeviceCiImport computer = mock(DeviceCiImport.class);
        CiIntegrationJob job = job(computer, mock(DiskCiImport.class), loader, mock(CiRelationJob.class));
        job.run();

        assertThatThrownBy(job::run).hasMessageContaining("definitions unavailable");
        verify(computer, times(1)).integrate(first);
        verifyNoMoreInteractions(computer);
    }

    @Test
    void runsRelationStageAfterEveryBodyTask() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache definitions = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load(CiClassification.ids())).thenReturn(definitions);
        DeviceCiImport computer = mock(DeviceCiImport.class);
        DiskCiImport os = mock(DiskCiImport.class);
        CiRelationJob relations = mock(CiRelationJob.class);

        job(computer, os, loader, relations).run();

        var order = inOrder(computer, os, relations);
        order.verify(computer).integrate(definitions);
        order.verify(os).integrate(definitions);
        order.verify(relations).run();
    }

    @Test
    void runsRelationStageEvenAfterBodyTaskFailure() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache definitions = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load(CiClassification.ids())).thenReturn(definitions);
        DeviceCiImport failing = mock(DeviceCiImport.class);
        doThrow(new IllegalStateException("source failure")).when(failing).integrate(definitions);
        CiRelationJob relations = mock(CiRelationJob.class);

        job(failing, mock(DiskCiImport.class), loader, relations).run();

        verify(relations).run();
    }
    private CiIntegrationJob job(DeviceCiImport device, DiskCiImport disk,
                                 CiDefinitionLoader loader, CiRelationJob relations) {
        return new CiIntegrationJob(mock(DatabaseInstanceCiImport.class), device, disk,
                mock(FilesystemCiImport.class), mock(IpCiImport.class), mock(OsCiImport.class),
                loader, relations);
    }
}
