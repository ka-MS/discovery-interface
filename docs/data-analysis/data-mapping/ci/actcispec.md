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

유형별 문서가 ASSETATTRID와 값 원천을 정한다. [Computer](types/computer.md), [DB](types/database.md),
[DB Instance](types/database-instance.md) 참조. 템플릿 구조는 [CI 분류 모델](../../knowledge/maximo/ci-classification.md)에 있다.

공통 표의 별칭은 `b=저장된 부모 ACTCI`, `c=선택한 CLASSSPEC`,
`u=CLASSSPECUSEWITH`다. c는 부모 분류·ASSETATTRID·SECTION으로 찾고,
u는 `u.CLASSSPECID=c.CLASSSPECID AND u.OBJECTNAME='ACTCI'`로 찾는다.
NULL 섹션끼리는 같게 비교한다. 참조가 0건 또는 여러 건이면 임의의 첫 행을 선택하지 않는다.
DATATYPE에 맞는 ALNVALUE·NUMVALUE·TABLEVALUE 중 한 컬럼에 값을 넣고 나머지는 NULL로 둔다.
속성 행 자체를 만들지 않는 조건은 ISSUE-11에서 별도로 결정한다.

Computer의 명시적 추가 속성은 예외다. 해당 분류 템플릿이 없을 때 전역 ASSETATTRIBUTE를
확인하여 CLASSSPECID=NULL로 적재할 수 있다. 표시 순서·필수 여부·허용 대상은
[Computer 매핑](types/computer.md)의 추가 속성 규칙을 따른다.

## 3. 조회 조건

유형 문서의 ASSETATTRID·SECTION과 부모 분류로 템플릿을 조회한다.
ASSETATTRIBUTE의 ID·속성명이 모두 일치하고 ACTCI용 적용 설정이 있어야 한다.
DB 1개·Instance 3개 속성을 선택했으며 전부 ALN이다. DB 속성의 적용 설정은 보완 전이다.
Computer는 ALN·NUMERIC을 사용하며 추가 속성의 미등록 상태는 유형 문서에 명시한다.

## 4. 컬럼 매핑

영속 18개 컬럼이다. 앞 4열은 Maximo 메타데이터다. 부모·템플릿 참조는 공통으로
적용하며 속성 값과 미확정 적재 정책은 아래에 구분한다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCINUM | 실제 구성 품목 번호 | UPPER(150) | N | 직접 | `b.ACTCINUM` | 부모와 동일 |
| ACTCISPECID | 고유 ID | BIGINT(19) | N | 미결 | Maximo 채번 | ACTCISPECSEQ 예약·발번 정책 ISSUE-11 |
| ALNVALUE | 영숫자 값 | ALN(254) | Y | 직접 | 유형 문서의 ALN 속성 원천 | ASSETATTRID별 대응은 유형 문서; 254자 초과 시 절단하지 않음 |
| ASSETATTRID | 속성 | UPPER(300) | N | 직접 | `c.ASSETATTRID` | 유형 문서가 선택한 속성 |
| CHANGEBY | 변경자 | UPPER(100) | N | 원천없음 | D42 해당 속성의 변경자 미제공 | ETL 계정 정책 ISSUE-11 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 미결 | 원천 후보: `view_resource_v2.last_changed` | 속성별 변경 시각 아님; ETL 시각 적용 여부 ISSUE-11 |
| CLASSSPECID | ClassSpec ID | BIGINT(19) | Y | 직접 | `c.CLASSSPECID` | 기존 템플릿 ID 사용. 명시적 추가 속성은 NULL |
| CLASSSTRUCTUREID | 클래스 구조 | UPPER(25) | N | 직접 | `b.CLASSSTRUCTUREID` | `c.CLASSSTRUCTUREID`와 일치 |
| DISPLAYSEQUENCE | 순서 표시 | SMALLINT(10) | N | 직접 | `u.SEQUENCE` 또는 유형별 추가 설정 | 기존 속성은 ACTCI용 설정, 추가 속성은 명시한 순서 |
| LINKEDTOATTRIBUTE | 속성에 연결됨 | UPPER(300) | Y | 직접 | `c.LINKEDTOATTRIBUTE` | NULL 유지 |
| LINKEDTOSECTION | 섹션에 연결됨 | UPPER(10) | Y | 직접 | `c.LINKEDTOSECTION` | NULL 유지 |
| MANDATORY | 필수적 | YORN(1) | N | 직접 | `u.MANDATORY` 또는 유형별 추가 설정 | 설정값을 저장하며 현재 Computer 코드는 필수 여부에 따른 값 검증을 하지 않음 |
| MEASUREUNITID | 측정 단위 | UPPER(16) | Y | 변환 | 유형별 명시적 단위 매핑 또는 `c.MEASUREUNITID` | Computer 메모리·속도는 값과 원천 단위를 함께 매핑. 명시적 규칙이 없으면 템플릿 단위 사용 |
| NUMVALUE | 숫자 값 | DECIMAL(30,5) | Y | 변환 | 유형 문서의 NUMERIC 속성 원천 | Computer 메모리·CPU 수·속도·코어 수. ALN 속성은 NULL; 물리 DB는 DECIMAL(30,10) |
| REFOBJECTID | 참조 오브젝트 ID | BIGINT(19) | Y | 직접 | `b.ACTCIID` | 원천 PK가 아닌 저장된 부모 ID |
| REFOBJECTNAME | 참조 오브젝트 이름 | UPPER(30) | Y | 상수 | – | `'ACTCI'` |
| SECTION | 섹션 | UPPER(10) | Y | 직접 | `c.SECTION` | NULL 유지 |
| TABLEVALUE | 테이블 값 | ALN(254) | Y | 상수 | – | 현재 DB·Instance의 선택 속성은 모두 ALN이므로 NULL |

## 5. SQL

원천 값과 실제 템플릿 조회 SQL은 유형 문서 본문에 있다.
템플릿 조회는 적용 설정·속성 ID 연결이 없는 행을 정상 템플릿으로 반환하지 않는다.
부모 식별자·필수값 정책이 미결이므로 INSERT/MERGE SQL은 아직 확정하지 않았다.

## 6. 미결

[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 분류·속성·관계 코드와 식별자·필수값 적재 규칙 미정.
