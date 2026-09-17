package com.itmsg.device42.runtime;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** COUNT 기준 페이지 범위. 빈 조회 결과도 다음 offset 진행을 바꾸지 않는다. */
public final class PageLoop {
    private PageLoop() {
    }

    public record Page(long offset, int limit) {
    }

    public static Iterable<Page> pages(long totalCount, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
        return () -> new Iterator<>() {
            private long offset;

            @Override
            public boolean hasNext() {
                return offset < totalCount;
            }

            @Override
            public Page next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Page page = new Page(offset, (int) Math.min(batchSize, totalCount - offset));
                offset += batchSize;
                return page;
            }
        };
    }
}
