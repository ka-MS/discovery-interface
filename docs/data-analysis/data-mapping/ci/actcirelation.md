# ACTCIRELATION

실제 CI 관계의 공통 Target 매핑이다. 유형별 연결 의미와 원천 SQL은 출발 유형 문서가 소유한다.

> 메타데이터 재조회: 2026-09-15 · MAXIMO / BLUDB.
> 관계 설계: [Computer 중심 관계](../../design/ci/relations.md).
> 관측 근거: [관계 정의](../../knowledge/maximo/computer-ci-relations.md).
> 아래 공통 MERGE는 미실행 초안이다. OS → 물리 Computer 한 쌍은 **별도 INSERT·CI 승격·UI 표시를 검증했다**. [샘플 결과](../../knowledge/maximo/computer-ci-relations.md#oscomputer-승격-샘플-검증).

## 1. 관계 키

- 물리 PK: ACTCIRELATIONID.
- MERGE 키: **(SOURCECI, TARGETCI, RELATIONNUM)**. 실제 고유 인덱스와 동일하다.
- SOURCECI/TARGETCI: 각 ACTCI.ACTCINUM. 숫자 ACTCIID나 원천 PK만 넣지 않는다.
- relationnum은 정확한 RELATION 행의 코드. RELATION.CONTAINS와 CONTAINS를 동일시하지 않는다.
- 양 끝의 실제 CLASSSTRUCTUREID와 관계 코드로 RELATIONRULES를 대조한다.
- 물리 FK가 없으므로 DB가 양 끝 존재를 자동 보장하지 않는다.

Computer→Disk·Filesystem은 [Computer](types/computer.md#7-관계-매핑--2026-09-15),
OS→Computer는 [OS](types/os.md#6-관계-매핑--2026-09-15)를 참조한다.
DB Instance→DB의 기존 별도 검토는 [DB Instance](types/database-instance.md)를 유지하며,
Computer 관계 조사 결과를 DB 관계의 승인으로 해석하지 않는다.

## 2. 컬럼 매핑

공통 11개 영속 컬럼이다. 신규 Computer 관련 관계에 적용할 제안이며,
기존 관계를 일괄 갱신하거나 선택하지 않은 관계를 생성하지 않는다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCIRELATIONID | 고유 ID | BIGINT(19) | N | 채번 | MAXIMO.ACTCIRELATIONSEQ | INSERT에서 NEXT VALUE. 기존 행 ID 유지. 증가폭 사이 번호 임의 사용 금지 |
| SOURCECI | 소스 실제 구성 품목 번호 | UPPER(150) | N | 변환 | 유형별 원천 연결 키 | 유형 매핑의 출발 ACTCINUM |
| TARGETCI | 대상 실제 구성 품목 번호 | UPPER(150) | N | 변환 | 유형별 원천 연결 키 | 유형 매핑의 도착 ACTCINUM |
| RELATIONNUM | 관계 | UPPER(192) | N | 상수 | 확정할 CiRelationRule | RELATION·RELATIONRULES에서 확인된 정확한 코드 |
| SWAPPED | 스왑됨 | YORN(1) | Y | 상수 | 사전 정의한 저장 방향 | OS → 물리 Computer는 0으로 단건 승격·표시 확인. 다른 관계는 검증 필요. 규칙 SWAPPED를 그대로 복사하지 않음 |
| CHANGEBY | 변경자 | UPPER(100) | Y | 상수 | 기존 CI 적재 규약 | Device42 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | Y | 변환 | 관계 매핑 시각 | 기존 CI와 같은 JVM 기본 시간대. 원천 발견 시각이 아님 |
| ANCESTORCI | 상위 실제 CI | UPPER(150) | Y | 원천없음 | 별도 상위 원천 없음 | 신규 NULL. SOURCECI를 무조건 복사하지 않음 |
| BASELINEDATE | 기준선 날짜 | DATETIME(10) | Y | 원천없음 | D42 관계 기준선 없음 | 신규 NULL |
| SOURCECIGUID | 소스 실제 CI GUID | ALN(192) | Y | 미결 | ACTCI.GUID | 이번 최소안 신규 NULL. GUID 생성·복사 규칙 추가 시 별도 검증 |
| TARGETCIGUID | 대상 실제 CI GUID | ALN(192) | Y | 미결 | ACTCI.GUID | 이번 최소안 신규 NULL. 기존 값은 MERGE에서 유지 |

CONTAINMENT·REVRELATIONSHIP·CARDINALITY는 ACTCIRELATION 컬럼이 아니다.
SWAPPED는 저장 순서 변경 여부를 나타내는 별도 필드이며
[관측 설명](../../knowledge/maximo/computer-ci-relations.md#플래그-의미와-주의)을 따른다.

## 3. 공통 저장 SQL 제안

호출자가 승인된 규칙과 실제 원천 연결로 sourceci/targetci를 만든다.
캐시에서 얻은 **예상 출발·도착 CLASSSTRUCTUREID**를 함께 전달한다.
동일 관계 입력 중복은 세 컬럼 키로 제거하고, 양 끝과 규칙 확인은 MERGE의 USING 안에서 한다.

```sql
MERGE INTO MAXIMO.ACTCIRELATION AS target
USING (
    SELECT input.SOURCECI,input.TARGETCI,input.RELATIONNUM,
           input.SWAPPED,input.CHANGEBY,input.CHANGEDATE
    FROM (VALUES (
        CAST(? AS VARCHAR(150)), CAST(? AS VARCHAR(150)),
        CAST(? AS VARCHAR(192)), CAST(? AS VARCHAR(25)),
        CAST(? AS VARCHAR(25)), CAST(? AS INTEGER),
        CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
    )) AS input (
        SOURCECI,TARGETCI,RELATIONNUM,
        SOURCECLASS,TARGETCLASS,SWAPPED,CHANGEBY,CHANGEDATE
    )
    JOIN MAXIMO.ACTCI s ON s.ACTCINUM=input.SOURCECI
                          AND s.CLASSSTRUCTUREID=input.SOURCECLASS
    JOIN MAXIMO.ACTCI t ON t.ACTCINUM=input.TARGETCI
                          AND t.CLASSSTRUCTUREID=input.TARGETCLASS
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

파라미터 순서는 sourceCiNum, targetCiNum, relationNum, expectedSourceClassId,
expectedTargetClassId, swapped, changeBy, changeDate다.
선정된 기존 규칙만 사용한다는 프로젝트 정책을 SQL에 반영한 것이며,
Maximo의 모든 관계 적재 방식에 대한 제약이라고 주장하지 않는다.

- 부모/자식 존재 확인을 별도 SELECT로 반복할 필요는 없다.
- 규칙 조인을 EXISTS로 처리해 규칙 행 중복 때문에 한 관계 후보가 여러 행으로 늘어나지 않게 한다.
- 0건은 양 끝 미존재·분류 불일치·규칙 부재 가능성을 뜻한다. 최초 로그에 한 원인으로 단정하지 않는다.
- 저장 예외는 해당 관계를 기록하고 다음 관계로 진행하는 안이다.
- 가드는 현재 저장 상태를 확인한다. 외부 동시 삭제까지 물리 FK처럼 보장하지 않는다.
- MERGE 키가 달라진 새 관계는 추가된다. 이전 호스트·장비 관계 삭제는 자동 수행하지 않는다.
- 신규 NULL 필드들은 기존 행에서 덮어쓰지 않는다.

## 4. 검증 수준과 후속

D42의 논리키 관계 SELECT와 Maximo의 분류쌍·키·참조 메타데이터는 실조회했다.
공통 MERGE는 **실행하지 않은 초안**이다. 구현할 때 다음을 확인한다.

1. 양 끝 정상/누락/분류 불일치/규칙 부재별 적재 여부.
2. 재실행 시 관계 한 행 유지와 ACTCIRELATIONID 보존.
3. OS → 물리 Computer 단건은 SWAPPED=0, CI 승격 후 관계 방향·부모 보존 확인. 다른 분류쌍·복수 관계·탐색은 추가 검증.
4. 관계 하나의 저장 실패 뒤 나머지 관계 계속 처리.
5. 같은 유형의 뒤쪽 배치에 호스트가 있는 경우 관계 누락 방지.
6. 여러 장비 배열·여러 IP를 첫 번째 하나로 줄이지 않는지.

VM·IP의 기준정보, 이동·삭제·동시 실행·표시 및 GUID 처리 미결은
[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에서 추적한다.
