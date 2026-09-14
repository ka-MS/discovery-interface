# ACTCIRELATION

실제 CI 관계 테이블

> Target: MAXIMO.ACTCIRELATION · 공통 규약 / 관계 종류는 출발 유형 문서
> 관측 2026-09-04 · Maximo BLUDB
> 구조 분석: [CI 모델](../../knowledge/maximo/ci-model.md)

## 1. 관계

- 물리 PK: `ACTCIRELATIONID`.
- 고유 인덱스: `(SOURCECI, TARGETCI, RELATIONNUM)`.
- 양 끝점은 `ACTCI.ACTCINUM`, 관계 정의는 `RELATION.RELATIONNUM`을 참조한다.
- `RELATIONRULES` 조회는 관계 코드와 양 끝 CI의 분류 ID를 함께 사용한다.

## 2. 테이블 매핑

관계 정의는 출발 유형 문서 한 곳에 둔다. DB Instance→DB는
[DB Instance](types/database-instance.md) 참조.
공통 표의 s와 t는 유형을 구분한 원천 키로 찾은 저장된 출발·도착 ACTCI다.
양 끝 본체를 저장한 뒤 관계를 저장한다. D42 숫자 PK를 SOURCECI/TARGETCI에 직접 넣지 않는다.

## 3. 조회 조건

Instance→DB는 `view_database_v2.databaseinstance_fk=view_databaseinstance_v2.databaseinstance_pk`로 조회한다.
출발은 Instance, 도착은 DB다. 양 끝의 저장된 ACTCINUM과 승인된 관계 코드가 필요하다.
instance_id·root_resource_fk의 대체 사용은 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)이다.

## 4. 컬럼 매핑

영속 11개 컬럼이다. 앞 4열은 Maximo 메타데이터다. 저장된 본체 참조와
아직 결정되지 않은 관계 코드·적재 정책을 구분한다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ACTCIRELATIONID | 고유 ID | BIGINT(19) | N | 미결 | Maximo 채번 | ACTCIRELATIONSEQ 예약·발번 정책 ISSUE-11 |
| ANCESTORCI | 상위 실제 CI | UPPER(150) | Y | 미결 | 확인된 별도 상위 CI 원천 없음 | SOURCECI 복사 여부를 가정하지 않음; ISSUE-11 |
| BASELINEDATE | 기준선 날짜 | DATETIME(10) | Y | 원천없음 | D42 관계 기준선 시각 미제공 | 값 생성·미사용 정책 ISSUE-11 |
| CHANGEBY | 변경자 | UPPER(100) | Y | 원천없음 | D42 관계 변경자 미제공 | ETL 계정 적용 여부 ISSUE-11 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | Y | 원천없음 | D42 관계 변경 시각 미제공 | 본체 변경 시각을 복사하지 않음; ETL 시각 정책 ISSUE-11 |
| RELATIONNUM | 관계 | UPPER(192) | N | 미결 | `MAXIMO.RELATION.RELATIONNUM` | 일반 DB Server→DB 규칙 없음; 코드·기준정보 보완 ISSUE-11 |
| SOURCECI | 소스 실제 구성 품목 번호 | UPPER(150) | N | 직접 | `s.ACTCINUM` | (database-instance, i.databaseinstance_pk)에 대응하는 저장된 Instance 번호 |
| SOURCECIGUID | 소스 실제 CI GUID | ALN(192) | Y | 미결 | 출발 ACTCI.GUID | 본체 GUID 정책 확정 후 참조; ISSUE-11 |
| SWAPPED | 스왑됩 | YORN(1) | Y | 미결 | 선택할 `RELATIONRULES.SWAPPED` | 메타데이터 기본값 0을 임의 적용하지 않음; ISSUE-11 |
| TARGETCI | 대상 실제 구성 품목 번호 | UPPER(150) | N | 직접 | `t.ACTCINUM` | (resource, d.database_pk)에 대응하는 저장된 DB 번호 |
| TARGETCIGUID | 대상 실제 CI GUID | ALN(192) | Y | 미결 | 도착 ACTCI.GUID | 본체 GUID 정책 확정 후 참조; ISSUE-11 |

## 5. SQL

실제 관계 쌍과 분류쌍 규칙 조회 SQL은 DB Instance 문서 5절 본문에 있다.
관계 코드·본체 식별자 정책이 미결이므로 INSERT/MERGE SQL은 아직 확정하지 않았다.

## 6. 미결

[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 분류·속성·관계 코드와 식별자·필수값 적재 규칙 미정.
