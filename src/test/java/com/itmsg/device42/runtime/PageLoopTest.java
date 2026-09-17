package com.itmsg.device42.runtime;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageLoopTest {
    @Test
    void preservesCountBasedOffsetsAndFinalLimit() {
        List<PageLoop.Page> pages = new ArrayList<>();
        PageLoop.pages(2001, 1000).forEach(pages::add);
        assertThat(pages).containsExactly(
                new PageLoop.Page(0, 1000), new PageLoop.Page(1000, 1000), new PageLoop.Page(2000, 1));
        assertThat(PageLoop.pages(0, 1000)).isEmpty();
        assertThat(PageLoop.pages(-1, 1000)).isEmpty();
        assertThat(PageLoop.pages(1000, 1000)).containsExactly(new PageLoop.Page(0, 1000));
    }

    @Test
    void iterationsHaveIndependentStateAndLongOffsets() {
        var pages = PageLoop.pages(Integer.MAX_VALUE + 1L, Integer.MAX_VALUE);
        assertThat(pages).containsExactly(new PageLoop.Page(0, Integer.MAX_VALUE),
                new PageLoop.Page(Integer.MAX_VALUE, 1));
        assertThat(pages).containsExactly(new PageLoop.Page(0, Integer.MAX_VALUE),
                new PageLoop.Page(Integer.MAX_VALUE, 1));
    }
}
