package com.itmsg.device42.runtime;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import static org.assertj.core.api.Assertions.*;

class TaskSequenceTest {
    @Test
    void preservesOrderAndContinuesAfterExceptionOnEveryRun() {
        var calls = new ArrayList<String>();
        var tasks = List.of(
                new NamedTask("first", () -> { calls.add("first"); throw new IllegalStateException(); }),
                new NamedTask("next", () -> calls.add("next")));
        TaskSequence.run(LoggerFactory.getLogger(getClass()), tasks);
        TaskSequence.run(LoggerFactory.getLogger(getClass()), tasks);
        assertThat(calls).containsExactly("first", "next", "first", "next");
    }

    @Test
    void doesNotBroadenTheExistingExceptionBoundaryToErrors() {
        var calls = new ArrayList<String>();
        assertThatThrownBy(() -> TaskSequence.run(LoggerFactory.getLogger(getClass()), List.of(
                new NamedTask("error", () -> { throw new AssertionError("fatal"); }),
                new NamedTask("next", () -> calls.add("next")))))
                .isInstanceOf(AssertionError.class).hasMessage("fatal");
        assertThat(calls).isEmpty();
    }
}
