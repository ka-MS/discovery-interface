# ACTCIRELATION

실제 CI 관계의 공통 Target 매핑이다. 유형별 연결 의미와 원천 SQL은 출발 유형 문서가 소유한다.

> 메타데이터 재조회: 2026-09-15 · MAXIMO / BLUDB.
> 관계 설계: [CI 관계 설계](../../design/ci/relations.md).
> 관측 근거: [관계 정의](../../knowledge/maximo/computer-ci-relations.md).
> 아래 공통 MERGE는 2026-09-15 `./run.sh ci-relation`으로 운영 Maximo에 실행해 세 관계
> (OS_INSTALLED_ON_COMPUTER 63건·COMPUTER_CONTAINS_DISK 19건·COMPUTER_CONTAINS_FILESYSTEM 60건)를
> 적재했고 재실행에서 `ACTCIRELATIONID` 유지를 확인했다. 검증 쿼리는
> [관계 적재 검증](../../exploration-queries/maximo/ci-relation-load-check.sql). OS → 물리 Computer 한 쌍은 **별도 INSERT·CI 승격·UI 표시를 검증했다**. [샘플 결과](../../knowledge/maximo/computer-ci-relations.md#oscomputer-승격-샘플-검증).
> Host→VM용 `VIRTUALIZES` 관계 정의와 1:N 분류쌍 두 건은 MAS UI 등록·재조회·실제 관계 연결을 완료했다.
> 정확한 등록값은 [VIRTUALIZES MAS UI 등록 결과](../../knowledge/maximo/computer-ci-relations.md#virtualizes-mas-ui-등록-결과)를 따른다.
> Device→IP는 접두어 없는 `USES`와 `N:N` 분류 규칙 세 건을 사용한다. Computer·VM 118건은
> 운영 적재를 검증했고 Network Cluster를 포함한 120건은 코드·원천 검증까지 완료했다.

호출 위치는 CI 본체 적재 이후의 관계 단계 하나다.
본체 task는 이 Writer를 호출하지 않는다. [실행 구조](../../design/ci/relations.md#실행-위치--ci-본체-적재-이후-별도-단계).

## 1. 관계 키

- 물리 PK: ACTCIRELATIONID.
- MERGE 키: **(SOURCECI, TARGETCI, RELATIONNUM)**. 실제 고유 인덱스와 동일하다.
- SOURCECI/TARGETCI: 각 ACTCI.ACTCINUM. 숫자 ACTCIID나 원천 PK만 넣지 않는다.
- relationnum은 정확한 RELATION 행의 코드. RELATION.CONTAINS와 CONTAINS를 동일시하지 않는다.
- 양 끝의 실제 CLASSSTRUCTUREID와 관계 코드로 RELATIONRULES를 대조한다.
- 물리 FK가 없으므로 DB가 양 끝 존재를 자동 보장하지 않는다.

Computer→Disk·Filesystem은 [Device](types/device.md#7-관계-매핑--2026-09-15),
OS→Computer는 [OS](types/os.md#6-관계-매핑--2026-09-15)를 참조한다.
DB Instance→DB의 기존 별도 검토는 [DB Instance](types/database-instance.md)를 유지하며,
Computer 관계 조사 결과를 DB 관계의 승인으로 해석하지 않는다.
DB Instance→Device는 같은 DB Instance 문서의 원천 경로를 사용하며 관계 코드는
`RELATION.RUNSON`, 저장 방향은 Instance→Device다.

## 2. 컬럼 매핑

공통 11개 영속 컬럼이다. 신규 Computer 관련 관계에 적용할 제안이며,
기존 관계를 일괄 갱신하거나 선택하지 않은 관계를 생성하지 않는다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCIRELATIONID | 고유 ID | BIGINT(19) | N | 채번 | MAXIMO.ACTCIRELATIONSEQ | INSERT에서 NEXT VALUE. 기존 행 ID 유지. 증가폭 사이 번호 임의 사용 금지 |
| SOURCECI | 소스 실제 구성 품목 번호 | UPPER(150) | N | 변환 | 유형별 원천 연결 키 | 유형 매핑의 출발 ACTCINUM |
| TARGETCI | 대상 실제 구성 품목 번호 | UPPER(150) | N | 변환 | 유형별 원천 연결 키 | 유형 매핑의 도착 ACTCINUM |
| RELATIONNUM | 관계 | UPPER(192) | N | 상수 | CiRelationSource 상수 | RELATION·RELATIONRULES에서 확인된 정확한 코드 |
| SWAPPED | 스왑됨 | YORN(1) | Y | 상수 | 사전 정의한 저장 방향 | OS → 물리 Computer는 0으로 단건 승격·표시 확인. 다른 관계는 검증 필요. 규칙 SWAPPED를 그대로 복사하지 않음 |
| CHANGEBY | 변경자 | UPPER(100) | Y | 상수 | 기존 CI 적재 규약 | Device42 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | Y | 변환 | 관계 매핑 시각 | 기존 CI와 같은 JVM 기본 시간대. 원천 발견 시각이 아님 |
| ANCESTORCI | 상위 실제 CI | UPPER(150) | Y | 원천없음 | 별도 상위 원천 없음 | 신규 NULL. SOURCECI를 무조건 복사하지 않음 |
| BASELINEDATE | 기준선 날짜 | DATETIME(10) | Y | 원천없음 | D42 관계 기준선 없음 | 신규 NULL |
| SOURCECIGUID | 소스 실제 CI GUID | ALN(192) | Y | 원천없음 | D42 GUID 없음 | 신규 NULL. 아래 관측 참조. 기존 값은 MERGE에서 유지 |
| TARGETCIGUID | 대상 실제 CI GUID | ALN(192) | Y | 원천없음 | D42 GUID 없음 | 신규 NULL. 기존 값은 MERGE에서 유지 |

**GUID 두 컬럼을 NULL로 두는 근거:** OS → 물리 Computer 샘플을 GUID 없이 INSERT한 뒤
CI 승격과 관계·부모 보존이 정상 동작했다. GUID는 UI가 만든 관계에서 관측되는 값이며
우리 원천에는 대응 값이 없다. 승격이 이를 요구하지 않는 것이 확인됐으므로 미결에서 내린다.
단건 관측이므로 다른 분류쌍에서 승격 문제가 보이면 재검토한다.
기존 행의 GUID는 MERGE의 UPDATE 대상이 아니다. NULL로 덮어쓰지 않는다.

CONTAINMENT·REVRELATIONSHIP·CARDINALITY는 ACTCIRELATION 컬럼이 아니다.
SWAPPED는 저장 순서 변경 여부를 나타내는 별도 필드이며
[관측 설명](../../knowledge/maximo/computer-ci-relations.md#플래그-의미와-주의)을 따른다.

## 3. 공통 저장 SQL 제안

호출자가 승인된 규칙과 실제 원천 연결로 sourceci/targetci를 만든다.
동일 관계 입력 중복은 세 컬럼 키로 제거하고, 양 끝과 규칙 확인은 MERGE의 USING 안에서 한다.

**예상 분류를 파라미터로 받지 않는다.** 현재 최소 정책은 저장된 ACTCI의
**실제 CLASSSTRUCTUREID** 쌍에 등록된 관계 규칙이 있는지 확인하는 것이다.
ACTCINUM의 접두어는 DB에 저장된 분류를 강제하지 않는다. 원천 매핑이 의도한 분류와의
일치 여부는 별도로 검사하지 않으며, 기존 CI의 분류가 올바르게 관리된다는 전제를 둔다.
기존 분류가 잘못되어도 그 분류쌍에 규칙이 있으면 이 가드는 통과한다. 그래서 도착 분류가
`SYS.COMPUTERSYSTEM`·`SYS.VIRTUALCOMPUTERSYSTEM` 둘 다 허용되는 관계도 분기 없이 처리된다.
RELATIONRULES에 두 분류쌍 행이 모두 있기 때문이다.

```sql
MERGE INTO MAXIMO.ACTCIRELATION AS target
USING (
    SELECT input.SOURCECI,input.TARGETCI,input.RELATIONNUM,
           input.SWAPPED,input.CHANGEBY,input.CHANGEDATE
    FROM (VALUES (
        CAST(? AS VARCHAR(150)), CAST(? AS VARCHAR(150)),
        CAST(? AS VARCHAR(192)), CAST(? AS INTEGER),
        CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
    )) AS input (
        SOURCECI,TARGETCI,RELATIONNUM,SWAPPED,CHANGEBY,CHANGEDATE
    )
    JOIN MAXIMO.ACTCI s ON s.ACTCINUM=input.SOURCECI
    JOIN MAXIMO.ACTCI t ON t.ACTCINUM=input.TARGETCI
    WHERE EXISTS (
        SELECT 1 FROM MAXIMO.RELATIONRULES r
        WHERE r.RELATIONNUM=input.RELATIONNUM
          AND r.SOURCECLASS=s.CLASSSTRUCTUREID
          AND r.TARGETCLASS=t.CLASSSTRUCTUREID
    )
    AND EXISTS (
        SELECT 1 FROM MAXIMO.RELATION r
        WHERE r.RELATIONNUM=input.RELATIONNUM
    )
) AS source
ON target.SOURCECI=source.SOURCECI
   AND target.TARGETCI=source.TARGETCI
   AND target.RELATIONNUM=source.RELATIONNUM
WHEN MATCHED THEN UPDATE SET
    SWAPPED=source.SWAPPED,
    CHANGEBY=source.CHANGEBY,
    CHANGEDATE=source.CHANGEDATE
WHEN NOT MATCHED THEN INSERT (
    ACTCIRELATIONID,SOURCECI,TARGETCI,RELATIONNUM,
    SWAPPED,CHANGEBY,CHANGEDATE
) VALUES (
    NEXT VALUE FOR MAXIMO.ACTCIRELATIONSEQ,
    source.SOURCECI,source.TARGETCI,source.RELATIONNUM,
    source.SWAPPED,source.CHANGEBY,source.CHANGEDATE
);
```

파라미터 순서는 sourceCiNum, targetCiNum, relationNum, swapped, changeBy, changeDate다.
선정된 기존 규칙만 사용한다는 프로젝트 정책을 SQL에 반영한 것이며,
Maximo의 모든 관계 적재 방식에 대한 제약이라고 주장하지 않는다.

- 부모/자식 존재 확인을 별도 SELECT로 반복할 필요는 없다.
- 규칙 조인을 EXISTS로 처리해 규칙 행 중복 때문에 한 관계 후보가 여러 행으로 늘어나지 않게 한다.
- 0건은 양 끝 미존재 또는 실제 분류쌍의 규칙 부재 가능성을 뜻한다. 최초 로그에 한 원인으로 단정하지 않는다.
- 저장 예외는 해당 관계를 기록하고 다음 관계로 진행하는 안이다.
- 가드는 현재 저장 상태를 확인한다. 외부 동시 삭제까지 물리 FK처럼 보장하지 않는다.
- MERGE 키가 달라진 새 관계는 추가된다. 이전 호스트·장비 관계 삭제는 자동 수행하지 않는다.
- 신규 NULL 필드들은 기존 행에서 덮어쓰지 않는다.
- 가드는 RELATIONRULES 행의 존재만 확인하고 CARDINALITY는 읽지 않는다. 원천과 다른 카디널리티가
  등록돼도 Writer가 경고하지 않으므로 관계 도입 전 별도로 대조해야 한다. Host→VM은 2026-09-16
  두 분류쌍의 신규 VIRTUALIZES 1:N 규칙을 읽기 전용 재조회한 뒤 enum 상수를 추가했다.

## 4. 검증 수준과 후속

D42의 논리키 관계 SELECT와 Maximo의 분류쌍·키·참조 메타데이터는 실조회했다.
공통 MERGE는 2026-09-15 `./run.sh ci-relation`으로 운영 Maximo에 실제 실행했다.
아래 여섯 항목 중 이번에 확인한 것과 확인하지 않은 것을 구분한다.

1. 양 끝 정상/누락, 실제 분류쌍의 규칙 부재별 적재 여부. **자동 테스트 확인.** `ActCiRelationWriterTest`에서 H2로 정상 저장·양 끝 누락·규칙 및 관계 코드 부재 시 건너뜀을 확인했다. 실제 DB의 `orphan-check`=0, `rule-check`=0은 저장 결과에 고아 관계·규칙 없는 관계가 없다는 관측이며, 누락·규칙 부재 입력을 넣어 가드 동작을 검증한 결과는 아니다.
2. 재실행 시 관계 한 행 유지와 ACTCIRELATIONID 보존. **확인.** 재실행 후 `relation-count` 합계 143건(RELATION.CONTAINS 79 + RELATION.INSTALLEDON 63 + RELATION.RUNSON 1) 전 행의 ID가 적재 직후 스냅샷과 동일했고(`relation-rows.tsv` diff 없음), 수동 샘플 6001도 유지됐다.
3. OS → 물리 Computer 단건은 SWAPPED=0, CI 승격 후 관계 방향·부모 보존 확인. 다른 분류쌍·복수 관계·탐색은 추가 검증. **분류쌍 확인.** 도착이 물리(SYS.COMPUTERSYSTEM) 5건·가상(SYS.VIRTUALCOMPUTERSYSTEM) 58건 모두 관측됐다. 신규로 자동 적재된 62건의 CI 승격은 이번에 재검증하지 않았다. 기존 수동 샘플 6001 한 쌍만 승격까지 확인된 상태다.
4. 관계 하나의 DB 저장 예외 뒤 나머지 관계 계속 처리. **자동 테스트 확인, 실제 Maximo에서는 미검증.** `ActCiRelationWriterTest.continuesAfterDatabaseRejectsFirstRelation`에서 H2 CHECK 제약으로 양 끝·규칙이 정상인 첫 관계의 저장을 거부하고, 다음 관계만 저장되며 성공 건수가 1인지 확인했다. 실제 Maximo 적재에서는 저장 오류가 발생하지 않아 예외 경로를 재현하지 않았다.
5. 관계 단계가 본체 적재 이후에 실행되어 뒤쪽 배치의 상대 CI를 놓치지 않는지. **실행 순서는 자동 테스트 확인, 실제 DB 시나리오는 미검증.** `CiIntegrationJobTest`에서 본체 task 종료 후 관계 호출 및 본체 실패 후 관계 진행을 확인했다. 실제 적재 검증은 `ci-relation` 단독 실행이므로 뒤쪽 본체 배치의 상대 CI를 연결하는 시나리오는 재현하지 않았다.
6. 여러 장비 배열·여러 IP를 첫 번째 하나로 줄이지 않는지. **Writer의 다중 연결 저장·재실행은 자동 테스트 확인, Filesystem 원천 추출부터 전달까지는 미검증.** `ActCiRelationWriterTest.keepsBothComputerLinksToOneFilesystemAndTheirIdsOnRerun`에서 Filesystem 하나와 물리·가상 Computer 둘의 관계 DTO를 직접 전달해 두 관계가 저장되고, 순서를 바꿔 재실행해도 각 관계 키·ID가 유지되는지 확인했다. `CiRelationJobTest`는 SQL 문자열의 `ANY(m.device_fks)` 사용과 `DISTINCT ON` 부재를 확인한다. 원천(D42 .35, 수집 필터 적용) 재조회는 `pair_cnt=60, mountpoint_cnt=60`으로 다중 장비 표본이 없었고, 적재 후 `filesystem-array-fanout`도 0건이었다. Device→IP는 `view_ipaddress_device_v2`의 모든 쌍을 읽으며 `.35` 120건 중 Cluster 2건, `.68` 53건 중 Cluster 2건을 확인했다.

Host→VM의 CI 승격·이동 정리와 IP 기준정보, 관계 삭제·동시 실행·표시 미결은
[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에서 추적한다.
