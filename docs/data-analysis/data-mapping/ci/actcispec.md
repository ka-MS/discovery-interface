# ACTCISPEC

실제 구성 품목 사양 테이블

> Target: MAXIMO.ACTCISPEC · 공통 규약 / 속성 선택은 유형 문서
> 관측 2026-09-04 · Maximo BLUDB
> 구조 분석: [CI 모델](../../knowledge/maximo/ci-model.md), [CI 분류 모델](../../knowledge/maximo/ci-classification.md)

## 1. 관계

- 물리 PK: `ACTCISPECID`; 고유 인덱스: `(ASSETATTRID, ACTCINUM, SECTION)`.
- 부모: `ACTCI` 1 : N `ACTCISPEC`; `SECTION`은 nullable이다.
- 부모·분류·템플릿 참조는 [CI 모델](../../knowledge/maximo/ci-model.md) 참조.

## 2. 테이블 매핑

[분류·스펙 명세](classstructure.md)가 전체 수집 ASSETATTRID와 적용 분류를, 유형별 문서가 값 원천을 정한다. 템플릿 구조는 [CI 분류 모델](../../knowledge/maximo/ci-classification.md)에 있다.

공통 표의 별칭은 `b=저장된 부모 ACTCI`, `c=선택한 CLASSSPEC`,
`u=CLASSSPECUSEWITH`다. c는 부모 분류·ASSETATTRID·SECTION으로 찾고,
u는 `u.CLASSSPECID=c.CLASSSPECID AND u.OBJECTNAME='ACTCI'`로 찾는다.
NULL 섹션끼리는 같게 비교한다. 참조가 0건 또는 여러 건이면 임의의 첫 행을 선택하지 않는다.
현재 Mapper는 ALN/String과 NUMERIC/BigDecimal만 처리한다. ALNVALUE·NUMVALUE 중 한 컬럼을 사용하며 TABLEVALUE는 Writer가 항상 NULL로 쓴다.
해당 분류의 정상 CLASSSPEC은 원천 값이 없어도 행을 만들어 값 컬럼을 NULL로 동기화한다.
템플릿이 없는 명시적 추가 속성은 원천 값이 있을 때만 행을 만든다.

Computer의 명시적 추가 속성은 예외다. 해당 분류 템플릿이 없을 때 전역 ASSETATTRIBUTE를
확인하여 CLASSSPECID=NULL로 적재할 수 있다. 표시 순서·필수 여부·허용 대상은
[Device 매핑](types/device.md)의 추가 속성 규칙을 따른다.

## 3. 조회 조건

유형 문서의 ASSETATTRID·SECTION과 부모 분류로 템플릿을 조회한다.
ASSETATTRIBUTE의 ID·속성명이 모두 일치하고 ACTCI용 적용 설정이 있어야 한다.
현재 여섯 CI 유형이 ALN·NUMERIC 속성을 선택한다. DB Instance는 네 분류에 같은 5개 ALN을 사용하고 독립 Database는 구현하지 않는다.
실제 조회·유효성 검사·추가 속성 예외는 [분류·스펙 명세](classstructure.md)를 따른다.

## 4. 컬럼 매핑

영속 18개 컬럼이다. 앞 4열은 Maximo 메타데이터다. 부모·템플릿 참조는 공통으로
적용하며 현재 바인딩과 INSERT/UPDATE 동작을 아래에 구분한다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCINUM | 실제 구성 품목 번호 | UPPER(150) | N | 직접 | `b.ACTCINUM` | 부모와 동일 |
| ACTCISPECID | 고유 ID | BIGINT(19) | N | 채번 | MAXIMO.ACTCISPECSEQ | INSERT에서 NEXT VALUE FOR; 동일 ACTCINUM·ASSETATTRID·SECTION의 기존 ID 유지 |
| ALNVALUE | 영숫자 값 | ALN(254) | Y | 직접 | 유형 문서의 ALN 속성 원천 | ASSETATTRID별 대응은 유형 문서; 254자 초과 시 절단하지 않음 |
| ASSETATTRID | 속성 | UPPER(300) | N | 직접 | `c.ASSETATTRID` | 유형 문서가 선택한 속성 |
| CHANGEBY | 변경자 | UPPER(100) | N | 상수 | 부모 DTO | `Device42`; 부모와 같은 값 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 변환 | 부모 DTO | 부모와 같은 페이지 매핑 시각; 원천 변경 시각 아님 |
| CLASSSPECID | ClassSpec ID | BIGINT(19) | Y | 직접 | `c.CLASSSPECID` | 기존 템플릿 ID 사용. 명시적 추가 속성은 NULL |
| CLASSSTRUCTUREID | 클래스 구조 | UPPER(25) | N | 직접 | `b.CLASSSTRUCTUREID` | `c.CLASSSTRUCTUREID`와 일치 |
| DISPLAYSEQUENCE | 순서 표시 | SMALLINT(10) | N | 직접 | `u.SEQUENCE` 또는 유형별 추가 설정 | 기존 속성은 ACTCI용 설정, 추가 속성은 명시한 순서 |
| LINKEDTOATTRIBUTE | 속성에 연결됨 | UPPER(300) | Y | 직접 | `c.LINKEDTOATTRIBUTE` | NULL 유지 |
| LINKEDTOSECTION | 섹션에 연결됨 | UPPER(10) | Y | 직접 | `c.LINKEDTOSECTION` | NULL 유지 |
| MANDATORY | 필수적 | YORN(1) | N | 직접 | `u.MANDATORY` 또는 유형별 추가 설정 | 설정값을 저장하며 현재 Computer 코드는 필수 여부에 따른 값 검증을 하지 않음 |
| MEASUREUNITID | 측정 단위 | UPPER(16) | Y | 변환 | 유형별 명시적 단위 매핑 또는 `c.MEASUREUNITID` | Computer 메모리·속도는 값과 원천 단위를 함께 매핑. 명시적 규칙이 없으면 템플릿 단위 사용 |
| NUMVALUE | 숫자 값 | DECIMAL(30,5) | Y | 변환 | 유형 문서의 NUMERIC 속성 원천 | Computer 메모리·CPU 수·속도·코어 수, Disk 크기, Filesystem 용량·가용량. ALN 속성은 NULL; Writer CAST는 DECIMAL(30,10) |
| REFOBJECTID | 참조 오브젝트 ID | BIGINT(19) | Y | 직접 | `b.ACTCIID` | 원천 PK가 아닌 저장된 부모 ID |
| REFOBJECTNAME | 참조 오브젝트 이름 | UPPER(30) | Y | 상수 | – | `'ACTCI'` |
| SECTION | 섹션 | UPPER(10) | Y | 직접 | `c.SECTION` | NULL 유지 |
| TABLEVALUE | 테이블 값 | ALN(254) | Y | 상수 | – | 현재 전체 유형에서 INSERT·UPDATE 모두 NULL. TABLE 자료형으로 값을 매핑하지 않음 |

## 5. 실제 저장 SQL

구현: [ActCiWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/ci/ActCiWriter.java).
MERGE 키는 (ACTCINUM,ASSETATTRID,SECTION)이며 NULL SECTION끼리도 일치시킨다.
부모 ACTCIID는 ACTCINUM으로 조회한다. 바인딩 순서는
actCiNum, assetAttrId, section, classStructureId, classSpecId, actCiNum(부모 조회),
displaySequence, mandatory(0/1), measureUnitId, linkedToAttribute, linkedToSection,
alnValue, numValue, changeBy, changeDate다.

```sql
MERGE INTO MAXIMO.ACTCISPEC AS target
USING (VALUES (
    CAST(? AS VARCHAR(151)), CAST(? AS VARCHAR(300)), CAST(? AS VARCHAR(10)),
    CAST(? AS VARCHAR(25)), CAST(? AS BIGINT),
    (SELECT actci.ACTCIID FROM MAXIMO.ACTCI AS actci
     WHERE actci.ACTCINUM = ?),
    CAST(? AS INTEGER), CAST(? AS INTEGER), CAST(? AS VARCHAR(16)),
    CAST(? AS VARCHAR(300)), CAST(? AS VARCHAR(10)), CAST(? AS VARCHAR(254)),
    CAST(? AS DECIMAL(30,10)), CAST(? AS VARCHAR(100)), CAST(? AS TIMESTAMP)
)) AS source (
    ACTCINUM, ASSETATTRID, SECTION, CLASSSTRUCTUREID, CLASSSPECID,
    REFOBJECTID, DISPLAYSEQUENCE, MANDATORY, MEASUREUNITID,
    LINKEDTOATTRIBUTE, LINKEDTOSECTION, ALNVALUE, NUMVALUE, CHANGEBY, CHANGEDATE
)
ON target.ACTCINUM = source.ACTCINUM
    AND target.ASSETATTRID = source.ASSETATTRID
    AND (target.SECTION = source.SECTION
         OR (target.SECTION IS NULL AND source.SECTION IS NULL))
WHEN MATCHED THEN
    UPDATE SET
        CLASSSTRUCTUREID = source.CLASSSTRUCTUREID,
        CLASSSPECID = source.CLASSSPECID,
        REFOBJECTID = source.REFOBJECTID,
        REFOBJECTNAME = 'ACTCI',
        DISPLAYSEQUENCE = source.DISPLAYSEQUENCE,
        MANDATORY = source.MANDATORY,
        MEASUREUNITID = source.MEASUREUNITID,
        LINKEDTOATTRIBUTE = source.LINKEDTOATTRIBUTE,
        LINKEDTOSECTION = source.LINKEDTOSECTION,
        ALNVALUE = source.ALNVALUE,
        NUMVALUE = source.NUMVALUE,
        TABLEVALUE = NULL,
        CHANGEBY = source.CHANGEBY,
        CHANGEDATE = source.CHANGEDATE
WHEN NOT MATCHED THEN
    INSERT (
        ACTCISPECID, ACTCINUM, ASSETATTRID, CLASSSTRUCTUREID, CLASSSPECID,
        SECTION, REFOBJECTID, REFOBJECTNAME, DISPLAYSEQUENCE, MANDATORY,
        MEASUREUNITID, LINKEDTOATTRIBUTE, LINKEDTOSECTION, ALNVALUE,
        NUMVALUE, TABLEVALUE, CHANGEBY, CHANGEDATE
    ) VALUES (
        NEXT VALUE FOR MAXIMO.ACTCISPECSEQ,
        source.ACTCINUM, source.ASSETATTRID, source.CLASSSTRUCTUREID,
        source.CLASSSPECID, source.SECTION, source.REFOBJECTID, 'ACTCI',
        source.DISPLAYSEQUENCE, source.MANDATORY, source.MEASUREUNITID,
        source.LINKEDTOATTRIBUTE, source.LINKEDTOSECTION, source.ALNVALUE,
        source.NUMVALUE, NULL, source.CHANGEBY, source.CHANGEDATE
    )
```

Mapper가 생성한 스펙만 MERGE한다. 원천 값이 없는 정상 템플릿은 NULL로 갱신하지만
정의·자료형·단위 문제로 생략한 스펙은 기존 행을 삭제하거나 초기화하지 않는다.
스펙 저장 실패는 로그를 남기고 다음 스펙으로 계속한다. 본체 적재는 취소하지 않는다.

## 6. 후속 정책

현재 실행 규칙과 미수집·승격 관련 후속 정책을 구분한다.
정책 상태는 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)을 따른다.
