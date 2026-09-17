package com.itmsg.device42.runtime;

/** 실행별 값은 action의 지역 캡처로 전달한다. 공유 실행 컨텍스트는 두지 않는다. */
public record NamedTask(String name, Runnable action) {
}
