package com.itmsg.device42.integration.d42maximo.ci.databaseinstance;

/**
 * Device42 DB Instance 한 건. 식별자·버전·설명은 같은 PK의 Resource에서,
 * 설치 경로는 연결된 Application Component에서 가져온다.
 * Resource의 last_discovered는 전건 NULL이라 스캔 시각은 last_changed를 쓴다.
 */
public record DatabaseInstanceSource(
        long databaseInstancePk, String name, String engine, String identifier,
        String versionText, String description, String installPath, String lastChanged
) {
}
