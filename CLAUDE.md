# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

**Device42**에서 IT 자산 발견(discovery) 데이터를 조회해 DB2 상의 **IBM Maximo** ITAM 테이블로 적재하는 배치 ETL CLI다. 웹 서버가 아니다. Spring Boot 4.1.1, Java 25, Gradle.

## 데이터 분석 문서

원천·타겟 구조와 매핑은 `docs/data-analysis/` 에 있다. 매핑 관련 작업을 시작하기 전에 [docs/data-analysis/README.md](docs/data-analysis/README.md) 를 먼저 읽는다. 세 계층으로 나뉜다.

- `knowledge/` — 관측 사실. 수치는 스냅샷이며 상단에 관측 시점과 재조회 쿼리를 명시한다.
- `data-mapping/` — 테이블 단위 매핑 정본. 문서 한 장이 구현 클래스 하나에 대응한다.
- `exploration-queries/` — 재사용 조회 쿼리.

미결·정책 대기 항목의 정본은 `docs/data-analysis/open-issues.md` 하나다. 다른 문서는 이슈 ID와 한 줄 요약만 참조한다.

**Device42 는 두 대다.** `192.168.2.68` 은 소프트웨어·파트·마운트가, `192.168.1.35` 는 네트워크·OS 가 넓다. 같은 뷰라도 건수가 크게 다르므로 한 대만 보고 결론을 내지 않는다. 조사 결과를 문서에 옮길 때 어느 서버 관측인지 함께 적는다. 전환 방법은 `docs/data-analysis/knowledge/device42/servers.md` 에 있다.

## 명령어

```bash
./gradlew build          # 컴파일 + 테스트
./gradlew test           # 테스트만
./gradlew test --tests 'DiscoveryInterfaceApplicationTests.contextLoads'   # 단일 테스트
./gradlew bootRun --args='asset'                                          # 잡 실행
java -jar build/libs/discovery-interface-0.0.1-SNAPSHOT.jar asset ci      # 여러 개를 인자 순서대로 실행
```

CLI 인자로 넘기는 잡 이름은 Spring 빈 이름이다: `asset`, `ci`, `software`. 등록되지 않은 인자는 아무 경고 없이 무시된다.

## 로컬 사전 준비물 (git 미추적, 디스크에 반드시 존재해야 함)

`src/main/resources/application.yaml`, `libs/d42-jdbc-driver-1.0.2-jar-with-dependencies.jar`, `config/` 는 모두 git에 추적되지 않지만 반드시 필요하다. 벤더 D42 JDBC jar가 없으면 빌드가 실패하고, 애플리케이션에는 설정 폴백이 없다.

`config/d42-truststore.p12` 는 Device42 REST TLS 연결에 쓰인다. `config/device42-test-hosts` 는 REST 호스트명 `Device42Demo` 를 Device42 IP로 매핑하며, `build.gradle` 의 `jdk.net.hosts.file` 시스템 프로퍼티로 테스트에 주입된다. truststore 인증서가 해당 호스트명으로 발급돼 있어서, 실제 DNS 항목 없이도 이 hosts 파일 덕분에 TLS 검증이 통과한다.

`local/db-access-kit/` 은 원천 조사용 DB 접속 패키지다. 실행기·인증서·실제 자격정보가 들어 있고 git에 추적되지 않는다. 폴더 내용을 응답, 로그, 커밋에 옮기지 않는다. 실행 규칙은 `local/db-access-kit/AGENTS.md` 를 따른다. 조회 쿼리는 `docs/data-analysis/exploration-queries/` 에 있고, 실행 결과는 `local/db-access-kit/work/` 아래에만 둔다.

## 아키텍처

DB가 두 개이고, 의도적으로 서로 다르게 구성돼 있다.

- **Maximo (DB2)** — 표준 Spring `spring.datasource` 를 사용하고 `maximoJdbcTemplate` 로 노출된다 ([MaximoDatabaseConfig.java](src/main/java/com/itmsg/device42/integration/config/MaximoDatabaseConfig.java)). Hikari 풀은 커넥션 1개로 제한돼 있다.
- **Device42 (DOQL)** — 의도적으로 Spring `DataSource` 가 *아니다*. [Device42ConnectionFactory](src/main/java/com/itmsg/device42/integration/config/Device42ConnectionFactory.java) 가 쿼리마다 raw `DriverManager` 커넥션을 연다. 드라이버 클래스는 빈 생성 시점에 리플렉션으로 로드된다. Device42 DOQL 엔드포인트가 커넥션 풀링을 견디지 못하기 때문에, 모든 조회가 자기 커넥션을 열고 닫는다.

잡 디스패치: [JobRunner](src/main/java/com/itmsg/device42/integration/JobRunner.java) 는 `CommandLineRunner` 로, `Map<String, IntegrationJob>` 을 주입받아(Spring이 모든 `IntegrationJob` 빈을 이름을 키로 주입) `args` 에 지정된 잡만 실행한다. **잡 추가 = `IntegrationJob` 구현체에 `@Component("<이름>")` 붙이기.** 별도로 갱신할 등록 목록이 없다.

`asset` 잡 내부에서는 [AssetIntegrationJob](src/main/java/com/itmsg/device42/integration/asset/AssetIntegrationJob.java) 이 `List<AssetIntegrationTask>` 를 `@Order` 순서대로 실행하며, **태스크가 실패해도 다음 태스크를 계속 진행한다**. 이 순서는 실제 데이터 의존성이다. `DeployedAssetIntegrate`(1)가 `MAXIMO.DEPLOYEDASSET` 을 먼저 채워야 `DpaComputerIntegrate`(2)가 `(SOURCEID, IMPORTSOURCE)` 로 `NODEID` 를 조회할 수 있다. 교차키가 없는 행은 로그만 남기고 건너뛴다(insert하지 않는다).

### 모든 태스크가 따르는 ETL 형태

`getTotalCount()` → `DEFAULT_BATCH_SIZE` 단위로 `getData(offset, limit)` 루프 → `mapData()` (Device42 레코드 → Maximo 레코드) → `putData()` (DB2 `MERGE`). 새 태스크도 이 형태를 유지할 것.

- 페이징은 상수 쿼리 문자열에 `LIMIT %d OFFSET %d` 를 이어 붙이는 방식이다. 원천 SQL은 각 클래스 하단의 `private static final String` 텍스트 블록에 모여 있고, 공통 `DEVICE_FILTER` 를 조합해 만들기 때문에 건수 쿼리와 데이터 쿼리의 조건이 어긋날 수 없다.
- 쓰기는 `SOURCEID` + `IMPORTSOURCE`(항상 `"Device42"`)를 키로 하는 `MERGE INTO MAXIMO.<TABLE>` 이라, 재실행해도 멱등하다.
- `putData` 는 `SQLException` 을 **행 단위로** 잡아 로그를 남기고 계속 진행한다. 잘못된 행 하나가 배치 전체를 중단시켜서는 안 된다.

조회 실패는 `IllegalStateException` 을 던지고, 쓰기 실패는 로그만 남긴다. 이 비대칭은 의도적이다. 원천 쿼리가 깨지면 배치 자체가 무의미하지만, 행 하나가 깨진 건 그렇지 않다.

### 컨벤션

- 로그 메시지와 예외 메시지는 **한글**로 작성한다. 식별자, SQL, 주석은 영어다. 이 규칙을 따를 것.
- DTO는 모두 `record` 다. `dto/device42/*` 는 Device42 원천 컬럼을, `dto/maximo/*` 는 Maximo 테이블 전체 형태를 반영한다(상당수 필드는 `null` 로 남는다 — MERGE 문에 들어가는 부분집합만 실제로 기록된다).
- JDBC null은 명시적으로 처리해야 한다. `getNullableLocalDateTime` / `getNullableInteger` 헬퍼 참고. nullable 숫자 컬럼에서 `ResultSet.getInt` 가 NULL을 `0` 으로 돌려주는 문제를 피하기 위한 것이다.
- **원천 pk에 의존하는 코드나 문서를 만들지 않는다.** Device42 `device_pk` 는 수집 서버가 다르거나 재수집하면 값이 바뀐다. 동일 장비가 서버에 따라 다른 pk를 갖는 것이 실측으로 확인됐다. 대상 지정은 조건식이나 `uuid`/`serial_no` 같은 안정적인 값으로 한다.

### 미완성 영역

`DpaOsIntegrate`, `ActCiIntegrate`, `DpaSoftwareIntegrate` 는 본문이 비어 있거나 하드코딩된 스텁이다. `dto/device42/Device.java`, `Device1.java`, `ViewDeviceV2.java` 는 Device42 스키마 컬럼을 그대로 덤프한 미사용 코드다. 새 DOQL 쿼리를 작성할 때 컬럼 참고용으로는 쓸 만하지만, 살아 있는 코드가 아니다.

`MAXIMO.DEPLOYEDASSET` 에는 부모 행만 적재되고 자식 `DPA*` 테이블은 아직 채워지지 않는다. Device42 적재분에 자식 행이 0건인 것이 실측으로 확인됐다.

Device42→Maximo 필드 매핑의 기준 문서는 `docs/data-analysis/data-mapping/` 이다. `mapData` 로직을 바꾸기 전에 해당 테이블 문서를 확인할 것. `local/데이터매핑표v14.xlsx` 는 이 문서 체계 이전의 자료이며 참고용으로만 남아 있다.
