package com.itmsg.device42.integration.d42maximo.asset.cpu;

import com.itmsg.device42.maximo.asset.DpaCpuWriter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CpuImportFlowTest {
    @Test
    void emptyPagesStillAdvanceUsingSourceCount() {
        var query = mock(CpuQuery.class);
        var mapper = mock(CpuMapper.class);
        var writer = mock(DpaCpuWriter.class);
        when(query.getTotalCount()).thenReturn(1001L);
        when(query.getData(anyLong(), anyInt())).thenReturn(List.of());
        when(mapper.mapData(List.of())).thenReturn(List.of());

        new CpuImport(query, mapper, writer).integrate();

        var order = inOrder(query, mapper, writer);
        order.verify(query).getTotalCount();
        order.verify(query).getData(0, 1000);
        order.verify(mapper).mapData(List.of());
        order.verify(writer).write(List.of());
        order.verify(query).getData(1000, 1);
        order.verify(mapper).mapData(List.of());
        order.verify(writer).write(List.of());
        order.verifyNoMoreInteractions();
    }

    @Test
    void queryFailureAbortsCurrentImportWithoutBeingTurnedIntoAnEmptyPage() {
        var query = mock(CpuQuery.class);
        var mapper = mock(CpuMapper.class);
        var writer = mock(DpaCpuWriter.class);
        when(query.getTotalCount()).thenReturn(1001L);
        when(query.getData(0, 1000)).thenThrow(new IllegalStateException("source unavailable"));

        assertThatThrownBy(() -> new CpuImport(query, mapper, writer).integrate())
                .hasMessage("source unavailable");
        verify(query, never()).getData(1000, 1);
        verifyNoInteractions(mapper, writer);
    }
}
