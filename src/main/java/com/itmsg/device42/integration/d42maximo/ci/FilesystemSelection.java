package com.itmsg.device42.integration.d42maximo.ci;

import java.util.List;
import java.util.stream.Collectors;

public final class FilesystemSelection {
    private FilesystemSelection() {}

    /**
     * 적재하지 않는 파일시스템 종류. 컨테이너 런타임·이미지 마운트다.
     * 경로에 컨테이너 ID가 들어가 재기동 시 원천 PK가 바뀌면 매 실행마다 새 CI가 쌓인다.
     * devtmpfs는 경로가 고정돼 이 문제가 없어 수집 대상이다.
     */
    public static final List<String> EXCLUDED_TYPES = List.of("overlay", "squashfs", "efivarfs");

    /** EXCLUDED_TYPES를 DOQL IN 절에 넣을 수 있게 join한 문자열. CiRelationSource도 재사용한다. */
    public static final String EXCLUDED_TYPES_SQL = EXCLUDED_TYPES.stream()
            .map(type -> "'" + type + "'").collect(Collectors.joining(", "));
}
