package com.itmsg.device42.runtime;

import java.util.List;
import org.slf4j.Logger;

/** 기존 Job의 순차 실행과 작업별 Exception 격리만 담당한다. */
public final class TaskSequence {
    private TaskSequence() {}

    public static void run(Logger log, List<NamedTask> tasks) {
        for (NamedTask task : tasks) {
            try {
                log.info("{} 작업을 시작합니다.", task.name());
                task.action().run();
                log.info("{} 작업이 완료되었습니다.", task.name());
            } catch (Exception e) {
                log.error("{} 작업에 실패했습니다. 다음 작업을 계속합니다.", task.name(), e);
            }
        }
    }
}
