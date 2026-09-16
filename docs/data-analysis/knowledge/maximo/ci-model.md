# CI 모델

> 관측 2026-09-04 · Maximo BLUDB
> 재조회 [구조](../../exploration-queries/maximo/ci-target-structure.sql),
> [참조 데이터](../../exploration-queries/maximo/ci-classification-audit.sql),
> [분류·관계 정의](../../exploration-queries/maximo/ci-classification-templates.sql),
> [관계 규칙](../../exploration-queries/maximo/ci-relation-rules.sql),
> [분류쌍별 규칙](../../exploration-queries/maximo/ci-definition-coverage.sql)

## 조사 범위

| 테이블 | 한글명 | 행 | 영속 컬럼 |
| --- | --- | ---: | ---: |
| ACTCI | 실제 CI 테이블 | 0 | 14 |
| ACTCISPEC | 실제 구성 품목 사양 테이블 | 0 | 18 |
| ACTCIRELATION | 실제 CI 관계 테이블 | 0 | 11 |
| CI | 구성 품목 테이블 | 11 | 36 |
| CISPEC | 구성 품목 사양 테이블 | 43 | 20 |
| CIRELATION | 연관된 CI 테이블 | 7 | 8 |

컬럼 수는 `MAXATTRIBUTE.PERSISTENT=1` 기준이며 `ROWSTAMP`는 제외했다.
Actual CI는 메타데이터만, CI는 메타데이터와 기존 행을 확인했다.
Actual CI 적재·UI 표시·재동기화는 검증하지 않았다.

## 참조 경로

`MAXRELATIONSHIP`에 등록된 조건이다. 아래 참조를 강제하는 물리 FK는 없다.

| 관계 | 조건 |
| --- | --- |
| Actual CI → 속성 | `ACTCISPEC.REFOBJECTID=ACTCI.ACTCIID` |
| Actual CI → 분류별 속성 | `ACTCISPEC.ACTCINUM=ACTCI.ACTCINUM AND ACTCISPEC.CLASSSTRUCTUREID=ACTCI.CLASSSTRUCTUREID` |
| Actual 속성 → 템플릿 | `ACTCISPEC.CLASSSPECID=CLASSSPEC.CLASSSPECID` |
| Actual 속성 → 속성 정의 | `ACTCISPEC.ASSETATTRID=ASSETATTRIBUTE.ASSETATTRID` |
| Actual 관계 → 양 끝점 | `SOURCECI/TARGETCI=ACTCI.ACTCINUM` |
| CI → 속성 | `CISPEC.REFOBJECTID=CI.CIID` |
| CI 속성 → 속성 정의 | `CISPEC.ASSETATTRIBUTEID=ASSETATTRIBUTE.ASSETATTRIBUTEID` |
| CI 관계 → 양 끝점 | `SOURCECI/TARGETCI=CI.CINUM` |
| CI ↔ Actual CI | `CI.ACTCINUM=ACTCI.ACTCINUM` |
| 두 관계 테이블 → 관계 정의 | `RELATIONNUM=RELATION.RELATIONNUM` |
| Actual 관계 → 분류쌍 규칙 | `RELATIONNUM`과 양 끝 ACTCI의 `CLASSSTRUCTUREID`를 `RELATIONRULES`에 대조 |

`CIRELATION.SOURCECI/TARGETCI`의 SameAs는 ACTCI를 가리키지만, 관계 조건은 CI를
조회한다. SameAs만으로 조인 대상을 판정할 수 없다.

## CI와 Actual CI의 차이

| 구분 | Actual CI | CI |
| --- | --- | --- |
| 본체 번호 / 물리 PK | ACTCINUM / ACTCIID | CINUM / CIID |
| 상태 | STATUS 없음 | STATUS, STATUSDATE 필수 |
| 자산 연결 | ASSETNUM 없음 | ASSETNUM, ASSETLOCORGID, ASSETLOCSITEID |
| 스캔 시각 | LASTSCANDT 필수 | 대응 컬럼 없음 |
| 사양 값 | ALNVALUE, NUMVALUE, TABLEVALUE | 왼쪽 + DATEVALUE |
| 사양 속성 키 | ASSETATTRID | ASSETATTRID + ASSETATTRIBUTEID |
| 관계 전용 컬럼 | BASELINEDATE, SOURCECIGUID, TARGETCIGUID, SWAPPED | PARENTCI |

Actual CI 전체 컬럼: [ACTCI](../../data-mapping/ci/actci.md),
[ACTCISPEC](../../data-mapping/ci/actcispec.md),
[ACTCIRELATION](../../data-mapping/ci/actcirelation.md).

## 기존 CI 데이터 검증

- CI 11건은 분류와 CI 적용 설정이 모두 존재한다. GENCSYS 5건, OS 6건이며
  STATUS는 모두 NOT READY, LANGCODE는 EN이다. ACTCINUM은 전건 NULL이다.
- CISPEC 43건은 부모 번호·ID, 부모 분류, 템플릿 ID·분류·속성·섹션이 모두 일치한다.
  MANDATORY와 DISPLAYSEQUENCE도 CI용 CLASSSPECUSEWITH와 전건 일치한다.
- CISPEC 값은 ALNVALUE 14건, NUMVALUE 10건, TABLEVALUE·DATEVALUE 0건이다.
- CIRELATION은 INSTALON 2건, RUNSON 5건이며 양 끝점이 모두 CI에 존재한다.
  PARENTCI·ANCESTORCI는 전건 NULL이다.
  다만 아래의 정확한 분류쌍 조건으로 RELATIONRULES를 조회하면 7건 모두 일치 규칙이 없다.

관계 정의 `RELATION`은 USEWITH=CI 119건, ASSET 9건이며 ACTCI는 0건이다.
CI 관계 119건은 모두 UNIDIRECTIONAL이고, 그중 112건에 CLASSSTRUCTUREID가 있다.
관계 코드 `RUNSON`과 `RELATION.RUNSON`은 별개 행이다.
USEWITH에 ACTCI가 없더라도 Actual CI용 분류쌍 규칙은 존재한다.

## RELATION과 RELATIONRULES

| 테이블 | 역할 | 행 |
| --- | --- | ---: |
| RELATION | 관계 종류·방향 정의 | 128 |
| RELATIONRULES | 관계 종류별 출발·도착 분류와 제약 | 116,685 |

ACTCIRELATION의 `ACTCIRELATIONRELATIONRULE` 관계 조건은 다음 세 값의 일치다.

- `RELATIONRULES.RELATIONNUM = ACTCIRELATION.RELATIONNUM`
- `RELATIONRULES.SOURCECLASS = 출발 ACTCI.CLASSSTRUCTUREID`
- `RELATIONRULES.TARGETCLASS = 도착 ACTCI.CLASSSTRUCTUREID`

출발 분류에 ACTCI 적용 설정이 있는 규칙은 114,342건이며, 양 끝 분류 모두
ACTCI 적용 설정이 있는 규칙은 114,270건이다. 분류별 적용 여부와 관계 코드를
함께 확인해야 한다. 단순 RELATION 코드 존재 여부와는 다른 검사다.

| 규칙 컬럼 | 한글 의미 |
| --- | --- |
| SOURCECLASS / TARGETCLASS | 출발 / 도착 분류 ID |
| RELATIONNUM | 관계 코드 |
| CARDINALITY | 카디널리티 |
| CONTAINMENT | 포함 관계 |
| REVRELATIONSHIP | 대상 상위 여부 |
| SWAPPED | 스왑 여부 |
| PROPAGATECHANGE | 변경 전파 |

### 확인된 분류쌍 규칙

아래 `RELATION.USEWITH`는 모두 CI다. 메타데이터 설정값이며 원천 관계의 매핑 확정은 아니다.

| 출발 분류 | RELATIONNUM | 도착 분류 | 카디널리티 | 포함 | 대상 상위 | 스왑 |
| --- | --- | --- | --- | --- | --- | --- |
| APP.DB.MSSQL.SQLSERVER | RELATION.CONTAINS | APP.DB.MSSQL.SQLSERVERDATABASE | 1:N | 1 | 0 | 0 |
| APP.DB.MSSQL.SQLSERVER | RELATION.RUNSON | SYS.COMPUTERSYSTEM | 1:1 | 0 | 0 | 0 |
| APP.DB.DATABASESERVER | RELATION.RUNSON | SYS.COMPUTERSYSTEM | 1:1 | 0 | 0 | 0 |
| SYS.VIRTUALCOMPUTERSYSTEM | RELATION.VIRTUALIZES | SYS.COMPUTERSYSTEM | 1:1 | 1 | 1 | 1 |
| SYS.COMPUTERSYSTEM | VIRTUALIZES | SYS.VIRTUALCOMPUTERSYSTEM | 1:N | 1 | 0 | 0 |
| SYS.VIRTUALCOMPUTERSYSTEM | VIRTUALIZES | SYS.VIRTUALCOMPUTERSYSTEM | 1:N | 1 | 0 | 0 |

RUNSON 두 행은 도착 분류가 SYS.VIRTUALCOMPUTERSYSTEM인 규칙도 존재한다.
APP.DB.DATABASE와 APP.DB.DATABASESERVER 사이에는 양방향 모두 직접 규칙이 없다.

분류쌍 설정의 상속 처리, 규칙 없는 관계의 UI 동작은 미검증이다.
[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 관계 방향·카디널리티 적용과 기준정보 보완 여부 미정.

## 물리 DB와 Maximo 메타데이터

### CI 식별자 문자열 저장 확인 — 2026-09-14

재조회: [식별자 타입·제약·자동 번호](../../exploration-queries/maximo/ci-identifier-storage.sql).
결과: `local/db-access-kit/work/ci-identifier-storage-20260914/`.

| 필드 | 실제 DB2 타입 | Maximo 타입 | 확인된 역할 |
| --- | --- | --- | --- |
| ACTCI.ACTCIID | BIGINT | BIGINT | 숫자 PK, IDENTITY 아님 |
| ACTCI.ACTCINUM | VARGRAPHIC(150) | UPPER(150) | 문자열 번호, 고유 인덱스 ACTCI_NDX1 |
| ACTCISPEC.ACTCINUM | VARGRAPHIC(150) | UPPER(150) | 본체 번호 참조 |
| ACTCIRELATION.SOURCECI / TARGETCI | VARGRAPHIC(150) | UPPER(150) | 관계 양 끝의 본체 번호 참조 |
| CI.ACTCINUM | VARGRAPHIC(150) | 기존 CI의 Actual CI 참조 | 실제 저장 타입은 문자열 |

ACTCI·ACTCISPEC·ACTCIRELATION에는 CHECK 제약이 없고, 조회된 INSERT/UPDATE 트리거는 ROWSTAMP만 채번한다.
ACTCINUM의 DOMAINID는 비어 있다. 확인한 DB 타입·제약에서 숫자 전용이나 콜론 금지 조건은 없다.
AUTOKEYNAME='ACTCINUM'의 설정은 PREFIX 공란·SEED=1000이며, 이는 숫자형 DB 컬럼이라는 뜻이 아니다.

ACTCINUM에는 `com.ibm.ism.cci.app.actualci.CCIFldActCINum` 속성 클래스가 연결돼 있다.
해당 클래스의 구현은 로컬 저장소에서 확인되지 않았다.
따라서 문자열 저장 타입과 고유성은 확인했지만, 특정 번호 형식의 MBO·UI 검증과 자동 번호 공존은 미검증이다.
이 조사는 읽기 전용이며 실제 INSERT·UI 저장 시험을 수행하지 않았다.

사용자 시험 보고(2026-09-14 기록): 임의 문자열 ACTCINUM을 INSERT한 테스트 데이터가
화면에 정상 표시되고 CI 승격까지 성공했다고 확인했다. 정확한 문자열은 제공되지 않았으며,
에이전트의 직접 재현 결과나 `D42:DEVICE:100` 형식 자체의 시험 결과로 간주하지 않는다.

### 공통 저장 메타데이터

- Actual CI 세 테이블의 ID는 물리 PK이며 IDENTITY가 아니다.
  MAXSEQUENCE에 ACTCISEQ·ACTCISPECSEQ·ACTCIRELATIONSEQ가 연결되어 있다.
  세 시퀀스는 START=1, INCREMENT=1000이다. START는 현재 다음 값이 아니다.
- ACTCINUM은 별도 AUTOKEY 설정이다. 숫자 ACTCIID와 같지 않다.
- 세 테이블의 INSERT/UPDATE 트리거가 MAXSEQ로 ROWSTAMP를 설정한다.
- HASLD·MANDATORY·SWAPPED의 메타데이터 기본값은 0이지만 물리 DEFAULT는 없다.
  JDBC 적재에서 Maximo 기본값이 자동 적용된다고 볼 수 없다.
- ACTCISPEC.NUMVALUE는 Maximo DECIMAL(30,5), 물리 DECIMAL(30,10)이다.
  매핑표는 Maximo 기준을 사용한다.

분류·템플릿 구조와 적용 범위는 [CI 분류 모델](ci-classification.md) 참조.
