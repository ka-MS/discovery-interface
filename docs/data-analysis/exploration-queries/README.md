# 탐색 쿼리

Device42와 Maximo 원천을 반복 조회하는 재사용 쿼리다. 실행기는
`local/db-access-kit/scripts/` 에 있다.

## 실행

```bash
bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> <출력디렉터리>
bash local/db-access-kit/scripts/run-maximo.sh   <쿼리파일> <출력디렉터리>
```

출력은 `local/db-access-kit/work/` 아래에만 둔다. 저장소에 커밋하지 않는다.

## 실행 전 확인: 어느 Device42 인가

Device42 는 두 대이고 수집 범위가 다르다. 실행 전에 지금 어느 쪽에 붙는지
확인한다.

```bash
grep -E '^D42_RESOLVE=' local/db-access-kit/connections.env
```

| 서버 | 넓은 원천 |
| --- | --- |
| 192.168.2.68 | 소프트웨어, 파트, 마운트 |
| 192.168.1.35 | 네트워크, OS |

조사 대상에 맞는 서버를 고르고, 결과를 문서에 옮길 때 어느 서버 관측인지
함께 적는다. 상세는 `../knowledge/device42/servers.md` 참조.

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
| `maximo/table-description.sql` | 테이블 한글 설명 |
| `maximo/column-skeleton.sql` | 컬럼 매핑표 앞 4열 생성 |
| `maximo/dpa-child-coverage.sql` | DPA 자식 테이블 노드 커버리지, ASSETCLASS 분포 |
| `maximo/dpa-hardware-existing-values.sql` | 세 DPA 하드웨어 자식의 기존값 관례 |
| `maximo/dpa-os-network-existing-values.sql` | DPAOS·DPATCPIP·DPANETPRINTER·DPASWSUITE 기존값 관례 |
