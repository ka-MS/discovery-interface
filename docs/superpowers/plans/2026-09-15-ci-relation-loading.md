# CI 관계 적재 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Device42의 확인된 연결 키로 `MAXIMO.ACTCIRELATION`에 CI 관계를 적재한다. OS→Computer로 관통한 뒤 Computer→Disk·Filesystem을 상수 추가만으로 확장한다.

**Architecture:** 관계는 CI 본체 적재가 모두 끝난 뒤 별도 단계에서 실행한다. 관계 정의는 `CiRelationSource` enum 상수 하나(=relationnum + 건수 SQL + 페이지 SQL)이고, 조회 페이징·DTO 변환·저장 호출·집계는 `CiRelationJob` 한 곳에 있다. 저장은 `ActCiRelationWriter`의 MERGE 한 문장이 맡으며, 양 끝 ACTCI 존재와 실제 분류쌍의 `RELATIONRULES` 규칙을 SQL 안에서 확인한다.

**Tech Stack:** Java 25, Spring Boot 4.1.1, Gradle, JdbcTemplate(Db2 BLUDB), Device42 DOQL over JDBC, JUnit 5 + AssertJ + Mockito, H2 2.4.240 (MODE=DB2) 인메모리 테스트.

**Spec:**
- [관계 설계](../../data-analysis/design/ci/relations.md) — 실행 위치·구조·정의 계약·세 관계 대조
- [ACTCIRELATION 공통 매핑](../../data-analysis/data-mapping/ci/actcirelation.md) — 컬럼 매핑·MERGE 정본
- [OS 관계 매핑](../../data-analysis/data-mapping/ci/types/os.md#6-관계-매핑--2026-09-15)
- [Device 관계 매핑](../../data-analysis/data-mapping/ci/types/device.md#7-관계-매핑--2026-09-15)
- [OS–Computer 승격 샘플 검증](../../data-analysis/knowledge/maximo/computer-ci-relations.md#oscomputer-승격-샘플-검증)

## Global Constraints

- 커밋 메시지는 `<유형>: <짧은 설명>`. 접두어는 영문, 제목·본문은 한글. 커밋 하나에 목적 하나. 끝에 `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.
- MERGE 키는 `(SOURCECI, TARGETCI, RELATIONNUM)`. 실제 고유 인덱스와 같다.
- `SOURCECI`/`TARGETCI`는 `ACTCI.ACTCINUM` 문자열이다. `ACTCIID`나 원천 PK가 아니다.
- `SWAPPED`는 상수 `0`. 규칙의 `SWAPPED`를 관계 행에 복사하지 않는다.
- `CHANGEBY`는 상수 `"Device42"`.
- `ANCESTORCI`, `BASELINEDATE`, `SOURCECIGUID`, `TARGETCIGUID`는 신규 행에서 넣지 않는다. 기존 행의 값을 NULL로 덮어쓰지 않는다(MERGE의 UPDATE 절에 없다).
- 예상 분류를 파라미터로 넘기지 않는다. 규칙 확인은 조인된 `ACTCI` 행의 실제 `CLASSSTRUCTUREID`로 한다.
- MERGE 파라미터는 6개, 순서는 `sourceCiNum, targetCiNum, relationNum, swapped, changeBy, changeDate`.
- DOQL은 `FROM` 서브쿼리를 막는다. `WITH`는 된다. 건수 SQL과 페이지 SQL을 따로 쓴다.
- DOQL 페이지 SQL은 `ORDER BY sourceci, targetci`를 반드시 포함한다. 정렬 없는 `OFFSET`은 순서를 보장하지 않는다.
- 배치 크기는 기존 CI 작업과 같은 `1000`.
- 건별 오류 격리: 관계 하나의 실패가 나머지를 막지 않는다. 관계 유형 하나의 실패가 다른 유형을 막지 않는다.
- `CiRelationJob`은 `CiDefinitionCache`를 받지 않는다. `ci-relation` 단독 실행이 본체 기준정보 로딩에 의존하면 안 된다.
- 로그 메시지는 기존 CI 작업과 같은 한국어 문체를 쓴다.
- 본체 task 일부가 실패해도 관계 단계는 진행한다. 저장된 CI의 존재·분류·규칙만 본다.

---

### Task 1: ActCiRelationWriter와 DTO

공통 MERGE를 구현한다. 이 태스크가 끝나면 관계 한 건을 멱등하게 저장할 수 있고, 양 끝이 없거나 규칙이 없으면 건너뛴다.

**Files:**
- Create: `src/main/java/com/itmsg/device42/dto/maximo/ci/ActCiRelationUpsert.java`
- Create: `src/main/java/com/itmsg/device42/integration/ci/relation/ActCiRelationWriter.java`
- Modify: `src/test/resources/ci/schema.sql` (파일 끝에 추가)
- Test: `src/test/java/com/itmsg/device42/integration/ci/relation/ActCiRelationWriterTest.java`

**Interfaces:**
- Consumes: `@Qualifier("maximoJdbcTemplate") JdbcTemplate` (기존 빈)
- Produces:
  - `record ActCiRelationUpsert(String sourceCiNum, String targetCiNum, String relationNum)`
  - `ActCiRelationWriter(JdbcTemplate maximoJdbcTemplate)`
  - `int ActCiRelationWriter.write(List<ActCiRelationUpsert> data)` — 적재 성공 건수 반환

- [ ] **Step 1: 테스트 스키마에 관계 테이블 추가**

`src/test/resources/ci/schema.sql` 끝에 이어 붙인다. 기존 내용은 건드리지 않는다.

```sql
CREATE SEQUENCE MAXIMO.ACTCIRELATIONSEQ START WITH 3001 INCREMENT BY 1000;
CREATE TABLE MAXIMO.RELATION (RELATIONNUM VARCHAR(192) PRIMARY KEY);
CREATE TABLE MAXIMO.RELATIONRULES (
    RELATIONNUM VARCHAR(192), SOURCECLASS VARCHAR(25), TARGETCLASS VARCHAR(25)
);
CREATE TABLE MAXIMO.ACTCIRELATION (
    ACTCIRELATIONID BIGINT PRIMARY KEY,
    SOURCECI VARCHAR(150) NOT NULL, TARGETCI VARCHAR(150) NOT NULL,
    RELATIONNUM VARCHAR(192) NOT NULL,
    SWAPPED INTEGER, CHANGEBY VARCHAR(100), CHANGEDATE TIMESTAMP,
    ANCESTORCI VARCHAR(150), BASELINEDATE TIMESTAMP,
    SOURCECIGUID VARCHAR(192), TARGETCIGUID VARCHAR(192),
    UNIQUE (SOURCECI, TARGETCI, RELATIONNUM)
);
```

- [ ] **Step 2: 실패하는 테스트를 작성한다**

`src/test/java/com/itmsg/device42/integration/ci/relation/ActCiRelationWriterTest.java`

```java
package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActCiRelationWriterTest {
    private static final String INSTALLED_ON = "RELATION.INSTALLEDON";

    private JdbcTemplate jdbc;
    private ActCiRelationWriter writer;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=DB2;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("ci/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        writer = new ActCiRelationWriter(jdbc);

        jdbc.update("INSERT INTO MAXIMO.RELATION VALUES (?)", INSTALLED_ON);
        actCi("D42:DEVICEOS:147", "OS1");
        actCi("D42:DEVICEOS:148", "OS1");
        actCi("D42:DEVICE:173", "CS1");
        actCi("D42:DEVICE:174", "VCS1");
        rule(INSTALLED_ON, "OS1", "CS1");
        rule(INSTALLED_ON, "OS1", "VCS1");
    }

    private void actCi(String actCiNum, String classStructureId) {
        jdbc.update("""
                INSERT INTO MAXIMO.ACTCI
                    (ACTCIID,ACTCINUM,ACTCINAME,CLASSSTRUCTUREID,LASTSCANDT,LANGCODE,HASLD)
                VALUES (NEXT VALUE FOR MAXIMO.ACTCISEQ,?,?,?,CURRENT_TIMESTAMP,'KO',0)
                """, actCiNum, actCiNum, classStructureId);
    }

    private void rule(String relationNum, String sourceClass, String targetClass) {
        jdbc.update("INSERT INTO MAXIMO.RELATIONRULES VALUES (?,?,?)",
                relationNum, sourceClass, targetClass);
    }

    private static ActCiRelationUpsert relation(String source, String target) {
        return new ActCiRelationUpsert(source, target, INSTALLED_ON);
    }

    @Test
    void insertsRelationWithConstantSwappedAndChangeBy() {
        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SWAPPED FROM MAXIMO.ACTCIRELATION", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT CHANGEBY FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("Device42");
        assertThat(jdbc.queryForObject("SELECT CHANGEDATE FROM MAXIMO.ACTCIRELATION", Object.class)).isNotNull();
    }

    @Test
    void rerunKeepsSingleRowAndSameId() {
        writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));
        Long firstId = jdbc.queryForObject("SELECT ACTCIRELATIONID FROM MAXIMO.ACTCIRELATION", Long.class);

        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCIRELATION", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT ACTCIRELATIONID FROM MAXIMO.ACTCIRELATION", Long.class))
                .isEqualTo(firstId);
    }

    @Test
    void acceptsPhysicalAndVirtualComputerWithoutCallerBranching() {
        int loaded = writer.write(List.of(
                relation("D42:DEVICEOS:147", "D42:DEVICE:173"),
                relation("D42:DEVICEOS:148", "D42:DEVICE:174")));

        assertThat(loaded).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT TARGETCI FROM MAXIMO.ACTCIRELATION ORDER BY TARGETCI", String.class))
                .containsExactly("D42:DEVICE:173", "D42:DEVICE:174");
    }

    @Test
    void skipsMissingEndAndKeepsProcessingRest() {
        int loaded = writer.write(List.of(
                relation("D42:DEVICEOS:999", "D42:DEVICE:173"),
                relation("D42:DEVICEOS:147", "D42:DEVICE:999"),
                relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SOURCECI FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("D42:DEVICEOS:147");
    }

    @Test
    void skipsWhenNoRuleForActualClassPair() {
        jdbc.update("DELETE FROM MAXIMO.RELATIONRULES WHERE TARGETCLASS='CS1'");

        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM MAXIMO.ACTCIRELATION", Integer.class)).isZero();
    }

    @Test
    void skipsWhenRelationCodeIsNotRegistered() {
        jdbc.update("DELETE FROM MAXIMO.RELATION");

        int loaded = writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(loaded).isZero();
    }

    @Test
    void keepsExistingGuidAndAncestorOnUpdate() {
        writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));
        jdbc.update("UPDATE MAXIMO.ACTCIRELATION SET SOURCECIGUID='urn:uuid:kept', ANCESTORCI='D42:DEVICE:173'");

        writer.write(List.of(relation("D42:DEVICEOS:147", "D42:DEVICE:173")));

        assertThat(jdbc.queryForObject("SELECT SOURCECIGUID FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("urn:uuid:kept");
        assertThat(jdbc.queryForObject("SELECT ANCESTORCI FROM MAXIMO.ACTCIRELATION", String.class))
                .isEqualTo("D42:DEVICE:173");
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.relation.ActCiRelationWriterTest"
```

Expected: 컴파일 실패. `ActCiRelationUpsert`와 `ActCiRelationWriter`가 없다.

- [ ] **Step 4: DTO를 작성한다**

`src/main/java/com/itmsg/device42/dto/maximo/ci/ActCiRelationUpsert.java`

```java
package com.itmsg.device42.dto.maximo.ci;

/** 관계 한 쌍. SWAPPED·CHANGEBY·CHANGEDATE는 Writer가 상수·실행 시각으로 채운다. */
public record ActCiRelationUpsert(String sourceCiNum, String targetCiNum, String relationNum) {
}
```

- [ ] **Step 5: Writer를 작성한다**

`src/main/java/com/itmsg/device42/integration/ci/relation/ActCiRelationWriter.java`

```java
package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/** ACTCIRELATION 쓰기를 전담한다. 모든 관계 유형이 공유한다. */
@Component
public class ActCiRelationWriter {
    private static final Logger log = LoggerFactory.getLogger(ActCiRelationWriter.class);
    private static final String CHANGE_BY = "Device42";
    private static final int SWAPPED = 0;

    private final JdbcTemplate maximoJdbcTemplate;

    public ActCiRelationWriter(@Qualifier("maximoJdbcTemplate") JdbcTemplate maximoJdbcTemplate) {
        this.maximoJdbcTemplate = maximoJdbcTemplate;
    }

    /** @return 적재에 성공한 관계 건수. 걸러진 건과 실패한 건은 각각 로그로 남는다. */
    public int write(List<ActCiRelationUpsert> data) {
        LocalDateTime changeDate = LocalDateTime.now();
        int loaded = 0;

        for (ActCiRelationUpsert relation : data) {
            try {
                int rows = maximoJdbcTemplate.update(MERGE_ACTCIRELATION_QUERY,
                        relation.sourceCiNum(), relation.targetCiNum(), relation.relationNum(),
                        SWAPPED, CHANGE_BY, changeDate);
                if (rows == 0) {
                    log.warn("관계를 건너뜁니다. 양 끝 CI가 없거나 실제 분류쌍의 규칙이 없습니다. {} -> {} ({})",
                            relation.sourceCiNum(), relation.targetCiNum(), relation.relationNum());
                    continue;
                }
                loaded++;
            } catch (DataAccessException e) {
                log.error("관계 적재에 실패했습니다. {} -> {} ({})",
                        relation.sourceCiNum(), relation.targetCiNum(), relation.relationNum(), e);
            }
        }
        return loaded;
    }

    /**
     * 파라미터 마커에 CAST가 필요하다. USING 절의 마커는 DB2가 타입을 추론하지 못해
     * SQLCODE=-418로 거부한다. USING 절에서는 NEXT VALUE FOR가 금지된다(SQLCODE=-348).
     * 예상 분류를 받지 않고 조인된 ACTCI 행의 실제 CLASSSTRUCTUREID로 규칙을 확인한다.
     * 그래서 도착이 물리·가상 Computer 어느 쪽이든 호출자 분기 없이 처리된다.
     * ANCESTORCI·BASELINEDATE·GUID는 UPDATE 절에 없다. 기존 값을 덮어쓰지 않는다.
     */
    private static final String MERGE_ACTCIRELATION_QUERY = """
            MERGE INTO MAXIMO.ACTCIRELATION AS target
            USING (
                SELECT input.SOURCECI, input.TARGETCI, input.RELATIONNUM,
                       input.SWAPPED, input.CHANGEBY, input.CHANGEDATE
                FROM (VALUES (
                    CAST(? AS VARCHAR(150)), CAST(? AS VARCHAR(150)),
                    CAST(? AS VARCHAR(192)), CAST(? AS INTEGER),
                    CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
                )) AS input (
                    SOURCECI, TARGETCI, RELATIONNUM, SWAPPED, CHANGEBY, CHANGEDATE
                )
                JOIN MAXIMO.ACTCI s ON s.ACTCINUM = input.SOURCECI
                JOIN MAXIMO.ACTCI t ON t.ACTCINUM = input.TARGETCI
                WHERE EXISTS (
                    SELECT 1 FROM MAXIMO.RELATIONRULES r
                    WHERE r.RELATIONNUM = input.RELATIONNUM
                      AND r.SOURCECLASS = s.CLASSSTRUCTUREID
                      AND r.TARGETCLASS = t.CLASSSTRUCTUREID
                )
                AND EXISTS (
                    SELECT 1 FROM MAXIMO.RELATION r
                    WHERE r.RELATIONNUM = input.RELATIONNUM
                )
            ) AS source
            ON target.SOURCECI = source.SOURCECI
                AND target.TARGETCI = source.TARGETCI
                AND target.RELATIONNUM = source.RELATIONNUM
            WHEN MATCHED THEN
                UPDATE SET
                    SWAPPED = source.SWAPPED,
                    CHANGEBY = source.CHANGEBY,
                    CHANGEDATE = source.CHANGEDATE
            WHEN NOT MATCHED THEN
                INSERT (
                    ACTCIRELATIONID, SOURCECI, TARGETCI, RELATIONNUM,
                    SWAPPED, CHANGEBY, CHANGEDATE
                ) VALUES (
                    NEXT VALUE FOR MAXIMO.ACTCIRELATIONSEQ,
                    source.SOURCECI, source.TARGETCI, source.RELATIONNUM,
                    source.SWAPPED, source.CHANGEBY, source.CHANGEDATE
                )
            """;
}
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.relation.ActCiRelationWriterTest"
```

Expected: 7개 테스트 PASS.

- [ ] **Step 7: 커밋한다**

```bash
git add src/main/java/com/itmsg/device42/dto/maximo/ci/ActCiRelationUpsert.java src/main/java/com/itmsg/device42/integration/ci/relation/ActCiRelationWriter.java src/test/resources/ci/schema.sql src/test/java/com/itmsg/device42/integration/ci/relation/ActCiRelationWriterTest.java
git commit -m "$(cat <<'EOF'
feat: ACTCIRELATION 공통 저장 구현

MERGE 한 문장이 양 끝 ACTCI 존재와 실제 분류쌍의 RELATIONRULES 규칙을
확인한다. 예상 분류를 파라미터로 받지 않으므로 도착이 물리·가상 Computer
어느 쪽이든 호출자가 분기하지 않는다.

ANCESTORCI·BASELINEDATE·GUID는 UPDATE 절에 두지 않아 기존 값을 보존한다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: CiRelationSource enum과 CiRelationJob

OS 관계 하나를 정의하고, 조회·페이징·변환·저장·집계를 한 곳에 구현한다. 이 태스크가 끝나면 관계 단계가 동작한다.

**Files:**
- Modify: `src/main/java/com/itmsg/device42/integration/ci/CiSourceFilter.java` (클래스와 상수를 public으로)
- Create: `src/main/java/com/itmsg/device42/integration/ci/relation/CiRelationSource.java`
- Create: `src/main/java/com/itmsg/device42/integration/ci/relation/CiRelationJob.java`
- Test: `src/test/java/com/itmsg/device42/integration/ci/relation/CiRelationJobTest.java`

**Interfaces:**
- Consumes: `ActCiRelationWriter.write(List<ActCiRelationUpsert>)` (Task 1), `Device42ConnectionFactory.openConnection()`, `CiSourceFilter.COMPUTER`
- Produces:
  - `enum CiRelationSource` — `String relationNum()`, `String countQuery()`, `String pageQuery(long offset, int limit)`
  - `CiRelationJob implements IntegrationJob` — 빈 이름 `ci-relation`, `void run()`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`src/test/java/com/itmsg/device42/integration/ci/relation/CiRelationJobTest.java`

```java
package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CiRelationJobTest {

    @Test
    void everyPageQueryOrdersByRelationKeyAndPages() {
        for (CiRelationSource source : CiRelationSource.values()) {
            assertThat(source.pageQuery(20, 10))
                    .as("%s 페이지 SQL", source)
                    .contains("AS sourceci")
                    .contains("AS targetci")
                    .contains("ORDER BY sourceci, targetci")
                    .contains("LIMIT 10 OFFSET 20");
            assertThat(source.countQuery()).as("%s 건수 SQL", source).contains("COUNT(*)");
            assertThat(source.relationNum()).as("%s 관계 코드", source).startsWith("RELATION.");
        }
    }

    @Test
    void readsEveryPageAndReportsLoadedCount() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        stubCount(factory, 2);
        var writer = mock(ActCiRelationWriter.class);
        when(writer.write(anyList())).thenReturn(2);

        new CiRelationJob(factory, writer).run();

        var captured = ArgumentCaptor.forClass(List.class);
        verify(writer, atLeastOnce()).write(captured.capture());
        assertThat(captured.getAllValues().getFirst())
                .containsExactly(new ActCiRelationUpsert(
                        "D42:DEVICEOS:147", "D42:DEVICE:173", "RELATION.INSTALLEDON"));
    }

    @Test
    void skipsWriterWhenSourceHasNoRows() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        stubCount(factory, 0);
        var writer = mock(ActCiRelationWriter.class);

        new CiRelationJob(factory, writer).run();

        verifyNoInteractions(writer);
    }

    @Test
    void continuesAfterOneRelationSourceFails() throws Exception {
        var factory = mock(Device42ConnectionFactory.class);
        when(factory.openConnection()).thenThrow(new IllegalStateException("device42 down"));
        var writer = mock(ActCiRelationWriter.class);

        new CiRelationJob(factory, writer).run();

        verify(factory, times(CiRelationSource.values().length)).openConnection();
        verifyNoInteractions(writer);
    }

    /**
     * 건수 조회는 count를, 페이지 조회는 관계 한 쌍을 돌려준다.
     * ResultSet 목은 Answer 밖에서 미리 만든다. Answer 안에서 when()을 부르면
     * Mockito의 진행 중 스터빙 상태가 꼬인다.
     */
    private static void stubCount(Device42ConnectionFactory factory, long count) throws SQLException {
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(factory.openConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        var countRs = mock(ResultSet.class);
        when(countRs.next()).thenReturn(true, false);
        when(countRs.getLong(1)).thenReturn(count);

        var pageRs = mock(ResultSet.class);
        when(pageRs.next()).thenReturn(true, false);
        when(pageRs.getString("sourceci")).thenReturn("D42:DEVICEOS:147");
        when(pageRs.getString("targetci")).thenReturn("D42:DEVICE:173");

        when(statement.executeQuery(anyString())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).contains("COUNT(*)") ? countRs : pageRs);
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.relation.CiRelationJobTest"
```

Expected: 컴파일 실패. `CiRelationSource`와 `CiRelationJob`이 없다.

- [ ] **Step 3: CiSourceFilter를 public으로 연다**

`src/main/java/com/itmsg/device42/integration/ci/CiSourceFilter.java` 에서 두 줄만 바꾼다. 다른 줄은 손대지 않는다.

```java
public final class CiSourceFilter {
```

```java
    public static final String COMPUTER = """
```

- [ ] **Step 4: CiRelationSource enum을 작성한다**

`src/main/java/com/itmsg/device42/integration/ci/relation/CiRelationSource.java`

`WITH computer` 프리픽스가 닮았다고 템플릿으로 묶지 않는다. 반복되는 것은 문자열이고, DOQL 러너에 그대로 붙여 넣어 확인할 수 있어야 한다.

```java
package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.integration.ci.CiSourceFilter;

/**
 * 관계 하나의 정의. 관계를 늘릴 때 늘어나는 것은 이 enum의 상수 하나뿐이다.
 * 조회 반복·DTO 변환·저장·집계는 CiRelationJob이 공유한다.
 * 페이지 SQL은 sourceci·targetci 두 컬럼만 돌려주고 관계 키로 정렬한다.
 * DOQL은 정렬 없는 OFFSET의 순서를 보장하지 않는다.
 */
public enum CiRelationSource {
    /** OS → Computer. 근거: view_deviceos_v1 의 deviceos_pk·device_fk. */
    OS_INSTALLED_ON_COMPUTER("RELATION.INSTALLEDON", Queries.OS_COUNT, Queries.OS_PAGE);

    private final String relationNum;
    private final String countQuery;
    private final String pageQuery;

    CiRelationSource(String relationNum, String countQuery, String pageQuery) {
        this.relationNum = relationNum;
        this.countQuery = countQuery;
        this.pageQuery = pageQuery;
    }

    public String relationNum() {
        return relationNum;
    }

    public String countQuery() {
        return countQuery;
    }

    public String pageQuery(long offset, int limit) {
        return pageQuery.formatted(limit, offset);
    }

    /** DOQL이 FROM 서브쿼리를 막아 건수 SQL과 페이지 SQL을 따로 둔다. */
    private static final class Queries {
        static final String OS_COUNT = """
                SELECT COUNT(*)
                FROM view_deviceos_v1 o
                JOIN view_device_v2 d ON d.device_pk = o.device_fk
                WHERE
                """ + CiSourceFilter.COMPUTER;

        static final String OS_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICEOS:' || CAST(o.deviceos_pk AS varchar) AS sourceci,
                       'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS targetci
                FROM view_deviceos_v1 o
                JOIN computer c ON c.device_pk = o.device_fk
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;
    }
}
```

- [ ] **Step 5: CiRelationJob을 작성한다**

`src/main/java/com/itmsg/device42/integration/ci/relation/CiRelationJob.java`

```java
package com.itmsg.device42.integration.ci.relation;

import com.itmsg.device42.config.Device42ConnectionFactory;
import com.itmsg.device42.dto.maximo.ci.ActCiRelationUpsert;
import com.itmsg.device42.integration.IntegrationJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * CI 관계 적재. 본체·스펙 적재가 모두 끝난 뒤 실행한다.
 * 분류·속성 정의가 필요 없으므로 CiDefinitionCache를 받지 않는다.
 * 저장된 CI의 존재·분류·규칙만 보므로 본체 일부가 실패해도 진행한다.
 */
@Component("ci-relation")
public class CiRelationJob implements IntegrationJob {
    private static final Logger log = LoggerFactory.getLogger(CiRelationJob.class);
    private static final int DEFAULT_BATCH_SIZE = 1000;

    private final Device42ConnectionFactory connectionFactory;
    private final ActCiRelationWriter writer;

    public CiRelationJob(Device42ConnectionFactory connectionFactory, ActCiRelationWriter writer) {
        this.connectionFactory = connectionFactory;
        this.writer = writer;
    }

    @Override
    public void run() {
        for (CiRelationSource source : CiRelationSource.values()) {
            try {
                log.info("{} 관계 적재를 시작합니다.", source);
                integrate(source);
            } catch (Exception e) {
                log.error("{} 관계 적재에 실패했습니다. 다음 관계를 계속합니다.", source, e);
            }
        }
    }

    private void integrate(CiRelationSource source) {
        long totalCount = getTotalCount(source);
        if (totalCount <= 0) {
            log.info("배치할 {} 관계가 없습니다. totalCount={}", source, totalCount);
            return;
        }

        log.info("배치할 {} 관계 총 데이터. totalCount={}", source, totalCount);

        long readCount = 0;
        long loadedCount = 0;

        for (long offset = 0; offset < totalCount; offset += DEFAULT_BATCH_SIZE) {
            int limit = (int) Math.min(DEFAULT_BATCH_SIZE, totalCount - offset);
            log.info("{} 관계 배치를 조회합니다. offset={}, limit={}", source, offset, limit);

            List<ActCiRelationUpsert> data = getData(source, offset, limit);
            readCount += data.size();
            loadedCount += writer.write(data);
        }

        if (loadedCount < readCount) {
            log.warn("적재되지 않은 {} 관계가 있습니다. 조회={}, 적재={}", source, readCount, loadedCount);
        }

        log.info("{} 관계 적재를 마쳤습니다. 원천={}, 조회={}, 적재={}",
                source, totalCount, readCount, loadedCount);
    }

    private long getTotalCount(CiRelationSource source) {
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(source.countQuery())) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new IllegalStateException(source + " 관계 건수 조회에 실패했습니다.", e);
        }
    }

    private List<ActCiRelationUpsert> getData(CiRelationSource source, long offset, int limit) {
        String query = source.pageQuery(offset, limit);
        try (Connection connection = connectionFactory.openConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            List<ActCiRelationUpsert> data = new ArrayList<>(limit);
            while (rs.next()) {
                data.add(new ActCiRelationUpsert(
                        rs.getString("sourceci"), rs.getString("targetci"), source.relationNum()));
            }
            return data;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    source + " 관계 조회에 실패했습니다. offset=" + offset, e);
        }
    }
}
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.relation.CiRelationJobTest"
```

Expected: 4개 테스트 PASS.

- [ ] **Step 7: 전체 테스트가 깨지지 않았는지 확인한다**

`CiSourceFilter`를 public으로 열었으므로 기존 CI 테스트를 함께 돌린다.

```bash
./gradlew test
```

Expected: 전체 PASS.

- [ ] **Step 8: 커밋한다**

```bash
git add src/main/java/com/itmsg/device42/integration/ci/CiSourceFilter.java src/main/java/com/itmsg/device42/integration/ci/relation/ src/test/java/com/itmsg/device42/integration/ci/relation/CiRelationJobTest.java
git commit -m "$(cat <<'EOF'
feat: CI 관계 적재 단계와 OS 관계 정의 추가

관계 정의는 CiRelationSource enum 상수 하나이고, 조회 페이징·DTO 변환·
저장 호출·집계는 CiRelationJob 한 곳에 둔다. 관계를 늘릴 때 늘어나는
것이 상수 하나가 되도록 한다.

CiDefinitionCache를 받지 않아 ci-relation 단독 실행이 본체 기준정보
로딩에 의존하지 않는다.

관계 패키지에서 수집 범위 조건을 쓰기 위해 CiSourceFilter를 public으로 연다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: 진입점 연결

`./run.sh ci`가 관계까지 하고 `./run.sh ci-relation`이 관계만 하도록 연결한다.

**Files:**
- Modify: `src/main/java/com/itmsg/device42/integration/ci/CiIntegrationJob.java`
- Modify: `run.sh` (사용법 주석)
- Test: `src/test/java/com/itmsg/device42/integration/ci/CiIntegrationJobTest.java` (테스트 추가, 기존 두 테스트 수정)

**Interfaces:**
- Consumes: `CiRelationJob.run()` (Task 2)
- Produces: `CiIntegrationJob(List<CiIntegrationTask>, CiDefinitionLoader, CiRelationJob)` — 생성자 인자가 셋으로 늘어난다

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`CiIntegrationJobTest.java`의 기존 두 테스트는 생성자 인자가 늘어나므로 함께 고친다. 아래 import를 추가한다.

```java
import com.itmsg.device42.integration.ci.relation.CiRelationJob;
```

기존 두 테스트의 생성자 호출을 바꾼다.

```java
CiIntegrationJob job = new CiIntegrationJob(List.of(computer, next), loader, mock(CiRelationJob.class));
```

```java
CiIntegrationJob job = new CiIntegrationJob(List.of(computer), loader, mock(CiRelationJob.class));
```

그리고 테스트 두 개를 추가한다.

```java
    @Test
    void runsRelationStageAfterEveryBodyTask() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache definitions = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load()).thenReturn(definitions);
        CiIntegrationTask computer = mock(CiIntegrationTask.class);
        CiIntegrationTask os = mock(CiIntegrationTask.class);
        CiRelationJob relations = mock(CiRelationJob.class);

        new CiIntegrationJob(List.of(computer, os), loader, relations).run();

        var order = inOrder(computer, os, relations);
        order.verify(computer).integrate(definitions);
        order.verify(os).integrate(definitions);
        order.verify(relations).run();
    }

    @Test
    void runsRelationStageEvenAfterBodyTaskFailure() {
        CiDefinitionLoader loader = mock(CiDefinitionLoader.class);
        CiDefinitionCache definitions = new CiDefinitionCache(Map.of(), Map.of(), Set.of(), Map.of());
        when(loader.load()).thenReturn(definitions);
        CiIntegrationTask failing = mock(CiIntegrationTask.class);
        doThrow(new IllegalStateException("source failure")).when(failing).integrate(definitions);
        CiRelationJob relations = mock(CiRelationJob.class);

        new CiIntegrationJob(List.of(failing), loader, relations).run();

        verify(relations).run();
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.CiIntegrationJobTest"
```

Expected: 컴파일 실패. `CiIntegrationJob` 생성자가 인자 두 개만 받는다.

- [ ] **Step 3: CiIntegrationJob을 고친다**

import를 추가한다.

```java
import com.itmsg.device42.integration.ci.relation.CiRelationJob;
```

필드와 생성자를 바꾼다.

```java
    private final List<CiIntegrationTask> tasks;
    private final CiDefinitionLoader definitionLoader;
    private final CiRelationJob relationJob;

    public CiIntegrationJob(List<CiIntegrationTask> tasks, CiDefinitionLoader definitionLoader,
                            CiRelationJob relationJob) {
        this.tasks = tasks;
        this.definitionLoader = definitionLoader;
        this.relationJob = relationJob;
    }
```

`run()`의 for 루프가 끝난 뒤, 메서드 끝에 한 줄을 추가한다. `failures`는 기존 코드 그대로 둔다.

```java
        relationJob.run();
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.CiIntegrationJobTest"
```

Expected: 4개 테스트 PASS.

- [ ] **Step 5: run.sh 사용법에 관계 잡을 적는다**

`run.sh` 상단 주석의 잡 이름 줄을 바꾼다.

```bash
# 잡 이름은 Spring 빈 이름이다: asset, ci, ci-relation, software, conversion
# ci 는 CI 본체·스펙에 이어 관계까지 적재한다. ci-relation 은 관계만 적재한다.
# ./run.sh ci ci-relation 처럼 둘 다 주면 관계가 두 번 돈다. MERGE 라 결과는 같다.
```

- [ ] **Step 6: 전체 테스트를 돌린다**

```bash
./gradlew test
```

Expected: 전체 PASS.

- [ ] **Step 7: 커밋한다**

```bash
git add src/main/java/com/itmsg/device42/integration/ci/CiIntegrationJob.java src/test/java/com/itmsg/device42/integration/ci/CiIntegrationJobTest.java run.sh
git commit -m "$(cat <<'EOF'
feat: ci 잡에 관계 단계 연결과 ci-relation 진입점 추가

CiIntegrationJob 이 모든 본체 task 를 실행한 뒤 CiRelationJob 을 호출한다.
본체 task 가 실패해도 관계 단계는 진행한다. 저장된 CI 의 존재·분류·규칙만
보기 때문이다.

JobRunner 가 인자를 순서대로 실행하므로 빈 이름 두 개로 진입점 두 개를
얻는다. 관계 구현은 CiRelationJob 한 곳에만 있다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: OS 관계 운영 적재 검증

**이 태스크는 운영 Maximo에 씁니다. 시작 전에 사용자 승인을 받습니다.**

자동 적재 결과가 2026-09-15 수동 검증 샘플과 같은지 대조한다. 기준은 `ACTCIRELATIONID=6001`, `D42:DEVICEOS:147 → D42:DEVICE:173`, `RELATION.INSTALLEDON`, `SWAPPED=0`이다.

**Files:**
- Create: `docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql`
- Modify: `docs/data-analysis/data-mapping/ci/types/os.md` (6절 검증 상태)
- Modify: `docs/data-analysis/data-mapping/ci/actcirelation.md` (4절 검증 수준)
- Modify: `docs/data-analysis/design/ci/relations.md` (먼저 구현할 관계 표의 OS 행)

- [ ] **Step 1: 적재 전 상태를 기록한다**

`docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql`

```sql
-- name: relation-count
SELECT RELATIONNUM, COUNT(*) AS CNT FROM MAXIMO.ACTCIRELATION GROUP BY RELATIONNUM ORDER BY RELATIONNUM;

-- name: relation-rows
SELECT ACTCIRELATIONID, SOURCECI, TARGETCI, RELATIONNUM, SWAPPED, CHANGEBY, ANCESTORCI, SOURCECIGUID, TARGETCIGUID FROM MAXIMO.ACTCIRELATION ORDER BY ACTCIRELATIONID;

-- name: sample-6001
SELECT ACTCIRELATIONID, SOURCECI, TARGETCI, RELATIONNUM, SWAPPED, CHANGEBY, CHANGEDATE FROM MAXIMO.ACTCIRELATION WHERE SOURCECI = 'D42:DEVICEOS:147' AND TARGETCI = 'D42:DEVICE:173';

-- name: orphan-check
SELECT COUNT(*) AS ORPHANS FROM MAXIMO.ACTCIRELATION r WHERE NOT EXISTS (SELECT 1 FROM MAXIMO.ACTCI s WHERE s.ACTCINUM = r.SOURCECI) OR NOT EXISTS (SELECT 1 FROM MAXIMO.ACTCI t WHERE t.ACTCINUM = r.TARGETCI);

-- name: rule-check
SELECT COUNT(*) AS NO_RULE FROM MAXIMO.ACTCIRELATION r JOIN MAXIMO.ACTCI s ON s.ACTCINUM = r.SOURCECI JOIN MAXIMO.ACTCI t ON t.ACTCINUM = r.TARGETCI WHERE NOT EXISTS (SELECT 1 FROM MAXIMO.RELATIONRULES x WHERE x.RELATIONNUM = r.RELATIONNUM AND x.SOURCECLASS = s.CLASSSTRUCTUREID AND x.TARGETCLASS = t.CLASSSTRUCTUREID);

-- name: target-class-spread
SELECT t.CLASSSTRUCTUREID, c.CLASSIFICATIONID, COUNT(*) AS CNT FROM MAXIMO.ACTCIRELATION r JOIN MAXIMO.ACTCI t ON t.ACTCINUM = r.TARGETCI JOIN MAXIMO.CLASSSTRUCTURE c ON c.CLASSSTRUCTUREID = t.CLASSSTRUCTUREID WHERE r.RELATIONNUM = 'RELATION.INSTALLEDON' GROUP BY t.CLASSSTRUCTUREID, c.CLASSIFICATIONID ORDER BY CNT DESC;
```

읽기 전용 러너로 실행하고 결과를 `local/db-access-kit/work/` 아래에 남긴다. 커밋하지 않는다.

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql \
  local/db-access-kit/work/ci-relation-before
```

Expected: `relation-count`가 기존 2건(`RELATION.RUNSON` 1, `RELATION.INSTALLEDON` 1)을 보여준다.

- [ ] **Step 2: 관계만 실행한다**

```bash
./run.sh ci-relation
```

Expected: `OS_INSTALLED_ON_COMPUTER 관계 적재를 마쳤습니다.` 로그. ERROR 없음. 조회 건수와 적재 건수가 같거나, 다르면 건너뛴 사유가 WARN에 남는다.

- [ ] **Step 3: 적재 결과를 대조한다**

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql \
  local/db-access-kit/work/ci-relation-after
```

확인할 것:

| 항목 | 기대 |
| --- | --- |
| `sample-6001` | `ACTCIRELATIONID`가 6001 그대로. 자동 적재가 기존 행을 UPDATE 했고 새 행을 만들지 않았다 |
| `sample-6001` | `SWAPPED=0`, `CHANGEBY='Device42'`, `CHANGEDATE`가 이번 실행 시각 |
| `relation-rows` | `ACTCIRELATIONID=5001`(UI 생성)의 GUID와 `RELATION.RUNSON`이 그대로 |
| `orphan-check` | `ORPHANS`가 0 |
| `rule-check` | `NO_RULE`이 0 |
| `target-class-spread` | `SYS.COMPUTERSYSTEM`과 `SYS.VIRTUALCOMPUTERSYSTEM`이 둘 다 나온다. 호출자 분기 없이 양쪽이 저장됐다는 증거 |

- [ ] **Step 4: 재실행 멱등성을 확인한다**

```bash
./run.sh ci-relation
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql \
  local/db-access-kit/work/ci-relation-rerun
```

Expected: `relation-count`의 건수가 Step 3과 같다. `relation-rows`의 `ACTCIRELATIONID` 값이 모두 그대로다.

- [ ] **Step 5: 문서에 검증 결과를 적는다**

관측 사실만 적는다. 확인하지 않은 것을 확인한 것처럼 적지 않는다.

`docs/data-analysis/design/ci/relations.md`의 「먼저 구현할 관계」 표에서 OS 행의 판정을 고친다.

```
| OS_INSTALLED_ON_COMPUTER | OS → Computer | RELATION.INSTALLEDON | 자동 적재·재실행 멱등성·물리/가상 양쪽 확인 |
```

`docs/data-analysis/data-mapping/ci/types/os.md`의 상태 줄에서 `관계 자동 적재 미구현`을 실제 결과로 바꾼다.

`docs/data-analysis/data-mapping/ci/actcirelation.md`의 헤더 주석에서 `아래 공통 MERGE는 미실행 초안이다`를 실행 결과로 바꾸고, 4절 검증 항목 중 이번에 확인한 것을 표시한다. 확인하지 않은 항목(Disk·Filesystem 분류쌍, 이동·삭제)은 남긴다.

- [ ] **Step 6: 커밋한다**

```bash
git add docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql docs/data-analysis/design/ci/relations.md docs/data-analysis/data-mapping/ci/types/os.md docs/data-analysis/data-mapping/ci/actcirelation.md
git commit -m "$(cat <<'EOF'
docs: OS 관계 자동 적재 검증 결과 반영

공통 MERGE 로 OS → Computer 관계를 적재하고 재실행 멱등성과 물리·가상
Computer 양쪽 저장을 확인했다. 기존 수동 샘플 행의 ID 와 UI 가 만든
관계의 GUID 가 모두 보존됐다.

Disk·Filesystem 분류쌍과 이동·삭제는 검증하지 않았으므로 미결로 남긴다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Disk·Filesystem 관계 확장

상수 두 개만 추가해서 끝나는지 실제로 확인한다. `CiRelationJob`과 `ActCiRelationWriter`는 한 줄도 바뀌면 안 된다.

Filesystem은 원천 한 행이 여러 쌍을 낸다. 본체용 `DISTINCT ON`을 쓰지 않고, 건수도 마운트포인트가 아니라 **쌍**을 센다.

**Files:**
- Modify: `src/main/java/com/itmsg/device42/integration/ci/relation/CiRelationSource.java`
- Test: `src/test/java/com/itmsg/device42/integration/ci/relation/CiRelationJobTest.java` (테스트 추가)

**Interfaces:**
- Consumes: Task 2의 `CiRelationSource` 구조 그대로
- Produces: 상수 `COMPUTER_CONTAINS_DISK`, `COMPUTER_CONTAINS_FILESYSTEM`

- [ ] **Step 1: 실패하는 테스트를 작성한다**

`CiRelationJobTest.java`에 테스트를 추가한다. import를 추가한다.

```java
import com.itmsg.device42.integration.ci.FilesystemCiIntegrate;
```

```java
    @Test
    void definesEveryRelationPlannedForThisStage() {
        assertThat(CiRelationSource.values()).containsExactly(
                CiRelationSource.OS_INSTALLED_ON_COMPUTER,
                CiRelationSource.COMPUTER_CONTAINS_DISK,
                CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM);
        assertThat(CiRelationSource.COMPUTER_CONTAINS_DISK.relationNum()).isEqualTo("RELATION.CONTAINS");
        assertThat(CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM.relationNum()).isEqualTo("RELATION.CONTAINS");
    }

    @Test
    void diskRelationKeepsHardDiskFilter() {
        assertThat(CiRelationSource.COMPUTER_CONTAINS_DISK.pageQuery(0, 10))
                .contains("pm.type_name = 'Hard Disk'")
                .contains("'D42:DEVICE:'")
                .contains("'D42:PART:'");
        assertThat(CiRelationSource.COMPUTER_CONTAINS_DISK.countQuery())
                .contains("pm.type_name = 'Hard Disk'");
    }

    @Test
    void filesystemRelationExpandsDeviceArrayAndReusesExcludedTypes() {
        String page = CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM.pageQuery(0, 10);
        assertThat(page)
                .contains("ANY(m.device_fks)")
                .doesNotContain("DISTINCT ON")
                .contains("'D42:MOUNTPOINT:'");
        assertThat(CiRelationSource.COMPUTER_CONTAINS_FILESYSTEM.countQuery())
                .contains("ANY(m.device_fks)")
                .doesNotContain("EXISTS");
        for (String excluded : FilesystemCiIntegrate.EXCLUDED_TYPES) {
            assertThat(page).as("제외 타입 %s", excluded).contains("'" + excluded + "'");
        }
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew test --tests "com.itmsg.device42.integration.ci.relation.CiRelationJobTest"
```

Expected: 컴파일 실패. `COMPUTER_CONTAINS_DISK`가 없다. `FilesystemCiIntegrate.EXCLUDED_TYPES`는 이미 package-private static final이므로 다른 패키지의 테스트에서 보이지 않아 함께 실패한다.

- [ ] **Step 3: EXCLUDED_TYPES를 public으로 연다**

`src/main/java/com/itmsg/device42/integration/ci/FilesystemCiIntegrate.java`의 해당 줄만 바꾼다.

```java
    public static final List<String> EXCLUDED_TYPES = List.of("overlay", "devtmpfs", "squashfs", "efivarfs");
```

- [ ] **Step 4: 상수 두 개를 추가한다**

`CiRelationSource.java`의 enum 상수 목록에 둘을 더한다. 세미콜론 위치에 주의한다.

```java
    /** OS → Computer. 근거: view_deviceos_v1 의 deviceos_pk·device_fk. */
    OS_INSTALLED_ON_COMPUTER("RELATION.INSTALLEDON", Queries.OS_COUNT, Queries.OS_PAGE),

    /** Computer → Disk. 근거: Hard Disk 조건의 part_pk·device_fk. */
    COMPUTER_CONTAINS_DISK("RELATION.CONTAINS", Queries.DISK_COUNT, Queries.DISK_PAGE),

    /** Computer → Filesystem. 근거: mountpoint_pk·device_fks. 배열의 모든 연결을 보존한다. */
    COMPUTER_CONTAINS_FILESYSTEM("RELATION.CONTAINS", Queries.FILESYSTEM_COUNT, Queries.FILESYSTEM_PAGE);
```

`Queries` 클래스에 import와 상수를 더한다.

```java
import com.itmsg.device42.integration.ci.FilesystemCiIntegrate;
import java.util.stream.Collectors;
```

```java
        private static final String EXCLUDED_TYPES_SQL = FilesystemCiIntegrate.EXCLUDED_TYPES.stream()
                .map(type -> "'" + type + "'")
                .collect(Collectors.joining(", "));

        static final String DISK_COUNT = """
                SELECT COUNT(*)
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN view_device_v2 d ON d.device_pk = p.device_fk
                WHERE pm.type_name = 'Hard Disk' AND
                """ + CiSourceFilter.COMPUTER;

        static final String DISK_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS sourceci,
                       'D42:PART:' || CAST(p.part_pk AS varchar) AS targetci
                FROM view_part_v1 p
                JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
                JOIN computer c ON c.device_pk = p.device_fk
                WHERE pm.type_name = 'Hard Disk'
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;

        /** 본체 건수는 마운트포인트를 세지만 관계 건수는 장비 배열을 펼친 쌍을 센다. */
        static final String FILESYSTEM_COUNT = """
                SELECT COUNT(*)
                FROM view_mountpoint_v2 m
                JOIN view_device_v2 d ON d.device_pk = ANY(m.device_fks)
                WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + EXCLUDED_TYPES_SQL + """
                ))
                AND
                """ + CiSourceFilter.COMPUTER;

        static final String FILESYSTEM_PAGE = """
                WITH computer AS (
                    SELECT d.device_pk
                    FROM view_device_v2 d
                    WHERE
                """ + CiSourceFilter.COMPUTER + """
                )
                SELECT 'D42:DEVICE:' || CAST(c.device_pk AS varchar) AS sourceci,
                       'D42:MOUNTPOINT:' || CAST(m.mountpoint_pk AS varchar) AS targetci
                FROM view_mountpoint_v2 m
                JOIN computer c ON c.device_pk = ANY(m.device_fks)
                WHERE (m.fstype_name IS NULL OR m.fstype_name NOT IN (""" + EXCLUDED_TYPES_SQL + """
                ))
                ORDER BY sourceci, targetci
                LIMIT %d OFFSET %d
                """;
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
./gradlew test
```

Expected: 전체 PASS. `CiRelationJob`과 `ActCiRelationWriter`는 수정하지 않았다.

- [ ] **Step 6: 실행기가 안 바뀌었는지 확인한다**

```bash
git diff --stat src/main/java/com/itmsg/device42/integration/ci/relation/
```

Expected: `CiRelationSource.java`만 바뀐다. `CiRelationJob.java`와 `ActCiRelationWriter.java`는 목록에 없다. 목록에 있으면 공통화가 실패한 것이므로 왜 바뀌어야 했는지 먼저 확인한다.

- [ ] **Step 7: 커밋한다**

```bash
git add src/main/java/com/itmsg/device42/integration/ci/relation/CiRelationSource.java src/main/java/com/itmsg/device42/integration/ci/FilesystemCiIntegrate.java src/test/java/com/itmsg/device42/integration/ci/relation/CiRelationJobTest.java
git commit -m "$(cat <<'EOF'
feat: Computer-Disk·Computer-Filesystem 관계 정의 추가

CiRelationSource 에 상수 둘만 더했다. 실행기와 Writer 는 바뀌지 않았다.

Filesystem 은 원천 한 행이 여러 쌍을 내므로 본체용 DISTINCT ON 을 쓰지
않고 건수도 마운트포인트가 아니라 쌍을 센다. 제외 파일시스템 목록은
FilesystemCiIntegrate 의 정의를 그대로 쓴다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 8: 운영 적재를 검증한다 — 사용자 승인 후 진행**

```bash
./run.sh ci-relation
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/ci-relation-load-check.sql \
  local/db-access-kit/work/ci-relation-disk-fs
```

확인할 것:

| 항목 | 기대 |
| --- | --- |
| `relation-count` | `RELATION.CONTAINS`가 Disk·Filesystem 건수만큼 늘었다 |
| `orphan-check` | `ORPHANS`가 0 |
| `rule-check` | `NO_RULE`이 0 |
| Filesystem 배열 | 같은 `D42:MOUNTPOINT:*`가 여러 `D42:DEVICE:*`에 붙은 행이 있으면 배열 보존이 동작한 것이다. 없으면 원천에 다중 연결이 없는 것이므로 `device_fks` 길이를 D42에서 직접 확인한다 |

- [ ] **Step 9: 문서에 검증 결과를 적는다**

`docs/data-analysis/design/ci/relations.md`의 「먼저 구현할 관계」 표에서 Disk·Filesystem 행의 판정을 실제 결과로 바꾼다. 「세 관계가 같은 구조에 들어가는가」 절의 `이 대조는 문서상 확인이다` 문장을 실제 적재 결과로 갱신한다.

`docs/data-analysis/data-mapping/ci/types/computer.md` 7절의 상태 줄을 갱신한다.

```bash
git add docs/data-analysis/design/ci/relations.md docs/data-analysis/data-mapping/ci/types/computer.md
git commit -m "$(cat <<'EOF'
docs: Disk·Filesystem 관계 적재 검증 결과 반영

상수 두 개 추가만으로 두 관계가 적재되는 것을 확인했다. 실행기와 Writer 는
바뀌지 않았다. Filesystem 배열의 다중 연결 보존 결과를 함께 적는다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

## 범위 밖

이 계획에 포함하지 않는다. 각각 별도 결정이 필요하다.

- **VM → Host 관계.** 원천은 호스트 하나에 최대 17개 VM이 붙는데 등록 규칙은 `1:1`이다. 저장·승격·탐색 동작을 먼저 검증해야 한다.
- **Interface CI와 IP 관계.** Interface CI가 없다. Interface를 도입해도 `netport_fk`가 없는 공유 IP의 모든 장비 연결은 설명되지 않는다.
- **관계 이동·삭제.** MERGE는 새 관계를 추가할 뿐 이전 관계를 지우지 않는다. VM이 호스트 A에서 B로 옮기면 A 관계가 남는다. ETL 관리 범위와 원천 조회의 완전한 성공 여부를 전제로 한 정리 정책이 필요하며 [ISSUE-7](../../data-analysis/open-issues.md)과 함께 검토한다.
- **이번 실행에 성공한 CI만 연결.** `ActCiWriter`가 성공 건수 대신 식별자를 반환하는 결과 계약이 필요하다.
- **JDBC 배치와 시퀀스 CACHE.** 관계 200건 규모에서 필요하지 않다. 실제 적재 시간을 보고 판단한다.
