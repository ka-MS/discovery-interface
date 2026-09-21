# ACTCI

실제 CI 테이블

> Target: MAXIMO.ACTCI · 공통 규약 / 원천 필드는 유형 문서
> 관측 2026-09-04 · Maximo BLUDB
> 구조 분석: [CI 모델](../../knowledge/maximo/ci-model.md)

## 1. 관계

- 물리 PK: `ACTCIID`; 고유 인덱스: `ACTCINUM`.
- 분류: `CLASSSTRUCTUREID` → `CLASSSTRUCTURE.CLASSSTRUCTUREID`.
- 자식: `ACTCISPEC.REFOBJECTID=ACTCI.ACTCIID`.
- CI 연결: `CI.ACTCINUM=ACTCI.ACTCINUM`; `CI.CINUM`과는 별도다.

## 2. 테이블 매핑

현재 구현은 [Device](types/device.md), [OS](types/os.md), [Disk](types/disk.md), [Filesystem](types/filesystem.md), [IP](types/ip.md), [DB Instance](types/database-instance.md) 여섯 유형이다. 독립 Database는 구현하지 않는다.
후보 범위와 중복 근거는 [ci-targets.md](ci-targets.md) 참조.
본체를 먼저 식별·저장하고 저장된 ACTCIID·ACTCINUM을 속성과 관계에서 참조한다.
원천 SQL의 반환 컬럼과 변환식은 각 유형 문서에 작성한다. DB Instance 문자열의 trim·빈 값 NULL 처리는 SQL에서 수행하며 공통 Writer가 추가 정규화하지 않는다.

## 3. 조회 조건

범위·COUNT·정렬·LIMIT/OFFSET은 각 유형의 원천 SQL을 따른다. 분류 선택과 정의 조회는
[분류·스펙 명세](classstructure.md), 본체·관계 공통 식별자 접두어도 같은 문서의 전체 대응표를 따른다.

## 4. 컬럼 매핑

영속 14개 컬럼이다. 앞 4열은 Maximo 메타데이터다. 유형별 변환 결과를 받는
컬럼과 Writer가 전혀 쓰지 않는 컬럼을 구분한다. 메타데이터의 필수 표기와 실제 DB 제약은 같다고 단정하지 않는다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCIID | 고유 ID | BIGINT(19) | N | 채번 | MAXIMO.ACTCISEQ | INSERT에서 NEXT VALUE FOR; ACTCINUM 일치 시 기존 ID 유지 |
| ACTCINAME | 실제 구성 품목 이름 | UPPER(192) | Y | 직접 | 유형별 원천 이름: Computer는 `view_device_v2.name` | 임의 대문자 변환·절단 없음; DB·Instance 원천은 각 유형 문서 |
| ACTCINUM | 실제 구성 품목 번호 | UPPER(150) | N | 변환 | `D42:<원천 개체 종류>:<원천 PK>` | 문자열 sourceId 저장으로 합의. Computer는 DEVICE; 원천 개체 종류는 유형별 정의 |
| CCIDISGUID | 통합 ID | ALN(192) | Y | 원천없음 | 현재 매핑 없음 | Writer의 INSERT·UPDATE에 포함하지 않음. 신규 값은 DB 기본값/제약에 따르며 기존 값 유지 |
| CHANGEBY | 변경자 | UPPER(100) | Y | 상수 | – | `Device42`; 사용자 계정 유효성은 코드에서 검사하지 않음 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | Y | 변환 | 페이지 매핑 시각 | LocalDateTime.now(); 해당 페이지의 본체·스펙이 공유 |
| CLASSSTRUCTUREID | 분류 | UPPER(25) | Y | 변환 | `MAXIMO.CLASSSTRUCTURE.CLASSSTRUCTUREID` | 유형별 분류 조회 SQL; ACTCI 적용 설정 확인 |
| DESCRIPTION | 설명 | ALN(1024) | Y | 직접 | Device는 장비 notes, DB Instance는 Resource notes, Disk는 파트 description, IP는 IP notes; OS·Filesystem은 NULL | 유형별 SQL의 정규화를 따르며 Writer의 임의 절단 없음 |
| EXTENDEDINSTANCES | ExtendedInstances | CLOB(999999) | Y | 원천없음 | 현재 매핑 없음 | Writer의 INSERT·UPDATE에 포함하지 않음. 신규 값은 DB 기본값/제약에 따르며 기존 값 유지 |
| GUID | 발견 ID | ALN(192) | Y | 원천없음 | 현재 매핑 없음 | Writer의 INSERT·UPDATE에 포함하지 않음. 신규 값은 DB 기본값/제약에 따르며 기존 값 유지 |
| HASLD | 상세 설명 있음 | YORN(1) | N | 상수 | – | 신규 INSERT는 0, 기존 행은 UPDATE 대상이 아니므로 유지 |
| LANGCODE | 언어 코드 | UPPER(4) | N | 상수 | – | `KO`; INSERT·UPDATE에 적용 |
| LASTSCANDT | 최종 스캔 날짜 | DATETIME(10) | N | 변환 | 유형별 발견·변경 시각 | SourceTimestamp로 JVM 기본 시간대 변환. 미보유는 NULL 전달, 파싱 예외는 해당 CI 제외. 필수값을 임의로 생성하지 않으며 DB 거부 시 본체 실패 처리 |
| PLUSPCUSTOMER | 기본 고객 | UPPER(12) | Y | 원천없음 | 현재 매핑 없음 | Writer의 INSERT·UPDATE에 포함하지 않음. 신규 값은 DB 기본값/제약에 따르며 기존 값 유지 |

## 5. 실제 저장 SQL

구현: [ActCiWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/ci/ActCiWriter.java).
MERGE 키는 ACTCINUM이다. 바인딩 순서는 actCiNum, actCiName, classStructureId,
description, lastScanDate, changeBy, changeDate, langCode다.
아래 CAST 길이는 실제 Writer 값이며 4절 메타데이터 길이로 임의 교정하지 않는다.

```sql
MERGE INTO MAXIMO.ACTCI AS target
USING (VALUES (
    CAST(? AS VARCHAR(151)), CAST(? AS VARCHAR(193)), CAST(? AS VARCHAR(26)),
    CAST(? AS VARCHAR(1025)), CAST(? AS TIMESTAMP), CAST(? AS VARCHAR(101)),
    CAST(? AS TIMESTAMP), CAST(? AS VARCHAR(5))
)) AS source (
    ACTCINUM, ACTCINAME, CLASSSTRUCTUREID, DESCRIPTION,
    LASTSCANDT, CHANGEBY, CHANGEDATE, LANGCODE
)
ON target.ACTCINUM = source.ACTCINUM
WHEN MATCHED THEN
    UPDATE SET
        CLASSSTRUCTUREID = source.CLASSSTRUCTUREID,
        ACTCINAME = source.ACTCINAME,
        DESCRIPTION = source.DESCRIPTION,
        LASTSCANDT = source.LASTSCANDT,
        CHANGEBY = source.CHANGEBY,
        CHANGEDATE = source.CHANGEDATE,
        LANGCODE = source.LANGCODE
WHEN NOT MATCHED THEN
    INSERT (
        ACTCIID, ACTCINUM, ACTCINAME, CLASSSTRUCTUREID, DESCRIPTION,
        LASTSCANDT, CHANGEBY, CHANGEDATE, LANGCODE, HASLD
    ) VALUES (
        NEXT VALUE FOR MAXIMO.ACTCISEQ,
        source.ACTCINUM, source.ACTCINAME, source.CLASSSTRUCTUREID,
        source.DESCRIPTION, source.LASTSCANDT, source.CHANGEBY,
        source.CHANGEDATE, source.LANGCODE, 0
    )
```

본체 저장 실패(DataAccessException) 시 해당 CI의 스펙은 건너뛰고 다음 CI를 처리한다.
본체 성공 건수가 Writer 반환값이며 스펙의 완전한 성공을 뜻하지 않는다.
스펙 한 건의 실패는 본체와 다른 스펙을 롤백하지 않는다. 자동 삭제·분류 변경 후 옛 스펙 청소는 하지 않는다.

## 6. 후속 정책

현재 식별자·상수·저장 규칙은 위 구현으로 확정되어 있다. 확장·승격·미수집 항목에 대한
후속 정책은 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에서 구분한다.
