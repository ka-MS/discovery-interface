package com.itmsg.device42.integration.d42maximo.ci.os;

/** 장비에 설치된 OS 한 건. 발견 시각은 부모 Computer에서 가져온다. */
public record OsSource(
        long deviceOsPk, long devicePk, String osName, String osVersion,
        String kernelVersion, String architecture, String lastDiscovered
) {
}
