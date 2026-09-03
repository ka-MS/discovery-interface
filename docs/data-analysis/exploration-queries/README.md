# 탐색 쿼리

Device42와 Maximo 원천을 반복 조회하는 재사용 쿼리다. 실행기는
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

- 블록 구분은 `-- name: <이름>`. 이름에 쓸 수 있는 문자는 `[a-zA-Z0-9-]` 뿐이다.
  언더스코어가 들어가면 블록이 무시되고 쿼리가 실행되지 않는다.
- 블록 이름이 결과 파일명이 된다.
- Device42는 `SELECT` 또는 `WITH` 로 시작해야 한다. Maximo는 `SELECT` 와
  `VALUES` 만 허용된다.

## 등록 기준

- 재실행 가능하다. 대상 변경이 최상단 한 줄 또는 `IN` 목록 수정으로 끝난다.
- 특정 조사 1회로 끝나지 않는다.
- 파일명이 목적을 설명한다.
- pk 리터럴에 의존하지 않는다.

## 목록

| 쿼리 | 용도 |
| --- | --- |
| `device42/device-type-distribution.sql` | type/subtype 분포, virtualsubtype_id 대응 |
| `device42/view-counts.sql` | 뷰별 건수, 파트 타입 분포 |
| `device42/device-related-inventory.sql` | 장비 한 대의 연관 자원 건수 |
| `device42/source-coverage-by-subtype.sql` | subtype별 원천 가용성, 식별자 충전율 |
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
| `device42/view-version-probe.sql` | 뷰 최고 버전 확인, 물리 서브타입·파트타입 마스터 |
| `maximo/table-description.sql` | 테이블 한글 설명 |
| `maximo/column-skeleton.sql` | 컬럼 매핑표 앞 4열 생성 |
| `maximo/dpa-child-coverage.sql` | DPA 자식 테이블 노드 커버리지, ASSETCLASS 분포, Device42 부모 목록 |
| `maximo/dpa-key-structure.sql` | DPA 기본키 구조와 Device42 자식 적재 건수 |
| `maximo/source-target-map-structure.sql` | 현행 미사용 SOURCE_TARGET_MAP의 잔존 상태 검증 |
| `maximo/dpa-hardware-existing-values.sql` | 세 DPA 하드웨어 자식의 기존값 관례 |
| `maximo/dpa-child-sequences.sql` | 현행 미사용 DPA 시퀀스와 기존 ID 범위 확인 |
| `maximo/dpa-os-network-existing-values.sql` | DPAOS·DPATCPIP·DPANETPRINTER·DPASWSUITE 기존값 관례 |
| `maximo/dpa-software-existing-values.sql` | DPASOFTWARE 기존값 관례, DPAM* 변환 계열 구조 |
| `maximo/dpa-view-conversion-requirements.sql` | UI 뷰가 요구하는 변환 데이터 등록 상태와 미표시 건수 |
