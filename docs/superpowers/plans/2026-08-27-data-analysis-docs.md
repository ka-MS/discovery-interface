# data-analysis 문서 체계 구축 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Device42 → Maximo 매핑 작업의 관측 사실·탐색 쿼리·매핑 설계를 `docs/data-analysis/` 아래 세 계층으로 분리해 구축하고, 조사 완료분을 이관한다.

**Architecture:** 문서는 3계층으로 나뉜다. `knowledge/`는 관측 사실(스냅샷), `data-mapping/`은 테이블 단위 매핑 정본, `exploration-queries/`는 재사용 조회 쿼리다. 매핑 문서의 컬럼표 앞 4열은 Maximo 메타데이터(`MAXATTRIBUTE`/`L_MAXATTRIBUTE`)에서 생성하고 뒤 3열만 분석으로 채운다. `local/db-access-kit/`에는 접속 수단만 남긴다.

**Tech Stack:** Markdown, SQL(Db2 / Device42 DOQL), Bash, Python 3(표준 라이브러리만)

**Spec:** `docs/superpowers/specs/2026-08-27-data-analysis-docs-design.md`

## Global Constraints

- 문서 언어는 한국어. 파일명과 디렉터리명은 영문 소문자 케밥케이스.
- 톤 규칙(spec 5.3): 관측된 값과 매핑 규칙만 기술한다. 판단과 의견은 `open-issues.md`에 둔다. 같은 사실을 반복하지 않는다. 결론에 필요한 수치만 남긴다. 테이블·컬럼은 한글명을 함께 적고, Source와 조건은 실행 가능한 식으로 쓴다.
- PK 리터럴 금지(spec 5.1): `WHERE device_fk = 83` 금지. `WHERE d.name = 'episode'`, `WHERE d.virtualsubtype_id = 11` 형태로 쓴다.
- 관측 스탬프(spec 5.2): 건수·분포를 담는 문서는 상단에 `> 관측 <날짜> · <대상 서버> / 재조회 <쿼리 경로>`를 단다.
- 컬럼 매핑 표는 7열 고정: `Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건`.
- `구분` 허용값: `직접` / `변환` / `상수` / `채번` / `원천없음` / `미결`.
- 매핑 대상 컬럼 목록은 `SYSCAT.COLUMNS`가 아니라 `MAXIMO.MAXATTRIBUTE` 기준.
- **커밋 범위 제한:** 이 저장소는 커밋 이력이 없고 애플리케이션 소스가 스테이징된 상태다. `git add .` 및 `git add -A`를 절대 사용하지 않는다. 각 태스크에 명시된 경로만 `git add` 한다.
- DOQL 실행기는 바인드 파라미터를 지원하지 않는다. 대상 지정은 쿼리 최상단 CTE 한 줄을 수정하는 방식으로 한다.
- `local/`은 `.gitignore` 대상이다. `local/db-access-kit/` 변경은 커밋되지 않으며, 커밋 단계에서 제외한다.
- 모든 명령은 저장소 루트에서 실행한다. 앞 단계에서 `cd` 한 경우 루트로 돌아온 뒤 실행한다.

---

## File Structure

**신규 생성**

| 경로 | 책임 |
| --- | --- |
| `docs/data-analysis/README.md` | 진입점. 문서 지도, 읽는 순서, 진행 상태 |
| `docs/data-analysis/open-issues.md` | 미결·정책 대기 항목 |
| `docs/data-analysis/knowledge/device42/doql-constraints.md` | DOQL 실행 제약 |
| `docs/data-analysis/knowledge/device42/views.md` | 실재 뷰 목록·컬럼·device 연결 키 |
| `docs/data-analysis/knowledge/device42/device-types.md` | type/subtype 체계와 분포 |
| `docs/data-analysis/knowledge/device42/servers.md` | 서버별 데이터 성격·소스 커버리지 |
| `docs/data-analysis/knowledge/maximo/deployedasset-model.md` | NODEID 계층·ASSETCLASS 판별자 |
| `docs/data-analysis/knowledge/maximo/tables.md` | DPA* 목록·컬럼수·노드 커버리지 |
| `docs/data-analysis/data-mapping/README.md` | ASSETCLASS 라우팅·실행 순서·진행 현황 |
| `docs/data-analysis/data-mapping/asset/*.md` (13장) | 테이블별 매핑 정본 |
| `docs/data-analysis/data-mapping/software/dpasoftware.md` | 테이블별 매핑 정본 |
| `docs/data-analysis/exploration-queries/README.md` | 쿼리 사용법·등록 기준 |
| `docs/data-analysis/exploration-queries/device42/*.sql` | Device42 탐색 쿼리 |
| `docs/data-analysis/exploration-queries/maximo/*.sql` | Maximo 탐색 쿼리 |
| `docs/data-analysis/tools/gen-mapping-skeleton.py` | 메타데이터 TSV → 매핑 문서 골격 생성 |
| `CLAUDE.md` | 새 세션 진입점 안내 |

**수정**

| 경로 | 변경 |
| --- | --- |
| `local/db-access-kit/AGENTS.md` | 쿼리 위치를 `docs/data-analysis/exploration-queries/`로 안내 |
| `local/db-access-kit/README.md` | 동일 |

**삭제**

| 경로 | 사유 |
| --- | --- |
| `local/db-access-kit/queries/device83-related.sql` | pk 83 하드코딩. Task 2에서 파라미터화 버전으로 대체 |

`local/db-access-kit/queries/device42-smoke.sql`과 `maximo-smoke.sql`은 남긴다.
`check-connections.sh`가 기본 인자로 참조하는 접속 확인용이며 분석 쿼리가 아니다.

---

## Task 1: 탐색 쿼리 디렉터리와 Maximo 메타데이터 쿼리

Maximo 메타데이터 쿼리는 Task 7의 매핑 골격 생성이 의존하므로 먼저 만든다.

**Files:**
- Create: `docs/data-analysis/exploration-queries/README.md`
- Create: `docs/data-analysis/exploration-queries/maximo/table-description.sql`
- Create: `docs/data-analysis/exploration-queries/maximo/column-skeleton.sql`

**Interfaces:**
- Produces: `column-skeleton.sql`은 `-- name: column-skeleton` 블록 하나를 가지며 `OBJECTNAME, ATTRIBUTENAME, KO_TITLE, MAXTYPE, LENGTH, SCALE, REQUIRED, DEFAULTVALUE` 8열 TSV를 출력한다. Task 7이 이 출력을 파싱한다.
- Produces: `table-description.sql`은 `-- name: table-description` 블록 하나를 가지며 `OBJECTNAME, KO_DESC` 2열 TSV를 출력한다. Task 7이 이 출력을 문서 최상단 설명으로 쓴다.

- [ ] **Step 1: 디렉터리 생성**

```bash
mkdir -p docs/data-analysis/exploration-queries/device42
mkdir -p docs/data-analysis/exploration-queries/maximo
```

- [ ] **Step 2: `maximo/table-description.sql` 작성**

블록 이름에는 언더스코어를 쓸 수 없다. 실행기가 `[a-zA-Z0-9-]+`로만 파싱한다.

```sql
-- 대상 테이블의 한글 설명. 매핑 문서 최상단 한 줄에 사용한다.
-- 대상 변경은 IN 목록만 수정한다.

-- name: table-description
SELECT O.OBJECTNAME,
       L.DESCRIPTION AS KO_DESC
FROM MAXIMO.MAXOBJECT O
LEFT JOIN MAXIMO.L_MAXOBJECT L
       ON L.OWNERID = O.MAXOBJECTID
      AND L.LANGCODE = 'KO'
WHERE O.OBJECTNAME IN ('DEPLOYEDASSET','DPACOMPUTER','DPAOS','DPASOFTWARE',
                       'DPACPU','DPADISK','DPALOGICALDRIVE','DPANETADAPTER',
                       'DPATCPIP','DPAMEDIAADAPTER','DPADISPLAY','DPASWSUITE',
                       'DPANETDEVICE','DPANETPRINTER')
ORDER BY O.OBJECTNAME;
```

- [ ] **Step 3: `maximo/column-skeleton.sql` 작성**

```sql
-- 매핑 문서 컬럼표의 앞 4열(Target 컬럼/한글명/타입/Null)을 생성한다.
-- DEFAULTVALUE 가 있으면 구분을 '상수' 후보로 본다.
-- 대상 컬럼 기준은 SYSCAT.COLUMNS 가 아니라 MAXATTRIBUTE 다. ROWSTAMP 등
-- 시스템 컬럼이 제외되어 매핑 대상과 일치한다.
-- 대상 변경은 IN 목록만 수정한다.

-- name: column-skeleton
SELECT A.OBJECTNAME,
       A.ATTRIBUTENAME,
       L.TITLE AS KO_TITLE,
       A.MAXTYPE,
       A.LENGTH,
       A.SCALE,
       A.REQUIRED,
       A.DEFAULTVALUE
FROM MAXIMO.MAXATTRIBUTE A
LEFT JOIN MAXIMO.L_MAXATTRIBUTE L
       ON L.OWNERID = A.MAXATTRIBUTEID
      AND L.LANGCODE = 'KO'
WHERE A.OBJECTNAME IN ('DEPLOYEDASSET','DPACOMPUTER','DPAOS','DPASOFTWARE',
                       'DPACPU','DPADISK','DPALOGICALDRIVE','DPANETADAPTER',
                       'DPATCPIP','DPAMEDIAADAPTER','DPADISPLAY','DPASWSUITE',
                       'DPANETDEVICE','DPANETPRINTER')
ORDER BY A.OBJECTNAME, A.ATTRIBUTENAME;
```

- [ ] **Step 4: 실행해서 출력 검증**

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/column-skeleton.sql \
  local/db-access-kit/work/maximo
awk -F'\t' 'NR>1{print $1}' local/db-access-kit/work/maximo/column-skeleton.tsv | sort | uniq -c
```

기대: 14개 테이블, 합계 293행.

```
39 DEPLOYEDASSET   42 DPACOMPUTER   19 DPACPU   19 DPADISK
14 DPADISPLAY      17 DPALOGICALDRIVE   16 DPAMEDIAADAPTER
19 DPANETADAPTER   12 DPANETDEVICE   19 DPANETPRINTER
15 DPAOS           29 DPASOFTWARE   18 DPASWSUITE   15 DPATCPIP
```

- [ ] **Step 5: 한글명 누락 없음 확인**

```bash
awk -F'\t' 'NR>1 && ($3=="" || $3=="\"\"")' local/db-access-kit/work/maximo/column-skeleton.tsv | wc -l
```

기대: `0`

- [ ] **Step 6: `exploration-queries/README.md` 작성**

````markdown
# 탐색 쿼리

Device42와 Maximo 원천을 반복 조회하는 재사용 쿼리다. 실행기는
`local/db-access-kit/scripts/` 에 있다.

## 실행

```bash
bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> <출력디렉터리>
bash local/db-access-kit/scripts/run-maximo.sh   <쿼리파일> <출력디렉터리>
```

출력은 `local/db-access-kit/work/` 아래에만 둔다. 저장소에 커밋하지 않는다.

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
| `maximo/table-description.sql` | 테이블 한글 설명 |
| `maximo/column-skeleton.sql` | 컬럼 매핑표 앞 4열 생성 |
````

- [ ] **Step 7: 커밋**

```bash
git add docs/data-analysis/exploration-queries
git commit -m "docs: add exploration query dir and Maximo metadata queries"
```

---

## Task 2: Device42 탐색 쿼리 이관과 키트 경계 정리

**Files:**
- Create: `docs/data-analysis/exploration-queries/device42/device-type-distribution.sql`
- Create: `docs/data-analysis/exploration-queries/device42/view-counts.sql`
- Create: `docs/data-analysis/exploration-queries/device42/device-related-inventory.sql`
- Create: `docs/data-analysis/exploration-queries/device42/source-coverage-by-subtype.sql`
- Create: `docs/data-analysis/exploration-queries/device42/etl-target-simulation.sql`
- Create: `docs/data-analysis/exploration-queries/maximo/dpa-child-coverage.sql`
- Modify: `docs/data-analysis/exploration-queries/README.md` (목록 표 교체)
- Modify: `local/db-access-kit/AGENTS.md`
- Modify: `local/db-access-kit/README.md`
- Delete: `local/db-access-kit/queries/device83-related.sql`

**Interfaces:**
- Consumes: Task 1의 `exploration-queries/README.md` 목록 표
- Produces: Task 4가 `device-type-distribution.sql`·`source-coverage-by-subtype.sql`·`view-counts.sql` 출력을 인용한다. Task 3이 `dpa-child-coverage.sql` 출력을 인용한다.

- [ ] **Step 1: `device42/device-type-distribution.sql` 작성**

```sql
-- Device42 장비의 type/subtype 분포. ASSETCLASS 판정 근거다.

-- name: type-distribution
SELECT type,
       COALESCE(virtualsubtype, '-')  AS virtualsubtype,
       COALESCE(physicalsubtype, '-') AS physicalsubtype,
       virtual_host,
       network_device,
       count(*) AS device_cnt
FROM view_device_v2
GROUP BY 1, 2, 3, 4, 5
ORDER BY 6 DESC;

-- name: subtype-ids
SELECT virtualsubtype_id, virtualsubtype, count(*) AS cnt
FROM view_device_v2
WHERE virtualsubtype_id IS NOT NULL
GROUP BY 1, 2
ORDER BY 1;
```

- [ ] **Step 2: `device42/view-counts.sql` 작성**

```sql
-- 실재 확인된 DOQL 뷰의 건수. 서버 간 데이터 성격 비교에 쓴다.

-- name: view-counts
SELECT 'view_device_v2' AS view_name, count(*) AS cnt FROM view_device_v2
UNION ALL SELECT 'view_hardware_v1', count(*) FROM view_hardware_v1
UNION ALL SELECT 'view_vendor_v1', count(*) FROM view_vendor_v1
UNION ALL SELECT 'view_software_v1', count(*) FROM view_software_v1
UNION ALL SELECT 'view_softwareinuse_v1', count(*) FROM view_softwareinuse_v1
UNION ALL SELECT 'view_serviceinstance_v2', count(*) FROM view_serviceinstance_v2
UNION ALL SELECT 'view_service_v2', count(*) FROM view_service_v2
UNION ALL SELECT 'view_appcomp_v1', count(*) FROM view_appcomp_v1
UNION ALL SELECT 'view_netport_v1', count(*) FROM view_netport_v1
UNION ALL SELECT 'view_ipaddress_v1', count(*) FROM view_ipaddress_v1
UNION ALL SELECT 'view_subnet_v1', count(*) FROM view_subnet_v1
UNION ALL SELECT 'view_vlan_v1', count(*) FROM view_vlan_v1
UNION ALL SELECT 'view_part_v1', count(*) FROM view_part_v1
UNION ALL SELECT 'view_partmodel_v1', count(*) FROM view_partmodel_v1
UNION ALL SELECT 'view_mountpoint_v1', count(*) FROM view_mountpoint_v1
UNION ALL SELECT 'view_deviceos_v1', count(*) FROM view_deviceos_v1
UNION ALL SELECT 'view_os_v1', count(*) FROM view_os_v1
UNION ALL SELECT 'view_deviceurl_v1', count(*) FROM view_deviceurl_v1
UNION ALL SELECT 'view_pdu_v1', count(*) FROM view_pdu_v1
UNION ALL SELECT 'view_rack_v1', count(*) FROM view_rack_v1
UNION ALL SELECT 'view_room_v1', count(*) FROM view_room_v1
UNION ALL SELECT 'view_building_v1', count(*) FROM view_building_v1
UNION ALL SELECT 'view_asset_v1', count(*) FROM view_asset_v1
UNION ALL SELECT 'view_customer_v1', count(*) FROM view_customer_v1
UNION ALL SELECT 'view_enduser_v1', count(*) FROM view_enduser_v1
UNION ALL SELECT 'view_remotecollector_v1', count(*) FROM view_remotecollector_v1
ORDER BY 2 DESC;

-- name: part-types
SELECT pm.type_name, count(*) AS part_cnt, count(DISTINCT p.device_fk) AS device_cnt
FROM view_part_v1 p
LEFT JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
GROUP BY pm.type_name
ORDER BY 2 DESC;
```

- [ ] **Step 3: `device42/device-related-inventory.sql` 작성**

pk 하드코딩을 제거한 버전이다. 대상은 `target` CTE 한 줄로만 지정한다.

```sql
-- 장비 한 대의 연관 자원 건수를 뷰별로 집계한다.
-- 대상 변경: 아래 target CTE 의 name 값만 수정한다. pk 를 쓰지 않는다.

-- name: related-inventory
WITH target AS (
    SELECT device_pk AS pk FROM view_device_v2 WHERE name = 'episode'
)
SELECT 'view_deviceos_v1' AS related_view, 'device_fk' AS link_column, count(*) AS row_cnt
  FROM view_deviceos_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_ipaddress_v1', 'device_fk', count(*)
  FROM view_ipaddress_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_netport_v1', 'device_fk', count(*)
  FROM view_netport_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_mountpoint_v1', 'device_fk', count(*)
  FROM view_mountpoint_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_part_v1', 'device_fk', count(*)
  FROM view_part_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_softwareinuse_v1', 'device_fk', count(*)
  FROM view_softwareinuse_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_serviceinstance_v2', 'device_fk', count(*)
  FROM view_serviceinstance_v2 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_appcomp_v1', 'device_fk', count(*)
  FROM view_appcomp_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_deviceurl_v1', 'device_fk', count(*)
  FROM view_deviceurl_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_pdu_v1', 'device_fk', count(*)
  FROM view_pdu_v1 t, target d WHERE t.device_fk = d.pk
UNION ALL SELECT 'view_device_v2', 'virtual_host_device_fk', count(*)
  FROM view_device_v2 t, target d WHERE t.virtual_host_device_fk = d.pk
UNION ALL SELECT 'view_device_v2', 'host_chassis_device_fk', count(*)
  FROM view_device_v2 t, target d WHERE t.host_chassis_device_fk = d.pk
UNION ALL SELECT 'view_device_v2', 'vm_manager_device_fk', count(*)
  FROM view_device_v2 t, target d WHERE t.vm_manager_device_fk = d.pk
ORDER BY 3 DESC, 1, 2;
```

- [ ] **Step 4: `device42/source-coverage-by-subtype.sql` 작성**

```sql
-- subtype 별로 어떤 원천이 채워져 있는지 본다. 매핑 문서의 커버리지 근거다.

-- name: coverage-by-subtype
SELECT COALESCE(d.virtualsubtype, d.physicalsubtype, d.type) AS subtype,
       count(*) AS devices,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_deviceos_v1 o WHERE o.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_os,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_softwareinuse_v1 s WHERE s.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_software,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_netport_v1 n WHERE n.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_netport,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_ipaddress_v1 i WHERE i.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_ip,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_part_v1 p WHERE p.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_part,
       sum(CASE WHEN EXISTS (SELECT 1 FROM view_mountpoint_v1 m WHERE m.device_fk=d.device_pk) THEN 1 ELSE 0 END) AS has_mount,
       sum(CASE WHEN d.hardware_fk IS NOT NULL THEN 1 ELSE 0 END) AS has_hardware
FROM view_device_v2 d
GROUP BY COALESCE(d.virtualsubtype, d.physicalsubtype, d.type)
ORDER BY 2 DESC;

-- name: identifier-fill-rate
SELECT COALESCE(virtualsubtype, physicalsubtype, type) AS subtype,
       count(*) AS devices,
       sum(CASE WHEN uuid IS NOT NULL AND uuid <> '' THEN 1 ELSE 0 END) AS has_uuid,
       sum(CASE WHEN serial_no IS NOT NULL AND serial_no <> '' THEN 1 ELSE 0 END) AS has_serial
FROM view_device_v2
WHERE type IN ('virtual','physical')
  AND (virtualsubtype_id IS NULL OR virtualsubtype_id <> 15)
GROUP BY 1
ORDER BY 2 DESC;
```

- [ ] **Step 5: `device42/etl-target-simulation.sql` 작성**

```sql
-- DeployedAssetIntegrate 의 현행 필터와 ASSETCLASS 판정을 SQL 로 재현한다.
-- 코드 변경 시 이 쿼리도 함께 고친다.
-- 근거: DeployedAssetIntegrate.java DEVICE_FILTER, mapData()

-- name: etl-target
SELECT CASE
         WHEN d.network_device THEN 'NETDEVICE'
         WHEN d.physicalsubtype = 'Network Printer' THEN 'NETPRINTER'
         ELSE 'COMPUTER'
       END AS assetclass,
       d.type,
       COALESCE(d.virtualsubtype, d.physicalsubtype, '-') AS subtype,
       count(*) AS device_cnt
FROM view_device_v2 d
WHERE d.type IN ('virtual','physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
GROUP BY 1, 2, 3
ORDER BY 1, 4 DESC;

-- name: etl-excluded
SELECT d.type,
       COALESCE(d.virtualsubtype, d.physicalsubtype, '-') AS subtype,
       count(*) AS device_cnt
FROM view_device_v2 d
WHERE NOT (d.type IN ('virtual','physical')
       AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15))
GROUP BY 1, 2
ORDER BY 3 DESC;
```

- [ ] **Step 6: `maximo/dpa-child-coverage.sql` 작성**

```sql
-- ASSETCLASS 별로 어떤 DPA 자식 테이블에 행이 붙는지, 노드 커버리지는 얼마인지 본다.

-- name: child-coverage
SELECT 'DPACOMPUTER' AS T, COUNT(DISTINCT NODEID) AS NODES, COUNT(*) AS ROW_CNT FROM MAXIMO.DPACOMPUTER
UNION ALL SELECT 'DPAOS', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPAOS
UNION ALL SELECT 'DPASOFTWARE', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPASOFTWARE
UNION ALL SELECT 'DPACPU', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPACPU
UNION ALL SELECT 'DPADISK', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPADISK
UNION ALL SELECT 'DPALOGICALDRIVE', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPALOGICALDRIVE
UNION ALL SELECT 'DPANETADAPTER', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPANETADAPTER
UNION ALL SELECT 'DPATCPIP', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPATCPIP
UNION ALL SELECT 'DPAMEDIAADAPTER', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPAMEDIAADAPTER
UNION ALL SELECT 'DPADISPLAY', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPADISPLAY
UNION ALL SELECT 'DPASWSUITE', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPASWSUITE
UNION ALL SELECT 'DPANETDEVICE', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPANETDEVICE
UNION ALL SELECT 'DPANETPRINTER', COUNT(DISTINCT NODEID), COUNT(*) FROM MAXIMO.DPANETPRINTER
ORDER BY 2 DESC;

-- name: assetclass-dist
SELECT ASSETCLASS,
       COALESCE(IMPORTSOURCE,'(null)') AS IMPORTSOURCE,
       COUNT(*) AS CNT
FROM MAXIMO.DEPLOYEDASSET
GROUP BY ASSETCLASS, IMPORTSOURCE
ORDER BY 3 DESC;
```

- [ ] **Step 7: 전체 실행 검증**

```bash
for q in device-type-distribution view-counts device-related-inventory \
         source-coverage-by-subtype etl-target-simulation; do
  echo "### $q"
  bash local/db-access-kit/scripts/run-device42.sh \
    docs/data-analysis/exploration-queries/device42/$q.sql \
    local/db-access-kit/work/device42 2>&1 | tail -3
done
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/dpa-child-coverage.sql \
  local/db-access-kit/work/maximo 2>&1 | tail -3
```

기대: 모든 블록이 `<이름> -> <경로>` 로 출력되고 curl 오류가 없다.
블록이 누락되면 `-- name:` 에 언더스코어가 들어갔는지 확인한다.

- [ ] **Step 8: pk 리터럴 잔존 검사**

```bash
grep -rnE 'device_fk[[:space:]]*=[[:space:]]*[0-9]+|device_pk[[:space:]]*=[[:space:]]*[0-9]+' \
  docs/data-analysis/exploration-queries/
```

기대: 출력 없음.

- [ ] **Step 9: 구버전 쿼리 삭제**

```bash
rm local/db-access-kit/queries/device83-related.sql
ls local/db-access-kit/queries/
```

기대: `device42-smoke.sql`, `maximo-smoke.sql` 두 개만 남는다.

- [ ] **Step 9b: 접속 점검 스크립트 회귀 확인**

smoke 쿼리를 키트에 남긴 이유가 이것이다. 쿼리 정리 후에도 접속 확인이
동작해야 한다.

```bash
bash local/db-access-kit/scripts/check-connections.sh 2>&1 | tail -5
```

기대: `Both read connections succeeded.` 로 끝난다.

- [ ] **Step 10: `exploration-queries/README.md` 목록 표 교체**

```markdown
| 쿼리 | 용도 |
| --- | --- |
| `device42/device-type-distribution.sql` | type/subtype 분포, virtualsubtype_id 대응 |
| `device42/view-counts.sql` | 뷰별 건수, 파트 타입 분포 |
| `device42/device-related-inventory.sql` | 장비 한 대의 연관 자원 건수 |
| `device42/source-coverage-by-subtype.sql` | subtype별 원천 가용성, 식별자 충전율 |
| `device42/etl-target-simulation.sql` | 현행 필터 적용 시 적재 대상과 제외 대상 |
| `maximo/table-description.sql` | 테이블 한글 설명 |
| `maximo/column-skeleton.sql` | 컬럼 매핑표 앞 4열 생성 |
| `maximo/dpa-child-coverage.sql` | DPA 자식 테이블 노드 커버리지, ASSETCLASS 분포 |
```

- [ ] **Step 11: 키트 문서에서 쿼리 위치 안내 수정**

`local/db-access-kit/AGENTS.md` 의 "실행 순서" 4항을 교체한다.

```markdown
4. 조사 쿼리는 `docs/data-analysis/exploration-queries/` 에 둔다. 이 폴더에는
   접속 확인용 `queries/*-smoke.sql` 만 남긴다.
```

`local/db-access-kit/README.md` 의 "4. 쿼리 추가 방법" 첫 문단을 교체한다.

```markdown
조사 쿼리는 `docs/data-analysis/exploration-queries/` 에 둔다. 등록 기준과 목록은
해당 폴더의 README.md 를 따른다. 아래 블록 규칙은 동일하다. 블록 이름에는
`[a-zA-Z0-9-]` 만 쓸 수 있고 언더스코어를 넣으면 블록이 무시된다.
```

- [ ] **Step 12: 커밋**

`local/` 은 `.gitignore` 대상이므로 커밋에 포함되지 않는다.

```bash
git add docs/data-analysis/exploration-queries
git commit -m "docs: migrate Device42 exploration queries, drop pk-hardcoded query"
```

---

## Task 3: knowledge/maximo 문서

**Files:**
- Create: `docs/data-analysis/knowledge/maximo/deployedasset-model.md`
- Create: `docs/data-analysis/knowledge/maximo/tables.md`

**Interfaces:**
- Consumes: Task 2의 `maximo/dpa-child-coverage.sql`, Task 1의 `maximo/column-skeleton.sql`
- Produces: Task 6의 `data-mapping/README.md` 가 ASSETCLASS 라우팅 표를 여기서 참조한다.

- [ ] **Step 1: 최신 수치 재조회**

```bash
mkdir -p docs/data-analysis/knowledge/maximo
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/dpa-child-coverage.sql \
  local/db-access-kit/work/maximo
column -t -s$'\t' local/db-access-kit/work/maximo/child-coverage.tsv
column -t -s$'\t' local/db-access-kit/work/maximo/assetclass-dist.tsv
```

- [ ] **Step 2: `deployedasset-model.md` 작성**

`<실행일>` 은 Step 1을 실행한 날짜로, `<커버리지 표>` 는 Step 1 출력으로 채운다.

```markdown
# DEPLOYEDASSET 모델

> 관측 <실행일> · Maximo BLUDB
> 재조회 docs/data-analysis/exploration-queries/maximo/dpa-child-coverage.sql

배치된 자산(Deployed Assets). 수집 도구가 발견한 자산의 공통 헤더다.

## 계층

`DEPLOYEDASSET` 이 부모, `DPA*` 가 자식이다. 모든 `DPA*` 테이블은 `NODEID`
단일 컬럼으로 부모를 참조한다. `DPA*` 에는 `IMPORTSOURCE` 컬럼이 없다.
출처 구분은 부모를 조인해야 한다.

## ASSETCLASS 판별자

`DEPLOYEDASSET.ASSETCLASS` 가 어떤 자식 테이블에 행이 붙는지 결정한다.

| ASSETCLASS | 자식 테이블 |
| --- | --- |
| COMPUTER | DPACOMPUTER, DPAOS, DPASOFTWARE, DPACPU, DPADISK, DPALOGICALDRIVE, DPANETADAPTER, DPATCPIP, DPAMEDIAADAPTER, DPADISPLAY, DPASWSUITE |
| NETDEVICE | DPANETDEVICE |
| NETPRINTER | DPANETPRINTER |

관측 시점 기준 ASSETCLASS 교차 사례와 부모 없는 자식 행은 없었다.

## 자식 테이블 커버리지

<Step 1 child-coverage.tsv 출력을 `테이블 | 노드 수 | 행 수` 표로 옮긴다>

부분 커버리지가 정상이다. 관측 대상이 없으면 행을 만들지 않는다.
`DPACOMPUTER` 만 부모와 1:1 로 전건 존재한다.

## 키

- `NODEID` 는 `MAXIMO.DEPLOYEDASSETSEQ` 시퀀스로 발번된다.
- 현행 적재의 MERGE 키는 `(SOURCEID, IMPORTSOURCE)` 다.
  근거: `DeployedAssetIntegrate.java` `MERGE_DEPLOYED_ASSET_QUERY`
- `SOURCEID` 에는 Device42 `device_pk` 가 들어간다. 관련 미결 사항은
  `../../open-issues.md` 참조.
```

- [ ] **Step 3: `tables.md` 작성**

```markdown
# DPA* 테이블

> 관측 <실행일> · Maximo BLUDB
> 재조회 docs/data-analysis/exploration-queries/maximo/column-skeleton.sql

매핑 대상 14개 테이블의 한글명과 컬럼 수다. 컬럼 목록 기준은
`MAXIMO.MAXATTRIBUTE` 다. `SYSCAT.COLUMNS` 에 있는 `ROWSTAMP` 등 시스템
컬럼은 제외된다.

| 테이블 | 한글명 | 컬럼 수 |
| --- | --- | --- |
| DEPLOYEDASSET | 배치된 자산 | 39 |
| DPACOMPUTER | 배치된 자산 컴퓨터 | 42 |
| DPACPU | 배치된 자산 컴퓨터 프로세서 | 19 |
| DPADISK | 배치된 자산 컴퓨터 디스크 | 19 |
| DPADISPLAY | 배치된 자산 컴퓨터 디스플레이 | 14 |
| DPALOGICALDRIVE | 배치된 자산 컴퓨터 논리 드라이브 | 17 |
| DPAMEDIAADAPTER | 배치된 자산 컴퓨터 미디어 어댑터 | 16 |
| DPANETADAPTER | 배치된 자산 컴퓨터 네트워크 어댑터 | 19 |
| DPANETDEVICE | 배치된 자산 네트워크 디바이스 | 12 |
| DPANETPRINTER | 배치된 자산 네트워크 프린터 | 19 |
| DPAOS | 배치된 자산 컴퓨터 운영 체제 | 15 |
| DPASOFTWARE | 배치된 자산 컴퓨터 애플리케이션 | 29 |
| DPASWSUITE | 배치된 자산 컴퓨터 스위트 | 18 |
| DPATCPIP | 배치된 자산 컴퓨터 TCP/IP | 15 |

합계 293개 컬럼. 14개 테이블 모두 한글명이 전건 제공된다.

## 메타데이터 출처

| 항목 | 출처 |
| --- | --- |
| 테이블 한글 설명 | `MAXOBJECT` / `L_MAXOBJECT` (LANGCODE='KO') |
| 컬럼 한글명 | `MAXATTRIBUTE` / `L_MAXATTRIBUTE` (LANGCODE='KO') |
| 타입·길이 | `MAXATTRIBUTE.MAXTYPE`, `LENGTH` |
| 필수 여부 | `MAXATTRIBUTE.REQUIRED` |
| 기본값 | `MAXATTRIBUTE.DEFAULTVALUE` |
```

- [ ] **Step 4: 문서의 수치와 쿼리 출력 일치 확인**

```bash
awk -F'\t' 'NR>1{print $1}' local/db-access-kit/work/maximo/column-skeleton.tsv \
  | sort | uniq -c | awk '{printf "%s %s\n", $2, $1}'
```

기대: `tables.md` 표의 테이블별 컬럼 수와 전부 일치.

- [ ] **Step 5: 커밋**

```bash
git add docs/data-analysis/knowledge/maximo
git commit -m "docs: add Maximo DEPLOYEDASSET model and DPA table knowledge"
```

---

## Task 4: knowledge/device42 문서

**Files:**
- Create: `docs/data-analysis/knowledge/device42/doql-constraints.md`
- Create: `docs/data-analysis/knowledge/device42/views.md`
- Create: `docs/data-analysis/knowledge/device42/device-types.md`
- Create: `docs/data-analysis/knowledge/device42/servers.md`

**Interfaces:**
- Consumes: Task 2의 `device42/*.sql`
- Produces: Task 6·Task 7이 `views.md` 의 뷰 정보를 매핑 Source 로 참조한다.

- [ ] **Step 1: 최신 수치 재조회**

```bash
mkdir -p docs/data-analysis/knowledge/device42
for q in device-type-distribution view-counts source-coverage-by-subtype; do
  bash local/db-access-kit/scripts/run-device42.sh \
    docs/data-analysis/exploration-queries/device42/$q.sql \
    local/db-access-kit/work/device42
done
grep -E '^D42_RESOLVE=' local/db-access-kit/connections.env
```

마지막 명령의 IP 가 관측 스탬프에 적을 대상 서버다.

- [ ] **Step 2: `doql-constraints.md` 작성**

```markdown
# DOQL 실행 제약

Device42 접근은 PostgreSQL 직접 접속이 아니라 DOQL REST API
(`POST /services/data/v1.0/query/`) 다.

## 차단되는 것

카탈로그 조회는 모두 HTTP 500 으로 거부된다.

| 대상 | 결과 |
| --- | --- |
| `information_schema.tables` / `.views` | 500 |
| `pg_views` | 500 |
| `pg_class` | 500 |

뷰 목록을 쿼리로 얻을 수 없다. 실재 여부는 `SELECT * FROM <뷰> LIMIT 1` 로
개별 확인한다. 존재하지 않는 뷰도 500 을 반환하므로, DOQL 의 500 은 접속
실패가 아니라 뷰명 또는 권한 오류로 해석한다.

## 허용되는 것

- `SELECT *` 사용 가능. 헤더 행으로 컬럼 목록을 얻을 수 있다.
- `WITH` 절 사용 가능.
- `UNION ALL`, 스칼라 서브쿼리, `EXISTS` 사용 가능.

## 실행기 제약

- 쿼리는 `SELECT` 또는 `WITH` 로 시작해야 한다.
- 블록 이름은 `[a-zA-Z0-9-]` 만 허용된다. 언더스코어가 들어가면 블록이
  조용히 무시되고 쿼리가 실행되지 않는다. 실패로 보고되지 않으므로
  결과 파일 생성 여부로 확인한다.
- 바인드 파라미터를 지원하지 않는다. 대상 지정은 쿼리 최상단 CTE 를
  수정한다.
```

- [ ] **Step 3: `views.md` 작성**

```markdown
# Device42 DOQL 뷰

> 관측 <실행일> · Device42 <IP>
> 재조회 docs/data-analysis/exploration-queries/device42/view-counts.sql

실재가 확인된 뷰와 device 연결 키다. 카탈로그 조회가 막혀 있어
개별 확인으로 얻은 목록이다.

## device 를 직접 참조하는 뷰

매핑의 주 원천이다.

| 뷰 | 연결 컬럼 | 대응 Maximo 테이블 |
| --- | --- | --- |
| `view_deviceos_v1` | `device_fk` | DPAOS |
| `view_softwareinuse_v1` | `device_fk` | DPASOFTWARE |
| `view_netport_v1` | `device_fk` | DPANETADAPTER |
| `view_ipaddress_v1` | `device_fk` | DPATCPIP |
| `view_mountpoint_v1` | `device_fk` | DPALOGICALDRIVE |
| `view_part_v1` | `device_fk` | DPACPU, DPADISK, DPAMEDIAADAPTER, DPACOMPUTER |
| `view_serviceinstance_v2` | `device_fk` | 대응 없음 |
| `view_appcomp_v1` | `device_fk` | 대응 없음 |
| `view_deviceurl_v1` | `device_fk` | 대응 없음 |
| `view_pdu_v1` | `device_fk` | 대응 없음 |

`view_device_v2` 는 `virtual_host_device_fk`, `host_chassis_device_fk`,
`vm_manager_device_fk` 로 자기 자신을 참조한다.

## 보강 조인용 뷰

| 뷰 | 조인 키 | 용도 |
| --- | --- | --- |
| `view_os_v1` | `view_deviceos_v1.os_fk = os_pk` | OS 이름·제조사 |
| `view_software_v1` | `view_softwareinuse_v1.software_fk = software_pk` | 소프트웨어명·분류 |
| `view_partmodel_v1` | `view_part_v1.partmodel_fk = partmodel_pk` | 파트 모델명·타입 |
| `view_vendor_v1` | 각 뷰의 `vendor_fk = vendor_pk` | 제조사명 |
| `view_hardware_v1` | `view_device_v2.hardware_fk = hardware_pk` | 하드웨어 모델명 |
| `view_subnet_v1` | `view_ipaddress_v1.subnet_fk = subnet_pk` | 넷마스크·게이트웨이 |
| `view_vlan_v1` | `view_subnet_v1.parent_vlan_fk = vlan_pk` | VLAN |
| `view_service_v2` | `view_serviceinstance_v2.service_fk = service_pk` | 서비스명 |

## 존재하지 않는 뷰

확인 결과 없는 것들이다. 재조사를 막기 위해 기록한다.

`view_macaddress_*`, `view_display_v1`, `view_monitor_v1`,
`view_customfieldvalue_v1`, `view_devicecustomfields_v1`,
`view_softwaresuite_v1`, `view_service_v1`, `view_serviceinstance_v1`,
`view_operatingsystem_v1`, `view_hardwaremodel_v1`

MAC 주소는 `view_netport_v1.hwaddress` 에 있다. 커스텀필드 값은
`view_device_v2.vendor_custom_fields` 컬럼에 있다.

## 건수

<Step 1 view-counts.txt 출력을 표로 옮긴다>
```

- [ ] **Step 4: `device-types.md` 작성**

```markdown
# Device 타입 체계

> 관측 <실행일> · Device42 <IP>
> 재조회 docs/data-analysis/exploration-queries/device42/device-type-distribution.sql

`view_device_v2.type` 과 서브타입이 적재 대상 판정과 ASSETCLASS 결정에 쓰인다.

## virtualsubtype_id

| id | virtualsubtype |
| --- | --- |
| 2 | Amazon EC2 Instance |
| 11 | VMWare |
| 14 | Hyper-V |
| 15 | Docker Container |

현행 필터가 `virtualsubtype_id <> 15` 로 Docker Container 를 제외한다.
근거: `DeployedAssetIntegrate.java` `DEVICE_FILTER`

## 분포

<Step 1 type-distribution.txt 출력을 표로 옮긴다>

## ASSETCLASS 판정

현행 코드는 타입이 아니라 플래그로 판정한다.
근거: `DeployedAssetIntegrate.java` `mapData()`

| 순서 | 조건 | ASSETCLASS |
| --- | --- | --- |
| 1 | `network_device = true` | NETDEVICE |
| 2 | `physicalsubtype = 'Network Printer'` | NETPRINTER |
| 3 | 그 외 | COMPUTER |

`type` 과 `virtualsubtype` 은 판정에 쓰이지 않는다. VMWare, Amazon EC2,
Hyper-V, physical Generic 이 모두 COMPUTER 로 합쳐진다.
```

- [ ] **Step 5: `servers.md` 작성**

```markdown
# Device42 서버별 데이터 성격

> 관측 <실행일> · Device42 <IP>
> 재조회 docs/data-analysis/exploration-queries/device42/source-coverage-by-subtype.sql

접속 대상은 `local/db-access-kit/connections.env` 의 `D42_RESOLVE` 로 전환한다.
`D42_BASE_URL` 은 인증서 이름과 맞추기 위해 `https://Device42Demo` 로 유지한다.

| 서버 | 용도 |
| --- | --- |
| 192.168.2.68 | 소프트웨어·파트·마운트 원천이 넓다 |
| 192.168.1.35 | 네트워크·OS 원천이 넓다 |

매핑 검증 시 한 서버로는 전 테이블을 덮지 못한다. 대상 테이블에 맞는
서버를 선택한다.

## subtype 별 원천 가용성

<Step 1 coverage-by-subtype.txt 출력을 표로 옮긴다>

## 식별자 충전율

<Step 1 identifier-fill-rate.txt 출력을 표로 옮긴다>

가상 장비는 `hardware_fk` 가 비어 있다. Device42 가 가상 장비에 하드웨어
모델을 부여하지 않는 구조이며 재수집으로 채워지지 않는다. 이 경우
`DEPLOYEDASSET.MANUFACTURER` 는 `MAXATTRIBUTE.DEFAULTVALUE` 인
`UNKNOWN` 이 된다.
```

- [ ] **Step 6: 관측 스탬프 누락 검사**

```bash
for f in views.md device-types.md servers.md; do
  head -5 docs/data-analysis/knowledge/device42/$f | grep -q '^> 관측' \
    && echo "OK $f" || echo "MISSING $f"
done
```

기대: 3개 모두 `OK`. `doql-constraints.md` 는 수치가 없으므로 스탬프가 없다.

- [ ] **Step 7: 커밋**

```bash
git add docs/data-analysis/knowledge/device42
git commit -m "docs: add Device42 views, device types, server coverage knowledge"
```

---

## Task 5: open-issues.md

**Files:**
- Create: `docs/data-analysis/open-issues.md`

**Interfaces:**
- Consumes: Task 3의 `deployedasset-model.md`, Task 4의 `device-types.md`·`servers.md`
- Produces: 다른 문서가 판단·의견이 필요할 때 이 파일 경로로 참조한다.

- [ ] **Step 1: 문서 작성**

```markdown
# 미결 사항

판단과 의견은 이 문서에만 둔다. 다른 문서는 관측된 값과 매핑 규칙만 기술한다.

## ISSUE-1 SOURCEID 가 서버 간 불일치

**상태:** 정책 정의 대기. 기록만 한다.

`DEPLOYEDASSET` 의 MERGE 키는 `(SOURCEID, IMPORTSOURCE)` 이고 `SOURCEID` 에는
Device42 `device_pk` 가 들어간다. `device_pk` 는 수집 서버가 다르거나
재수집하면 값이 바뀐다. 동일 장비가 서버에 따라 다른 `SOURCEID` 로 적재된다.

동일 장비 확인 근거는 `serial_no` 와 `uuid` 다. 두 값은 서버가 달라도 같다.

`DEPLOYEDASSET.TLOAMNRSUUID` 에 Device42 `uuid` 가 이미 적재된다. 다만
`uuid` 는 전건 존재하지 않는다. 충전율은
`knowledge/device42/servers.md` 의 식별자 충전율 표를 참조한다.

## ISSUE-2 스위치가 두 레코드로 분리됨

**상태:** 기록만 한다.

네트워크 장비가 Device42 에서 두 레코드로 나뉜다.

| 레코드 | 보유 | 미보유 |
| --- | --- | --- |
| `type = 'cluster'` | netport, ipaddress | serial_no |
| `type = 'physical'`, 이름에 ` - Switch N` 접미 | serial_no | netport, ipaddress |

현행 필터는 `type IN ('virtual','physical')` 이므로 physical 쪽만 적재된다.
포트와 IP 는 cluster 쪽에 있어 `DPANETDEVICE.NETWORKADDRESS` 와
`NETMACADDR` 의 원천이 없다.

두 레코드를 잇는 FK 는 없다. `host_chassis_device_fk`,
`virtual_host_device_fk`, `vm_manager_device_fk`, `chassisslot_fk` 가 모두
비어 있다. 이름 접미사 규칙 외에 연결 수단이 확인되지 않았다.

## ISSUE-3 PDU 가 COMPUTER 로 분류됨

**상태:** 수집 정책에 반영.

`physicalsubtype = 'PDU'` 인 장비가 현행 판정 순서에서 3번 분기로 떨어져
COMPUTER 가 된다. Maximo 에 PDU 용 ASSETCLASS 와 DPA 테이블이 없다.
수집 대상에서 제외하는 방향으로 정책에 반영한다.

판정 순서는 `knowledge/device42/device-types.md` 참조.

## 처리 완료

| 항목 | 결론 |
| --- | --- |
| 가상 장비 vendor 없음 | 이슈 아님. `MAXATTRIBUTE.DEFAULTVALUE` 가 `UNKNOWN` 으로 정의되어 있어 정상 결과다 |
```

- [ ] **Step 2: 참조 경로 유효성 확인**

```bash
grep -o 'knowledge/[a-z0-9/.-]*\.md' docs/data-analysis/open-issues.md | sort -u | \
  while read p; do [ -f "docs/data-analysis/$p" ] && echo "OK $p" || echo "BROKEN $p"; done
```

기대: 모두 `OK`.

- [ ] **Step 3: 커밋**

```bash
git add docs/data-analysis/open-issues.md
git commit -m "docs: record open issues on source id, switch split, PDU class"
```

---

## Task 6: data-mapping/README.md

**Files:**
- Create: `docs/data-analysis/data-mapping/README.md`

**Interfaces:**
- Consumes: Task 3의 `deployedasset-model.md` ASSETCLASS 라우팅 표
- Produces: Task 7이 생성하는 14개 문서의 진행 현황을 여기서 관리한다.

- [ ] **Step 1: 문서 작성**

```markdown
# 데이터 매핑

테이블 단위로 정리한다. 문서 한 장이 구현 클래스 하나에 대응한다.

| 문서 | 구현 |
| --- | --- |
| `asset/deployedasset.md` | `integration/asset/DeployedAssetIntegrate.java` |
| `asset/dpacomputer.md` | `integration/asset/DpaComputerIntegrate.java` |
| `asset/dpaos.md` | `integration/asset/DpaOsIntegrate.java` |
| `software/dpasoftware.md` | `integration/software/DpaSoftwareIntegrate.java` |

## 실행 순서

`DEPLOYEDASSET` 이 `NODEID` 를 발번한 뒤에야 자식 테이블을 적재할 수 있다.

1. `DEPLOYEDASSET`
2. `DPACOMPUTER` (COMPUTER 는 부모와 1:1)
3. 나머지 자식 테이블

## ASSETCLASS 라우팅

`DEPLOYEDASSET.ASSETCLASS` 가 적재 대상 자식 테이블을 결정한다.
판별자 구조는 `../knowledge/maximo/deployedasset-model.md` 참조.

| ASSETCLASS | 자식 테이블 |
| --- | --- |
| COMPUTER | DPACOMPUTER, DPAOS, DPASOFTWARE, DPACPU, DPADISK, DPALOGICALDRIVE, DPANETADAPTER, DPATCPIP, DPAMEDIAADAPTER, DPADISPLAY, DPASWSUITE |
| NETDEVICE | DPANETDEVICE |
| NETPRINTER | DPANETPRINTER |

## 진행 현황

`구분` 열 채움 상태 기준이다.

| 테이블 | Device42 원천 | 컬럼 매핑 |
| --- | --- | --- |
| DEPLOYEDASSET | `view_device_v2` | 미작성 |
| DPACOMPUTER | `view_device_v2`, `view_part_v1`(RAM) | 미작성 |
| DPAOS | `view_deviceos_v1`, `view_os_v1` | 미작성 |
| DPASOFTWARE | `view_softwareinuse_v1`, `view_software_v1` | 미작성 |
| DPACPU | `view_part_v1`(CPU) | 미작성 |
| DPADISK | `view_part_v1`(Hard Disk) | 미작성 |
| DPALOGICALDRIVE | `view_mountpoint_v1` | 미작성 |
| DPANETADAPTER | `view_netport_v1` | 미작성 |
| DPATCPIP | `view_ipaddress_v1`, `view_subnet_v1` | 미작성 |
| DPAMEDIAADAPTER | `view_part_v1`(GPU) | 미작성 |
| DPANETDEVICE | 원천 미확정 | 미작성 |
| DPANETPRINTER | `view_device_v2`, `view_part_v1`(printer_*) | 미작성 |
| DPADISPLAY | 없음 | 해당 없음 |
| DPASWSUITE | 없음 | 해당 없음 |

`DPANETDEVICE` 의 원천 미확정 사유는 `../open-issues.md` ISSUE-2 참조.

## 원천이 없는 테이블

문서는 만들되 원천 없음과 사유를 기록한다. 문서가 없으면 미조사와
구분되지 않는다.

| 테이블 | 사유 |
| --- | --- |
| DPADISPLAY | 모니터 정보. Device42 에 대응 뷰와 데이터가 없다 |
| DPASWSUITE | 소프트웨어 스위트 묶음. Device42 에 suite 개념이 없다 |
```

- [ ] **Step 2: 커밋**

```bash
git add docs/data-analysis/data-mapping/README.md
git commit -m "docs: add data-mapping index with routing and progress"
```

---

## Task 7: 매핑 문서 14장 골격 생성

**Files:**
- Create: `docs/data-analysis/tools/gen-mapping-skeleton.py`
- Create: `docs/data-analysis/data-mapping/asset/*.md` (13장)
- Create: `docs/data-analysis/data-mapping/software/dpasoftware.md`

**Interfaces:**
- Consumes: Task 1의 `column-skeleton.tsv`(8열: OBJECTNAME, ATTRIBUTENAME, KO_TITLE, MAXTYPE, LENGTH, SCALE, REQUIRED, DEFAULTVALUE), `table-description.tsv`(2열: OBJECTNAME, KO_DESC)
- Produces: 14개 매핑 문서. 각 문서의 `## 4. 컬럼 매핑` 표는 7열이며 앞 4열이 채워지고 `구분`/`Source`/`변환·조건` 은 비어 있다.

- [ ] **Step 1: 메타데이터 재조회**

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/column-skeleton.sql \
  local/db-access-kit/work/maximo
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/table-description.sql \
  local/db-access-kit/work/maximo
```

- [ ] **Step 2: `docs/data-analysis/tools/gen-mapping-skeleton.py` 작성**

```python
#!/usr/bin/env python3
"""Maximo 메타데이터 TSV 로 매핑 문서 골격을 생성한다.

입력:
  local/db-access-kit/work/maximo/column-skeleton.tsv
  local/db-access-kit/work/maximo/table-description.tsv
출력:
  docs/data-analysis/data-mapping/<패키지>/<테이블 소문자>.md

이미 존재하는 문서는 건너뛴다. 분석으로 채운 내용을 덮어쓰지 않는다.
저장소 루트에서 실행한다.
"""
import csv
import sys
from pathlib import Path

WORK = Path("local/db-access-kit/work/maximo")
OUT = Path("docs/data-analysis/data-mapping")

# 테이블 -> (패키지, 구현 클래스, ASSETCLASS)
TABLES = {
    "DEPLOYEDASSET":   ("asset", "DeployedAssetIntegrate", "COMPUTER, NETDEVICE, NETPRINTER"),
    "DPACOMPUTER":     ("asset", "DpaComputerIntegrate", "COMPUTER"),
    "DPAOS":           ("asset", "DpaOsIntegrate", "COMPUTER"),
    "DPACPU":          ("asset", "DpaCpuIntegrate", "COMPUTER"),
    "DPADISK":         ("asset", "DpaDiskIntegrate", "COMPUTER"),
    "DPALOGICALDRIVE": ("asset", "DpaLogicalDriveIntegrate", "COMPUTER"),
    "DPANETADAPTER":   ("asset", "DpaNetAdapterIntegrate", "COMPUTER"),
    "DPATCPIP":        ("asset", "DpaTcpIpIntegrate", "COMPUTER"),
    "DPAMEDIAADAPTER": ("asset", "DpaMediaAdapterIntegrate", "COMPUTER"),
    "DPADISPLAY":      ("asset", "DpaDisplayIntegrate", "COMPUTER"),
    "DPASWSUITE":      ("asset", "DpaSwSuiteIntegrate", "COMPUTER"),
    "DPANETDEVICE":    ("asset", "DpaNetDeviceIntegrate", "NETDEVICE"),
    "DPANETPRINTER":   ("asset", "DpaNetPrinterIntegrate", "NETPRINTER"),
    "DPASOFTWARE":     ("software", "DpaSoftwareIntegrate", "COMPUTER"),
}


def read_tsv(path):
    with path.open(encoding="utf-8") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def clean(value):
    """실행기가 빈 문자열을 큰따옴표 두 개로 출력하는 경우를 정리한다."""
    v = (value or "").strip()
    return "" if v == '""' else v


def type_label(row):
    """SCALE 이 0 이 아니면 MAXTYPE(LENGTH,SCALE) 로 적는다.

    DECIMAL 계열은 SCALE 없이는 반올림 자리수를 복원할 수 없다.
    대상 테이블에 DECIMAL(10,2) 컬럼이 13개 있다.
    """
    maxtype = clean(row["MAXTYPE"])
    length = clean(row["LENGTH"])
    scale = clean(row["SCALE"])
    if not length:
        return maxtype
    if scale and scale != "0":
        return f"{maxtype}({length},{scale})"
    return f"{maxtype}({length})"


def build(table, desc, columns):
    _package, impl, assetclass = TABLES[table]
    lines = [
        f"# {table}",
        "",
        desc or "(설명 없음)",
        "",
        f"> Target: MAXIMO.{table} · ASSETCLASS: {assetclass} · 구현: {impl}.java",
        "",
        "## 1. 관계",
        "",
    ]
    if table == "DEPLOYEDASSET":
        lines += [
            "- 계층의 루트. 부모 없음.",
            "- `NODEID` 는 `MAXIMO.DEPLOYEDASSETSEQ` 로 발번한다.",
            "- 적재 대상 필터와 키 전략은 3번에 기술한다.",
        ]
    else:
        lines += [
            "- 부모: MAXIMO.DEPLOYEDASSET (NODEID)",
            f"- 카디널리티: DEPLOYEDASSET 1 : <1|N> {table}  <!-- MERGE 키로 확정한다 -->",
            "- 선행: DEPLOYEDASSET",
        ]
    lines += [
        "",
        "## 2. 테이블 매핑",
        "",
        "| Source | Target | 조인 조건 | 카디널리티 |",
        "| --- | --- | --- | --- |",
        f"|  | MAXIMO.{table} |  |  |",
        "",
        "## 3. 조회 조건",
        "",
        "| 조건 | 식 | 사유 |",
        "| --- | --- | --- |",
        "|  |  |  |",
        "",
        "## 4. 컬럼 매핑",
        "",
        "| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for row in columns:
        nullable = "N" if clean(row["REQUIRED"]) == "1" else "Y"
        default = clean(row["DEFAULTVALUE"])
        note = f"DEFAULTVALUE={default}" if default else ""
        lines.append(
            f"| {clean(row['ATTRIBUTENAME'])} | {clean(row['KO_TITLE'])} "
            f"| {type_label(row)} | {nullable} |  |  | {note} |"
        )
    lines += [
        "",
        "구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결",
        "",
        "## 5. 조회 쿼리",
        "",
        "3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.",
        "",
        "## 6. 미결",
        "",
        "`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.",
        "",
    ]
    return "\n".join(lines) + "\n"


def main():
    col_rows = read_tsv(WORK / "column-skeleton.tsv")
    desc_rows = read_tsv(WORK / "table-description.tsv")
    descs = {r["OBJECTNAME"]: clean(r["KO_DESC"]) for r in desc_rows}

    by_table = {}
    for row in col_rows:
        by_table.setdefault(row["OBJECTNAME"], []).append(row)

    missing = sorted(set(TABLES) - set(by_table))
    if missing:
        print(f"메타데이터 없음: {', '.join(missing)}", file=sys.stderr)
        return 1

    created = skipped = 0
    for table, (package, _impl, _cls) in TABLES.items():
        path = OUT / package / f"{table.lower()}.md"
        if path.exists():
            skipped += 1
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(build(table, descs.get(table, ""), by_table[table]),
                        encoding="utf-8")
        created += 1
    print(f"생성 {created}건, 건너뜀 {skipped}건")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 3: 실행**

```bash
python3 docs/data-analysis/tools/gen-mapping-skeleton.py
```

기대: `생성 14건, 건너뜀 0건`

- [ ] **Step 4: 파일 수 검증**

```bash
ls docs/data-analysis/data-mapping/asset/*.md | wc -l
ls docs/data-analysis/data-mapping/software/*.md | wc -l
```

기대: `13`, `1`

- [ ] **Step 5: 컬럼 행 수 검증**

컬럼 표의 데이터 행만 센다. 정규식이 전부 대문자인 컬럼명 행에만 걸리므로
`| Target 컬럼 |` 헤더는 세지 않는다.

```bash
for t in dpaos dpacomputer deployedasset dpasoftware; do
  d=asset; [ "$t" = dpasoftware ] && d=software
  n=$(sed -n '/^## 4\./,/^## 5\./p' docs/data-analysis/data-mapping/$d/$t.md \
      | grep -cE '^\| [A-Z][A-Z0-9_]* \|')
  echo "$t $n"
done
```

기대: `dpaos 15`, `dpacomputer 42`, `deployedasset 39`, `dpasoftware 29`

- [ ] **Step 6: 재실행 시 덮어쓰지 않는지 검증**

```bash
python3 docs/data-analysis/tools/gen-mapping-skeleton.py
```

기대: `생성 0건, 건너뜀 14건`

- [ ] **Step 7: 원천 없는 두 문서에 사유 기재**

원천 없음은 조사로 확정된 사실이므로 `6. 미결` 이 아니라 `2. 테이블 매핑`
아래에 적는다. `6. 미결` 은 이슈 링크 전용이다.

`docs/data-analysis/data-mapping/asset/dpadisplay.md` 의 `## 2. 테이블 매핑`
표 아래에 추가한다.

```markdown
Device42 에 대응 원천이 없다. 모니터 정보를 담는 뷰와 데이터가 확인되지
않았다. 다른 수집 도구가 채우는 영역이다.
```

`docs/data-analysis/data-mapping/asset/dpaswsuite.md` 의 `## 2. 테이블 매핑`
표 아래에 추가한다.

```markdown
Device42 에 대응 원천이 없다. 소프트웨어를 스위트로 묶는 개념과 뷰가
확인되지 않았다. 다른 수집 도구가 채우는 영역이다.
```

- [ ] **Step 8: 커밋**

```bash
git add docs/data-analysis/tools docs/data-analysis/data-mapping
git commit -m "docs: generate 14 mapping doc skeletons from Maximo metadata"
```

---

## Task 8: 진입점 문서와 세션 연결

spec 1절의 "세션이 바뀌어도 이전 조사 결과를 이어받을 수 있어야 한다" 를
실현하는 태스크다. `CLAUDE.md` 는 새 세션에서 자동 로드되므로 여기서
문서 위치를 알린다.

**Files:**
- Create: `docs/data-analysis/README.md`
- Create: `CLAUDE.md`

**Interfaces:**
- Consumes: Task 1~7이 생성한 전체 문서 트리

- [ ] **Step 1: `docs/data-analysis/README.md` 작성**

````markdown
# Device42 → Maximo 데이터 분석

Device42(원천)와 Maximo(타겟) 사이의 매핑 작업 문서다.

## 읽는 순서

1. `knowledge/maximo/deployedasset-model.md` — 타겟 구조와 ASSETCLASS 판별자
2. `knowledge/device42/views.md` — 원천 뷰와 device 연결 키
3. `data-mapping/README.md` — 라우팅과 진행 현황
4. `data-mapping/<패키지>/<테이블>.md` — 테이블별 매핑

## 구성

| 디렉터리 | 내용 |
| --- | --- |
| `knowledge/` | 관측 사실. 수치는 스냅샷이며 상단에 관측 시점과 재조회 쿼리를 명시한다 |
| `data-mapping/` | 테이블 단위 매핑 정본 |
| `exploration-queries/` | 재사용 조회 쿼리 |
| `tools/` | 매핑 문서 골격 생성 |
| `open-issues.md` | 미결·정책 대기 항목 |

접속 수단은 `local/db-access-kit/` 에 있다. git 추적 대상이 아니다.

## 작성 규칙

- 관측된 값과 매핑 규칙만 기술한다. 판단과 의견은 `open-issues.md` 에 둔다.
- 같은 사실을 반복하지 않는다. 결론에 필요한 수치만 남긴다.
- 테이블·컬럼은 한글명을 함께 적고, Source 와 조건은 실행 가능한 식으로 쓴다.
- pk 리터럴에 의존하지 않는다. 원천 pk 는 재수집 시 바뀐다.
- 수치를 담는 문서는 상단에 관측 스탬프를 단다.

## 컬럼 매핑 표

7열 고정이다. 앞 4열은 Maximo 메타데이터에서 생성한다.

| 열 | 출처 |
| --- | --- |
| Target 컬럼 | `MAXATTRIBUTE.ATTRIBUTENAME` |
| 한글명 | `L_MAXATTRIBUTE.TITLE` (LANGCODE='KO') |
| 타입 | `MAXATTRIBUTE.MAXTYPE` + `LENGTH` |
| Null | `MAXATTRIBUTE.REQUIRED` 반전 |
| 구분 | 분석. 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결 |
| Source | 분석. Device42 뷰.컬럼 |
| 변환·조건 | 분석. 변환식, 기본값, 조건 |

골격 생성:

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/column-skeleton.sql \
  local/db-access-kit/work/maximo
python3 docs/data-analysis/tools/gen-mapping-skeleton.py
```

설계 문서는 `../superpowers/specs/2026-08-27-data-analysis-docs-design.md` 다.
````

- [ ] **Step 2: `CLAUDE.md` 작성**

```markdown
# discovery-interface

Device42 에서 수집한 자산 정보를 Maximo 로 적재하는 Spring Boot 연동
애플리케이션이다.

## 데이터 분석 문서

원천·타겟 구조와 매핑은 `docs/data-analysis/` 에 있다. 매핑 관련 작업을
시작하기 전에 `docs/data-analysis/README.md` 를 먼저 읽는다.

## DB 접속

접속 수단은 `local/db-access-kit/` 에 있다. git 추적 대상이 아니며 실제
자격정보를 포함한다. 폴더 내용을 응답, 로그, 커밋에 옮기지 않는다.
실행 규칙은 `local/db-access-kit/AGENTS.md` 를 따른다.

조회 쿼리는 `docs/data-analysis/exploration-queries/` 에 있다. 실행 결과는
`local/db-access-kit/work/` 아래에만 둔다.

## 작업 규칙

- 원천 pk 에 의존하는 코드나 문서를 만들지 않는다. Device42 `device_pk` 는
  수집 서버와 재수집 시점에 따라 바뀐다.
- Maximo 는 읽기 확인만 한다. 쓰기가 필요하면 대상과 복구 방법을 확인한 뒤
  진행한다.
```

- [ ] **Step 3: 전체 트리 확인**

```bash
find docs/data-analysis -type f | sort | wc -l
find docs/data-analysis -type f | sort
```

기대: 33개 파일.

| 경로 | 개수 |
| --- | --- |
| `README.md` | 1 |
| `open-issues.md` | 1 |
| `knowledge/` | 6 |
| `data-mapping/` | 15 (README 1 + 테이블 14) |
| `exploration-queries/` | 9 (README 1 + device42 5 + maximo 3) |
| `tools/` | 1 |

- [ ] **Step 4: 문서 내 상대 경로 링크 검증**

```bash
python3 - <<'PY'
import re, pathlib
root = pathlib.Path("docs/data-analysis")
bad = []
for md in root.rglob("*.md"):
    for m in re.findall(r'`((?:\.\./|[a-z])[a-z0-9/._-]*\.md)`', md.read_text(encoding="utf-8")):
        if not (md.parent / m).resolve().exists():
            bad.append(f"{md}: {m}")
print("\n".join(bad) if bad else "링크 이상 없음")
PY
```

기대: `링크 이상 없음`

- [ ] **Step 5: 커밋**

```bash
git add docs/data-analysis/README.md CLAUDE.md
git commit -m "docs: add data-analysis entry point and CLAUDE.md guidance"
```
