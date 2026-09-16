# 탐색 쿼리

Device42와 Maximo 데이터를 조사·검색하는 재사용 쿼리다. 매핑 SQL의 정본이 아니며,
실제 매핑 SQL은 매핑 문서 본문에 작성한다. 실행기는
`local/db-access-kit/scripts/` 에 있다.

## 실행

```bash
bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> <출력디렉터리>
bash local/db-access-kit/scripts/run-maximo.sh   <쿼리파일> <출력디렉터리>
```

출력은 `local/db-access-kit/work/` 아래에만 둔다. 저장소에 커밋하지 않는다.

## Device42 서버 선택

데모 검증용 Device42 는 두 대이고 수집 범위가 다르다. 운영 적재는 한 서버만
사용한다. 실행기는 조사할 서버 하나를 `DB_ACCESS_ENV` 로 고른다.
`connections.env` 를 편집하지 않는다. 다른 세션이 같이 쓴다.

| 서버 | 접속 파일 | 넓은 원천 | 장비 수 |
| --- | --- | --- | --- |
| 192.168.2.68 | `local/db-access-kit/connections-d42-68.env` | 소프트웨어, 파트, 마운트 | 95 |
| 192.168.1.35 | `local/db-access-kit/connections-d42-35.env` | 네트워크, OS | 85 |

```bash
DB_ACCESS_ENV=local/db-access-kit/connections-d42-68.env \
  bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> local/db-access-kit/work/device42-68

DB_ACCESS_ENV=local/db-access-kit/connections-d42-35.env \
  bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> local/db-access-kit/work/device42-35
```

출력 디렉터리를 서버별로 나눈다. 같은 쿼리의 결과가 서로 덮어쓰지 않게 한다.

매핑 조사 때만 두 서버를 각각 확인한다. 같은 뷰라도 건수가 크게 다르다.
`view_part_v1` 은 192.168.2.68 에서 479건, 192.168.1.35 에서 86건이다.
결과를 문서에 옮길 때 어느 서버 관측인지 함께 적는다.

Maximo 는 한 대뿐이라 `DB_ACCESS_ENV` 없이 기본 파일을 쓴다.

상세는 `../knowledge/device42/servers.md` 참조.

## 쿼리 파일 규칙

Computer 중심 관계 재조사(2026-09-15):
- [Device42 연결](device42/computer-ci-relations.sql): 양쪽 서버의 직접 FK·배열 연결·VM 호스트·IP 포트 경로 및 매핑 SELECT.
- [Maximo 규칙](maximo/computer-ci-relations.sql): 현재 분류 쌍·정확한 관계 코드·방향·플래그·고유키·참조 조건.
- [OS–Computer 승격 확인](maximo/os-computer-promotion-check.sql): ACTCI 관계와 승격된 CI·관계·부모 식별자 대조.
- [관계 적재 검증](maximo/ci-relation-load-check.sql): 적재 전후 건수·샘플 행·고아·규칙 위반·분류쌍 분포·배열 팬아웃.


- 블록 구분은 `-- name: <이름>`. 이름에 쓸 수 있는 문자는 `[a-zA-Z0-9-]` 뿐이다.
  언더스코어가 들어가면 블록이 무시되고 쿼리가 실행되지 않는다.
- 블록 이름이 결과 파일명이 된다.
- Device42는 `SELECT` 또는 `WITH` 로 시작해야 한다. Maximo는 `SELECT` 와
  `VALUES` 만 허용된다.

## 실행기 차이

두 실행기의 제약이 다르다. 같은 SQL을 양쪽에 쓸 수 없다.

| 항목 | Device42 | Maximo |
| --- | --- | --- |
| 허용 시작 키워드 | `SELECT`, `WITH` | `SELECT`, `VALUES` |
| `WITH` 절 | 가능 | **거부된다** |
| `FROM` 절 서브쿼리 | **거부된다** | 가능 |
| 블록 실패 시 | 첫 500에서 **배치 전체 중단** | 해당 블록에서 중단 |
| 출력 | `<블록>.txt`, `|` 구분 | `<블록>.tsv`, 탭 구분 |

Device42는 실재가 확인되지 않은 뷰를 등록 파일에 넣지 않는다. 하나가 500이면 뒤 블록이
전부 실행되지 않는다. 새 뷰는 한 블록짜리 임시 파일로 먼저 확인한다.

Device42의 상세 제약은 `../knowledge/device42/doql-constraints.md` 에 있다.

## 등록 기준

- 재실행 가능하다. 대상 변경이 최상단 한 줄 또는 `IN` 목록 수정으로 끝난다.
- 특정 조사 1회로 끝나지 않는다.
- 파일명이 목적을 설명한다.
- pk 리터럴에 의존하지 않는다.

## 목록

Device 통합 수집 조사(2026-09-15):

- [원천 컬럼·유형](device42/device-ci-source-shapes.sql): 기존 여섯 뷰 헤더와 Device 유형 분포.
- [보강 투영·포트 범위](device42/device-ci-projection.sql): Computer·VM·네트워크·프린터 후보와 cluster 연결 종류·대표 MAC을 조회하고 Switch/미판별 분기를 확인.
- [JSON 키](device42/device-ci-json-keys.sql): 발견 방식별 상세 키와 네트워크·프린터 판별 정보.
- [분류 탐색](maximo/device-ci-classification-discovery.sql): Device 관련 분류·부모·적용 객체·속성 수.
- [스펙·관계 대조](maximo/device-ci-specs.sql): 실제 속성 ID·타입·단위·적용 상태, 분류쌍 관계와 기존 ACTCI.
- [속성·승격 공백](maximo/device-ci-target-gaps.sql): EOS 등 속성 후보, 신규 분류 승격 범위, SNMP 속성.

| 쿼리 | 용도 |
| --- | --- |
| `device42/device-type-distribution.sql` | type/subtype 분포, virtualsubtype_id 대응 |
| `device42/view-counts.sql` | 뷰별 건수, 파트 타입 분포 |
| `device42/device-related-inventory.sql` | 장비 한 대의 연관 자원 건수 |
| `device42/source-coverage-by-subtype.sql` | subtype별 원천 가용성, 식별자 충전율 |
| `device42/computer-collection-inventory.sql` | DPA에 의존하지 않는 Computer 연관 수집 항목·값 보유율·원천 연결 쌍 조사 |
| `device42/computer-identity-memory.sql` | Computer 식별 후보 중복, RAM 슬롯·장비 총량/파트 합계, 파트 수량 검증 |
| `device42/etl-target-simulation.sql` | 현행 필터 적용 시 적재 대상과 제외 대상 |
| `device42/dpa-hardware-source-shapes.sql` | 논리 드라이브·네트워크·GPU 원천 뷰 컬럼 형태 |
| `device42/dpa-hardware-mapping.sql` | 논리 드라이브·네트워크·GPU의 COMPUTER 대상 원천과 값 분포 |
| `device42/dpa-os-network-source-shapes.sql` | OS·IP·서브넷·프린터 원천 뷰 컬럼 형태 |
| `device42/dpa-os-network-mapping.sql` | OS·TCP/IP·프린터의 대상 원천과 값 분포 |
| `device42/dpa-software-mapping.sql` | 소프트웨어의 COMPUTER 대상 원천, 자연키 유일성 |
| `device42/json-column-shapes.sql` | JSON 컬럼의 키 집합과 성격. 수집 방식별 차이 |
| `device42/dpacpu-integration-source.sql` | DPACPU 구현 원천 조건·반환 컬럼 검증 |
| `device42/vendor-master-source.sql` | 벤더 뷰 사용처와 정규화 상태. 제조사 변환 대상 원천 |
| `device42/ci-target-candidates.sql` | CI 본체 후보 View의 서버별 건수, PK 유일성, 컬럼 형태 |
| `device42/ci-target-relations.sql` | CI 본체 후보 사이의 FK 충전율과 실제 조인 성공 건수 |
| `device42/ci-target-profiles.sql` | Resource·Service Instance 등 범위 결정용 유형·상태 분포 |
| `device42/ci-target-overlaps.sql` | 범용 View와 전용 View의 동일 개체 중복 여부 |
| `device42/ci-source-values.sql` | CI 후보 필드의 충전율·길이·단위, PK 충돌, DB 호스트 연결 |
| `device42/ci-db-mapping.sql` | DB·DB Instance 유형별 원천 투영, 관계 쌍, 조인·값 변환 검증 |
| `device42/ci-db-source-analysis.sql` | DB·Instance 전체 컬럼, 숫자 변환, Resource 보강, 참조값 차이 조사 |
| `device42/ci-db-resource-details.sql` | Instance Resource의 JSON 버전 문자열과 중복 값 조사 |
| `device42/view-version-probe.sql` | 뷰 최고 버전 확인, 물리 서브타입·파트타입 마스터 |
| `device42/subtype-census.sql` | type·물리/가상 서브타입·자산 타입 전수와 배치 플래그 서명, 서브타입별 장비·하드웨어 건수 |
| `device42/computer-ci-mapping-audit.sql` | Computer 단위·BIOS·CPU 모델/아키텍처·기본 포트 원천 재대조 |
| `maximo/table-description.sql` | 테이블 한글 설명 |
| `maximo/column-skeleton.sql` | 컬럼 매핑표 앞 4열 생성 |
| `maximo/ci-target-structure.sql` | Actual CI·CI·분류·템플릿의 컬럼, 키, 관계, 시퀀스 |
| `maximo/ci-identifier-storage.sql` | ACTCIID/ACTCINUM 타입·인덱스·CHECK·트리거·자동 번호·속성 클래스 확인 |
| `maximo/ci-classification-audit.sql` | 기존 CI 참조 정합성, ACTCI/CI 분류·속성 적용 범위 |
| `maximo/ci-classification-templates.sql` | 분류 계층, 대표 속성 템플릿, 관계 정의, 분류명 검색 |
| `maximo/computer-classification-discovery.sql` | 전체 CI/ACTCI 분류 계층·적용 객체 및 CPU·RAM·BIOS 속성 탐색 |
| `maximo/computer-classification-specs.sql` | Computer 관련 분류의 실제 스펙·타입·단위·적용 설정과 정확한 분류쌍 관계 대조 |
| `maximo/computer-ci-mapping-audit.sql` | BIOS 속성 정의·자료형과 메모리·속도 단위 코드 재대조 |
| `maximo/ci-load-prerequisites.sql` | CI 적재·추가 속성 등록에 필요한 필수 컬럼·시퀀스·적용 설정 확인 |
| `maximo/ci-cache-size.sql` | CLASSSPEC 전체·ACTCI·ACTCI+CI와 ASSETATTRIBUTE 전체 건수·캐시 측정용 조회 |
| `maximo/ci-cache-payload-size.sql` | CI 정의 전체 컬럼 값의 바이트 합과 DB 디스크 할당량 |
| `maximo/ci-definition-coverage.sql` | ACTCI 분류별 속성 적용·속성 ID 누락, 대표 분류쌍 규칙 |
| `maximo/ci-definition-scope.sql` | CI 정의 캐시가 읽는 템플릿의 조직·사이트 범위, 스펙 키 중복, 조인 증폭 |
| `device42/ci-component-source.sql` | OS·Disk·Filesystem·IP 원천 뷰 형태, PK 유일성, Computer 연결분, 값 보유율, 배열 연결 분포 |
| `maximo/ci-component-classifications.sql` | 네 유형의 ACTCI 분류 후보·스펙·적용 설정과 Computer 분류쌍 관계 규칙 |
| `maximo/ci-promotion-scope.sql` | CITEMPLATE의 승격 범위, 범위별 CI↔ACTCI 분류 매핑, 적재 분류의 등록 여부 |
| `maximo/ci-db-target-mapping.sql` | 일반 DB·DB Server의 속성 설정과 분류쌍 관계 규칙 |
| `maximo/ci-db-spec-analysis.sql` | 일반 DB·DB Server의 전체 속성명·타입·적용 설정 조사 |
| `maximo/ci-relation-rules.sql` | RELATION·RELATIONRULES 구조, 적용 범위와 기존 CI 규칙 일치 여부 |
| `maximo/dpa-child-coverage.sql` | DPA 자식 테이블 노드 커버리지, ASSETCLASS 분포, Device42 부모 목록, NODEID 보유 자식 전수 |
| `maximo/dpa-key-structure.sql` | DPA 기본키 구조와 Device42 자식 적재 건수 |
| `maximo/source-target-map-structure.sql` | 현행 미사용 SOURCE_TARGET_MAP의 잔존 상태 검증 |
| `maximo/dpa-hardware-existing-values.sql` | 세 DPA 하드웨어 자식의 기존값 관례 |
| `maximo/dpa-child-sequences.sql` | 현행 미사용 DPA 시퀀스와 기존 ID 범위 확인 |
| `maximo/dpa-os-network-existing-values.sql` | DPAOS·DPATCPIP·DPANETPRINTER·DPASWSUITE 기존값 관례 |
| `maximo/dpa-software-existing-values.sql` | DPASOFTWARE 기존값 관례, DPAM* 변환 계열 구조 |
| `maximo/dpa-view-conversion-requirements.sql` | UI 뷰가 요구하는 변환 데이터 등록 상태와 미표시 건수 |
