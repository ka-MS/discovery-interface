# OS·Disk·Filesystem·IP CI 매핑 구성 실행 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** OS·Disk·Filesystem·IP 네 유형의 ACTCI 분류와 수집 스펙을 정하고 매핑 문서를 작성한다.

**Architecture:** 조사 → 설계 → 매핑 순서다. 원천(D42)과 타겟(Maximo)을 각각 읽기 전용으로 조사해 `knowledge/`에 관측 사실을 남기고, 분류 선택과 스펙 대조를 `design/ci/<유형>.md`에, 확정 매핑을 `data-mapping/ci/types/<유형>.md`에 쓴다. 관계는 추천안까지만 쓴다.

**Tech Stack:** Device42 DOQL REST API, Db2(Maximo) 읽기 전용 실행기(`local/db-access-kit/scripts/`), Markdown 문서.

**Spec:** [docs/superpowers/specs/2026-09-15-ci-os-disk-fs-ip-design.md](../specs/2026-09-15-ci-os-disk-fs-ip-design.md)

## Global Constraints

- 적재 구현 코드를 쓰지 않는다. `src/` 아래를 수정해야 할 이유가 생기면 범위 이탈이므로 멈추고 보고한다.
- Maximo 쓰기 일체 금지. 조사는 `run-maximo.sh`(SELECT·VALUES만 허용) 로만 한다.
- 기준정보 등록 SQL을 작성·실행하지 않는다.
- 문서의 모든 수치는 실행 결과에서 옮긴다. 추정치를 관측치로 적지 않는다.
- D42 조사는 `.68`과 `.35` **양쪽**에서 실행하고 어느 서버 관측인지 함께 적는다.
- 쿼리 출력은 `local/db-access-kit/work/` 아래에만 둔다. git 추적 대상이 아니다.
- DOQL 제약: `FROM` 절 서브쿼리 금지(CTE로 대체), 카탈로그 조회 금지, 블록 이름은 `[a-zA-Z0-9-]`만, 주석은 첫 `-- name:` 앞에만, `inet` 컬럼은 `HOST()`/`CAST(... AS VARCHAR)` 필요.
- 뷰는 높은 버전을 쓴다. `_v2`가 있으면 그쪽이다.
- 6절 판단 규칙(스펙 문서)을 따른다. 분류 후보가 1개가 아니면 매핑 문서를 쓰지 않는다.
- 커밋 접두어는 `docs:`. 조사·설계·매핑을 분리해 커밋한다.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>` 를 붙인다.

### 실행 명령

```bash
DB_ACCESS_ENV=local/db-access-kit/connections-d42-68.env \
  bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> local/db-access-kit/work/device42-68
DB_ACCESS_ENV=local/db-access-kit/connections-d42-35.env \
  bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> local/db-access-kit/work/device42-35
bash local/db-access-kit/scripts/run-maximo.sh <쿼리파일> local/db-access-kit/work/maximo
```

### Computer 대상 필터 (재사용)

`ComputerCiIntegrate.COMPUTER_FILTER`와 같아야 한다. 별칭은 `d`다.

```sql
d.type IN ('physical', 'virtual')
AND (d.network_device = false OR d.network_device IS NULL)
AND (
    (d.type = 'physical' AND d.physicalsubtype IN
        ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
    OR
    (d.type = 'virtual' AND d.virtualsubtype IN
        ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
)
```

---

### Task 1: D42 원천 형태 확인

**Files:**
- Create: `docs/data-analysis/exploration-queries/device42/ci-component-source.sql`

**Interfaces:**
- Produces: 네 유형 뷰의 실제 컬럼 목록. Task 2 이후 모든 쿼리가 이 컬럼명을 쓴다. 특히 `view_part_v1`의 PK 컬럼명은 기존 문서에 없으므로 여기서 확정한다.

- [x] **Step 1: 형태 조회 쿼리 작성**

`view_part_v1`의 PK 컬럼명이 문서에 없다. `SELECT *`로 헤더를 받아 확인한다.

```sql
-- 네 유형 원천 뷰의 컬럼 목록과 표본 1행. 카탈로그 조회가 막혀 있어 헤더로 확인한다.
-- name: os-shape
SELECT * FROM view_deviceos_v1 LIMIT 1;

-- name: os-product-shape
SELECT * FROM view_os_v1 LIMIT 1;

-- name: part-shape
SELECT * FROM view_part_v1 LIMIT 1;

-- name: partmodel-shape
SELECT * FROM view_partmodel_v1 LIMIT 1;

-- name: mount-shape
SELECT * FROM view_mountpoint_v2 LIMIT 1;

-- name: ip-shape
SELECT * FROM view_ipaddress_v2 LIMIT 1;

-- name: subnet-shape
SELECT * FROM view_subnet_v1 LIMIT 1;

-- name: netport-shape
SELECT * FROM view_netport_v1 LIMIT 1;
```

- [x] **Step 2: 두 서버에서 실행**

```bash
DB_ACCESS_ENV=local/db-access-kit/connections-d42-68.env \
  bash local/db-access-kit/scripts/run-device42.sh \
  docs/data-analysis/exploration-queries/device42/ci-component-source.sql \
  local/db-access-kit/work/device42-68
DB_ACCESS_ENV=local/db-access-kit/connections-d42-35.env \
  bash local/db-access-kit/scripts/run-device42.sh \
  docs/data-analysis/exploration-queries/device42/ci-component-source.sql \
  local/db-access-kit/work/device42-35
```

- [x] **Step 3: 결과 확인**

블록 8개 모두 `.tsv` 파일이 생겼는지 본다. 블록 이름에 언더스코어가 있으면 조용히 무시되므로 **파일 개수로 확인한다**.
500이 난 뷰는 이름 또는 권한 문제다. `view-version-probe.sql`로 상위 버전을 찾아 쿼리를 고친다.

```bash
ls local/db-access-kit/work/device42-68/*.tsv | wc -l   # 8이어야 한다
head -1 local/db-access-kit/work/device42-68/part-shape.tsv
```

- [x] **Step 4: 커밋**

```bash
git add docs/data-analysis/exploration-queries/device42/ci-component-source.sql
git commit -m "$(cat <<'EOF'
docs: OS·Disk·FS·IP 원천 뷰 형태 조회 쿼리 추가

네 유형 CI의 원천 뷰 컬럼을 확인하는 블록을 등록했다.
카탈로그 조회가 막혀 있어 표본 1행의 헤더로 확인한다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: D42 원천 값 조사와 관측 문서

**Files:**
- Modify: `docs/data-analysis/exploration-queries/device42/ci-component-source.sql`
- Create: `docs/data-analysis/knowledge/device42/ci-component-inventory.md`

**Interfaces:**
- Consumes: Task 1이 확정한 컬럼명.
- Produces: 유형별 원천 PK 유일성, Computer 연결분/전체 건수, 값 보유율. Task 5·6의 수집 대상 범위 결정 근거다.

- [x] **Step 1: 집계 블록 추가**

Task 1에서 확인한 실제 컬럼명으로 아래를 작성한다. `<part_pk>`는 Task 1 Step 3에서 확인한 이름으로 바꾼다.
`FROM` 절 서브쿼리가 금지되므로 집계는 전부 CTE로 쓴다.

```sql
-- name: pk-uniqueness
WITH os AS (SELECT COUNT(*) AS n, COUNT(DISTINCT deviceos_pk) AS d FROM view_deviceos_v1),
     mnt AS (SELECT COUNT(*) AS n, COUNT(DISTINCT mountpoint_pk) AS d FROM view_mountpoint_v2),
     ipa AS (SELECT COUNT(*) AS n, COUNT(DISTINCT ipaddress_pk) AS d FROM view_ipaddress_v2),
     dsk AS (SELECT COUNT(*) AS n, COUNT(DISTINCT <part_pk>) AS d FROM view_part_v1 WHERE type_name = 'Hard Disk')
SELECT 'os' AS entity, n AS row_count, d AS distinct_pk FROM os
UNION ALL SELECT 'filesystem', n, d FROM mnt
UNION ALL SELECT 'ip', n, d FROM ipa
UNION ALL SELECT 'disk', n, d FROM dsk;

-- name: computer-attachment
WITH computer AS (
    SELECT d.device_pk FROM view_device_v2 d
    WHERE d.type IN ('physical', 'virtual')
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (
          (d.type = 'physical' AND d.physicalsubtype IN
              ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
          OR
          (d.type = 'virtual' AND d.virtualsubtype IN
              ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
      )
), os AS (
    SELECT COUNT(*) AS total,
        COUNT(*) FILTER (WHERE EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = o.device_fk)) AS on_computer
    FROM view_deviceos_v1 o
), mnt AS (
    SELECT COUNT(*) AS total,
        COUNT(*) FILTER (WHERE EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = ANY(m.device_fks))) AS on_computer
    FROM view_mountpoint_v2 m
), ipa AS (
    SELECT COUNT(*) AS total,
        COUNT(*) FILTER (WHERE EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = ANY(i.device_fks))) AS on_computer
    FROM view_ipaddress_v2 i
), dsk AS (
    SELECT COUNT(*) AS total,
        COUNT(*) FILTER (WHERE EXISTS (SELECT 1 FROM computer c WHERE c.device_pk = p.device_fk)) AS on_computer
    FROM view_part_v1 p WHERE p.type_name = 'Hard Disk'
)
SELECT 'os' AS entity, total, on_computer FROM os
UNION ALL SELECT 'filesystem', total, on_computer FROM mnt
UNION ALL SELECT 'ip', total, on_computer FROM ipa
UNION ALL SELECT 'disk', total, on_computer FROM dsk;

-- name: os-value-coverage
SELECT COUNT(*) AS row_count,
    COUNT(o.os_name) AS has_name,
    COUNT(o.os_version) AS has_version,
    COUNT(o.os_version_no) AS has_version_no,
    COUNT(o.os_fk) AS has_product,
    COUNT(DISTINCT o.device_fk) AS device_count
FROM view_deviceos_v1 o;

-- name: os-per-device
WITH per AS (
    SELECT o.device_fk, COUNT(*) AS n FROM view_deviceos_v1 o GROUP BY o.device_fk
)
SELECT MAX(n) AS max_os_per_device, COUNT(*) FILTER (WHERE n > 1) AS device_with_multiple FROM per;

-- name: mount-value-coverage
SELECT COUNT(*) AS row_count,
    COUNT(m.mountpoint) AS has_mountpoint,
    COUNT(m.filesystem) AS has_filesystem,
    COUNT(m.fstype_name) AS has_fstype,
    COUNT(m.capacity) AS has_capacity,
    COUNT(m.free_capacity) AS has_free,
    COUNT(m.label) AS has_label
FROM view_mountpoint_v2 m;

-- name: ip-value-coverage
SELECT COUNT(*) AS row_count,
    COUNT(i.ip_address) AS has_ip,
    COUNT(i.netport_fk) AS has_port,
    COUNT(i.subnet_fk) AS has_subnet,
    COUNT(i.is_shared) FILTER (WHERE i.is_shared) AS shared_count,
    COUNT(b.mask_bits) AS has_mask,
    COUNT(b.gateway) AS has_gateway
FROM view_ipaddress_v2 i
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk;

-- name: disk-value-coverage
SELECT COUNT(*) AS row_count,
    COUNT(p.serial_no) AS has_serial,
    COUNT(p.pcount) AS has_pcount,
    COUNT(pm.name) AS has_model,
    COUNT(pm.hdsize) AS has_size,
    COUNT(pm.hdsize_unit) AS has_size_unit,
    COUNT(pm.hddtype_name) AS has_hddtype,
    COUNT(pm.media_type_name) AS has_media,
    COUNT(pm.vendor_fk) AS has_vendor
FROM view_part_v1 p
LEFT JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
WHERE p.type_name = 'Hard Disk';

-- name: disk-pcount-distribution
SELECT p.pcount, COUNT(*) AS row_count
FROM view_part_v1 p WHERE p.type_name = 'Hard Disk'
GROUP BY p.pcount ORDER BY p.pcount;
```

- [x] **Step 2: 두 서버에서 실행**

Task 1 Step 2와 같은 명령을 다시 쓴다.

- [x] **Step 3: 결과 확인**

새 블록 8개의 `.tsv`가 생겼는지 본다. `FILTER (WHERE ...)`가 500이면 `SUM(CASE WHEN ... THEN 1 ELSE 0 END)`로 바꿔 다시 실행한다.

- [x] **Step 4: 관측 문서 작성**

`docs/data-analysis/knowledge/device42/ci-component-inventory.md`를 만든다. 상단에 관측 스탬프와 재조회 쿼리 링크를 단다.

```markdown
# OS·Disk·Filesystem·IP 원천 조사

> 관측 2026-09-15 · D42 .68 / .35
> 재조회 [원천 형태·값 분포](../../data-analysis/exploration-queries/device42/ci-component-source.sql)

Computer 연관 범위의 연결 키·건수는 [Computer 연관 수집 원천](../../data-analysis/knowledge/device42/computer-inventory.md)에 있다.
이 문서는 네 유형을 독립 CI로 다루기 위해 필요한 원천 PK 유일성, Computer 외 연결분,
유형별 전체 컬럼과 값 보유율을 다룬다.
```

절 구성: 1. 뷰와 컬럼 / 2. 원천 PK 유일성 / 3. Computer 연결분과 전체 / 4. 값 보유율 / 5. 유형별 특이사항.
수치는 `.68 / .35` 두 값을 나란히 적는다. 한쪽만 적지 않는다.
`pcount`가 수량인지 개체 수인지에 대한 판단 근거를 5절에 남긴다.

- [x] **Step 5: 커밋**

```bash
git add docs/data-analysis/exploration-queries/device42/ci-component-source.sql \
        docs/data-analysis/knowledge/device42/ci-component-inventory.md
git commit -m "$(cat <<'EOF'
docs: OS·Disk·FS·IP 원천 값 분포와 PK 유일성 조사

원천 PK 유일성, Computer 연결분과 전체 건수, 값 보유율을 두 서버에서
조회해 정리했다. 수집 대상 범위 결정의 근거 자료다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Maximo ACTCI 분류 후보 조사

**Files:**
- Create: `docs/data-analysis/exploration-queries/maximo/ci-component-classifications.sql`
- Create: `docs/data-analysis/knowledge/maximo/ci-component-classifications.md`

**Interfaces:**
- Produces: 유형별 ACTCI 분류 후보 목록과 각 후보의 스펙·자료형·적용 설정. Task 4의 입력이고, Task 5의 분류 선택 근거다.

- [x] **Step 1: 분류 탐색 쿼리 작성**

ACTCI 적용 분류는 1,030개다. 이름으로 후보를 좁힌 뒤 각 후보의 스펙을 본다.

```sql
-- OS·Disk·Filesystem·IP에 쓸 ACTCI 분류 후보와 그 스펙을 찾는다.
-- 후보 키워드는 candidate-classifications 블록의 LIKE 목록으로 바꾼다.
-- name: candidate-classifications
SELECT s.CLASSIFICATIONID, s.CLASSSTRUCTUREID, s.DESCRIPTION,
    (SELECT COUNT(*) FROM MAXIMO.CLASSSPEC c WHERE c.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID) AS SPEC_COUNT,
    (SELECT COUNT(*) FROM MAXIMO.CLASSSPECUSEWITH u
       WHERE u.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID AND u.OBJECTNAME = 'ACTCI' AND u.USEINSPEC = 1) AS ACTCI_SPEC_COUNT
FROM MAXIMO.CLASSSTRUCTURE s
WHERE EXISTS (SELECT 1 FROM MAXIMO.CLASSUSEWITH cu
        WHERE cu.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID AND cu.OBJECTNAME = 'ACTCI')
  AND (s.CLASSIFICATIONID LIKE '%OPERATINGSYSTEM%' OR s.CLASSIFICATIONID LIKE '%OSINSTALL%'
    OR s.CLASSIFICATIONID LIKE '%FILESYSTEM%' OR s.CLASSIFICATIONID LIKE '%DISK%'
    OR s.CLASSIFICATIONID LIKE '%STORAGE%' OR s.CLASSIFICATIONID LIKE '%VOLUME%'
    OR s.CLASSIFICATIONID LIKE '%IPADDR%' OR s.CLASSIFICATIONID LIKE '%IPINTERFACE%'
    OR s.CLASSIFICATIONID LIKE '%NETWORK%')
ORDER BY s.CLASSIFICATIONID;

-- name: all-actci-classifications
SELECT s.CLASSIFICATIONID, s.CLASSSTRUCTUREID, s.DESCRIPTION
FROM MAXIMO.CLASSSTRUCTURE s
WHERE EXISTS (SELECT 1 FROM MAXIMO.CLASSUSEWITH cu
        WHERE cu.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID AND cu.OBJECTNAME = 'ACTCI')
ORDER BY s.CLASSIFICATIONID;

-- name: candidate-specs
SELECT s.CLASSIFICATIONID, c.CLASSSTRUCTUREID, c.CLASSSPECID, c.ASSETATTRID, c.SECTION,
    a.DATATYPE, c.MEASUREUNITID, u.SEQUENCE, u.MANDATORY, u.USEINSPEC,
    c.ORGID, c.SITEID
FROM MAXIMO.CLASSSTRUCTURE s
JOIN MAXIMO.CLASSSPEC c ON c.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID
LEFT JOIN MAXIMO.ASSETATTRIBUTE a
  ON a.ASSETATTRIBUTEID = c.ASSETATTRIBUTEID AND a.ASSETATTRID = c.ASSETATTRID
LEFT JOIN MAXIMO.CLASSSPECUSEWITH u
  ON u.CLASSSPECID = c.CLASSSPECID AND u.OBJECTNAME = 'ACTCI'
WHERE s.CLASSIFICATIONID LIKE '%OPERATINGSYSTEM%' OR s.CLASSIFICATIONID LIKE '%FILESYSTEM%'
   OR s.CLASSIFICATIONID LIKE '%DISK%' OR s.CLASSIFICATIONID LIKE '%IPADDR%'
   OR s.CLASSIFICATIONID LIKE '%IPINTERFACE%'
ORDER BY s.CLASSIFICATIONID, c.ASSETATTRID;

-- name: attribute-name-search
SELECT a.ASSETATTRIBUTEID, a.ASSETATTRID, a.DATATYPE, a.MEASUREUNITID, a.ORGID, a.SITEID
FROM MAXIMO.ASSETATTRIBUTE a
WHERE a.ASSETATTRID LIKE 'OPERATINGSYSTEM%' OR a.ASSETATTRID LIKE 'FILESYSTEM%'
   OR a.ASSETATTRID LIKE 'DISK%' OR a.ASSETATTRID LIKE 'IPADDRESS%'
   OR a.ASSETATTRID LIKE 'IPINTERFACE%' OR a.ASSETATTRID LIKE 'STORAGE%'
ORDER BY a.ASSETATTRID;
```

- [x] **Step 2: 실행**

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/ci-component-classifications.sql \
  local/db-access-kit/work/ci-component-classifications
```

- [x] **Step 3: 후보 판정**

`candidate-classifications.tsv`가 비었으면 `all-actci-classifications.tsv`를 직접 훑어 후보를 찾고 Step 1의 LIKE 목록을 고쳐 다시 실행한다.
유형별 후보 수를 세어 스펙 6절 규칙을 적용한다.

```bash
wc -l local/db-access-kit/work/ci-component-classifications/*.tsv
```

- [x] **Step 4: 관측 문서 작성**

`docs/data-analysis/knowledge/maximo/ci-component-classifications.md`를 만든다.

```markdown
# OS·Disk·Filesystem·IP 분류 조사

> 관측 2026-09-15 · Maximo BLUDB
> 재조회 [분류 후보·스펙](../../data-analysis/exploration-queries/maximo/ci-component-classifications.sql)
```

절 구성: 1. 유형별 후보 분류 / 2. 후보별 스펙·자료형·단위 / 3. ACTCI 적용 설정 유무 / 4. 미등록 속성.
후보가 0개인 유형은 그 사실을 명시한다. "없음"도 관측 결과다.
ORGID·SITEID가 지정된 스펙이 있으면 별도로 표기한다. 적재 코드는 전역 템플릿만 읽는다.

- [x] **Step 5: 커밋**

```bash
git add docs/data-analysis/exploration-queries/maximo/ci-component-classifications.sql \
        docs/data-analysis/knowledge/maximo/ci-component-classifications.md
git commit -m "$(cat <<'EOF'
docs: OS·Disk·FS·IP ACTCI 분류 후보와 스펙 조사

네 유형에 쓸 수 있는 ACTCI 분류 후보, 후보별 스펙·자료형·단위,
ACTCI 적용 설정 유무를 조회해 정리했다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: 관계 규칙 조사

**Files:**
- Modify: `docs/data-analysis/exploration-queries/maximo/ci-component-classifications.sql`
- Modify: `docs/data-analysis/knowledge/maximo/ci-component-classifications.md`

**Interfaces:**
- Consumes: Task 3이 정한 유형별 분류 후보의 `CLASSSTRUCTUREID`.
- Produces: Computer 분류와 각 유형 분류 사이에 쓸 수 있는 관계 코드. Task 5의 관계 추천안 근거다.

- [x] **Step 1: 관계 규칙 블록 추가**

Computer 두 분류의 CLASSSTRUCTUREID는 `SYS.COMPUTERSYSTEM`·`SYS.VIRTUALCOMPUTERSYSTEM`으로 조회한다.
후보 분류 목록은 Task 3 결과로 `IN` 목록을 채운다.

```sql
-- name: relation-rules-for-pairs
WITH src AS (
    SELECT s.CLASSSTRUCTUREID, s.CLASSIFICATIONID FROM MAXIMO.CLASSSTRUCTURE s
    WHERE s.CLASSIFICATIONID IN ('SYS.COMPUTERSYSTEM', 'SYS.VIRTUALCOMPUTERSYSTEM')
), tgt AS (
    SELECT s.CLASSSTRUCTUREID, s.CLASSIFICATIONID FROM MAXIMO.CLASSSTRUCTURE s
    WHERE s.CLASSIFICATIONID IN ('<후보1>', '<후보2>', '<후보3>', '<후보4>')
)
SELECT src.CLASSIFICATIONID AS SOURCE_CLASS, tgt.CLASSIFICATIONID AS TARGET_CLASS,
    r.RELATIONNUM, r.CONTAINMENT, r.REVRELATIONSHIP, r.USEWITH
FROM MAXIMO.RELATIONRULES r
JOIN src ON src.CLASSSTRUCTUREID = r.SOURCECLASS
JOIN tgt ON tgt.CLASSSTRUCTUREID = r.TARGETCLASS
ORDER BY 1, 2, 3;

-- name: relation-rules-reverse
WITH src AS (
    SELECT s.CLASSSTRUCTUREID, s.CLASSIFICATIONID FROM MAXIMO.CLASSSTRUCTURE s
    WHERE s.CLASSIFICATIONID IN ('<후보1>', '<후보2>', '<후보3>', '<후보4>')
), tgt AS (
    SELECT s.CLASSSTRUCTUREID, s.CLASSIFICATIONID FROM MAXIMO.CLASSSTRUCTURE s
    WHERE s.CLASSIFICATIONID IN ('SYS.COMPUTERSYSTEM', 'SYS.VIRTUALCOMPUTERSYSTEM')
)
SELECT src.CLASSIFICATIONID AS SOURCE_CLASS, tgt.CLASSIFICATIONID AS TARGET_CLASS,
    r.RELATIONNUM, r.CONTAINMENT, r.REVRELATIONSHIP, r.USEWITH
FROM MAXIMO.RELATIONRULES r
JOIN src ON src.CLASSSTRUCTUREID = r.SOURCECLASS
JOIN tgt ON tgt.CLASSSTRUCTUREID = r.TARGETCLASS
ORDER BY 1, 2, 3;

-- name: relation-definitions
SELECT r.RELATIONNUM, r.DESCRIPTION, r.USEWITH, r.UNIDIRECTIONAL, r.CLASSSTRUCTUREID
FROM MAXIMO.RELATION r
ORDER BY r.USEWITH, r.RELATIONNUM;
```

- [x] **Step 2: 실행과 확인**

Task 3 Step 2와 같은 명령이다. `relation-rules-for-pairs.tsv`와 `relation-rules-reverse.tsv`의 행 수를 본다.

```bash
wc -l local/db-access-kit/work/ci-component-classifications/relation-rules-*.tsv
```

- [x] **Step 3: 관측 문서에 관계 절 추가**

조회 결과가 0건이면 "사용 가능한 규칙 없음"을 관측 사실로 쓴다. 관계 코드를 추측해 적지 않는다.
`RELATION.USEWITH`에 ACTCI가 0건이라는 기존 관측([ci-model.md](../../data-analysis/knowledge/maximo/ci-model.md))과 대조해 쓴다.

- [x] **Step 4: 커밋**

```bash
git add docs/data-analysis/exploration-queries/maximo/ci-component-classifications.sql \
        docs/data-analysis/knowledge/maximo/ci-component-classifications.md
git commit -m "$(cat <<'EOF'
docs: Computer와 네 유형 분류쌍의 관계 규칙 조사

확정한 분류쌍으로 RELATIONRULES를 양방향 조회해 사용 가능한
관계 코드 유무를 확인했다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: OS 수집 구성안

**Files:**
- Create: `docs/data-analysis/design/ci/os.md`

**Interfaces:**
- Consumes: Task 2의 원천 관측, Task 3의 분류 후보, Task 4의 관계 규칙.
- Produces: 나머지 세 유형 설계 문서의 절 구성과 표 형식. Task 6이 이 형식을 복제한다.

- [x] **Step 1: 문서 작성**

[design/ci/computer.md](../../data-analysis/design/ci/computer.md)의 절 구성을 따르되 짧게 쓴다.

절 구성:
1. 상태·범위 (상단에 상태 한 줄, 관측 근거 링크)
2. 관리 단위 — 독립 CI. 근거는 스펙 3절
3. 분류 선택안 — 후보 대조표와 추천 1개, 이유
4. 스펙 대조표 — `CI.*` 기준 속성 ↔ 원천 대응. 열은 `ASSETATTRID | 자료형 | 원천 | 대응 상태`
5. 식별자 — `D42:DEVICEOS:<deviceos_pk>`
6. 관계 추천안 — Task 4 결과 기반. 규칙이 없으면 "사용 가능한 규칙 없음"과 대안
7. 수집 대상 범위 — Computer 연결분/전체 대조와 추천
8. 미결 — ISSUE-8·11 연결

추천과 확정을 구분해 표기한다. 분류를 가정한 대응안을 확정처럼 쓰지 않는다.

- [x] **Step 2: 판단 규칙 적용 확인**

분류 후보가 1개가 아니면 그 사실과 이유를 3절에 쓰고, Task 7에서 OS 매핑 문서를 작성하지 않는다.

- [x] **Step 3: 커밋**

```bash
git add docs/data-analysis/design/ci/os.md
git commit -m "$(cat <<'EOF'
docs: OS CI 수집 구성안 작성

관리 단위·분류 선택안·스펙 대조표·식별자·관계 추천안·수집 대상 범위를
정리했다. 추천과 확정을 구분해 표기한다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Disk·Filesystem·IP 수집 구성안

**Files:**
- Create: `docs/data-analysis/design/ci/disk.md`
- Create: `docs/data-analysis/design/ci/filesystem.md`
- Create: `docs/data-analysis/design/ci/ip.md`

**Interfaces:**
- Consumes: Task 5가 확립한 절 구성과 표 형식.
- Produces: 세 유형의 분류 선택안과 관계 추천안.

- [x] **Step 1: 세 문서 작성**

Task 5의 8개 절을 그대로 쓴다. 유형별로 다음을 반드시 다룬다.

- Disk: `pcount` 해석(수량 대 개체 수), 식별자 `D42:PART:<part_pk>`, Part 기반 단위(`hdsize`, `hdsize_unit`)
- Filesystem: `device_fks` 배열과 `DISTINCT ON`(ISSUE-9), 식별자 `D42:MOUNTPOINT:<mountpoint_pk>`, 용량 단위
- IP: `device_fks` 배열, `netport_fk` 유무에 따른 처리, 식별자 `D42:IPADDRESS:<ipaddress_pk>`, `inet` 타입은 `HOST()` 사용, Subnet 보강

- [x] **Step 2: 형식 일치 확인**

네 문서의 절 제목과 표 열이 같은지 본다.

```bash
grep -h '^## ' docs/data-analysis/design/ci/os.md docs/data-analysis/design/ci/disk.md \
  docs/data-analysis/design/ci/filesystem.md docs/data-analysis/design/ci/ip.md | sort | uniq -c
```

각 절 제목이 4번씩 나와야 한다.

- [x] **Step 3: 커밋**

```bash
git add docs/data-analysis/design/ci/disk.md docs/data-analysis/design/ci/filesystem.md \
        docs/data-analysis/design/ci/ip.md
git commit -m "$(cat <<'EOF'
docs: Disk·Filesystem·IP CI 수집 구성안 작성

OS 문서와 같은 절 구성으로 세 유형의 분류 선택안·스펙 대조표·
식별자·관계 추천안·수집 대상 범위를 정리했다.
배열 원천 두 유형의 중복 처리 근거를 함께 적었다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: 유형별 확정 매핑 문서

**Files:**
- Create: `docs/data-analysis/data-mapping/ci/types/os.md`
- Create: `docs/data-analysis/data-mapping/ci/types/disk.md`
- Create: `docs/data-analysis/data-mapping/ci/types/filesystem.md`
- Create: `docs/data-analysis/data-mapping/ci/types/ip.md`

**Interfaces:**
- Consumes: Task 5·6의 확정된 분류와 스펙 선택.
- Produces: 구현 단계가 읽을 매핑 정본. 조회 SQL, 본체 표, 속성 표.

- [x] **Step 1: 작성 대상 판정**

분류 후보가 정확히 1개인 유형만 쓴다. 나머지는 쓰지 않고 그 이유를 Task 8에서 진행표에 남긴다.

- [x] **Step 2: 문서 작성**

[types/device.md](../../data-analysis/data-mapping/ci/types/device.md) 형식을 따른다. README의 CI 매핑 예외 절이 정본이다.

- 상단: Target, 관측 스탬프, 구현 상태, 실행 준비 링크
- 1절 대상과 식별자
- 2절 원천과 조회 조건 — **실제 실행 가능한 SQL 전문**을 본문에 둔다. 쿼리 파일 링크로 대신하지 않는다
- 3절 본체 매핑 — 5열 `Target 컬럼 | 한글명 | 구분 | Source | 변환·조건`
- 4절 속성 매핑 — 6열 `ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건`. 표 앞에 분류를 명시
- 5절 미대응·미결

공통 컬럼 정의를 복사하지 않고 `actci.md`·`actcispec.md`를 참조한다.
관계는 확정 표에 넣지 않고 `design/ci/<유형>.md` 링크만 둔다.

- [x] **Step 3: 조회 SQL 실행 확인**

2절에 쓴 SQL을 두 서버에서 실제로 돌려 통과하는지 본다. 임시 파일은 스크래치패드에 둔다.
통과하지 않는 SQL을 매핑 문서에 남기지 않는다.

- [x] **Step 4: 커밋**

작성한 유형만 add 한다.

```bash
git add docs/data-analysis/data-mapping/ci/types/
git commit -m "$(cat <<'EOF'
docs: OS·Disk·Filesystem·IP 매핑 문서 작성

유형별 대상·식별자·조회 SQL·본체·속성 매핑을 작성했다.
조회 SQL은 두 D42 서버에서 실행해 확인했다.
관계는 확정 표에 넣지 않고 수집 구성안을 참조한다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: 색인·미결 갱신

**Files:**
- Modify: `docs/data-analysis/data-mapping/ci/README.md`
- Modify: `docs/data-analysis/exploration-queries/README.md`
- Modify: `docs/data-analysis/open-issues.md`
- Modify: `docs/data-analysis/README.md`

**Interfaces:**
- Consumes: Task 1~7의 산출물 목록과 보류 사유.

- [x] **Step 1: 진행표 갱신**

`data-mapping/ci/README.md`의 「유형별 진행」 표에 OS·Disk·Filesystem·IP 네 행을 추가한다.
매핑 문서를 쓰지 않은 유형은 상태 칸에 보류 사유를 적는다.
「읽는 순서」에 새 knowledge 문서 두 건을 넣는다.

- [x] **Step 2: 쿼리 목록 갱신**

`exploration-queries/README.md`의 목록 표에 두 행을 추가한다.

```markdown
| `device42/ci-component-source.sql` | OS·Disk·Filesystem·IP 원천 뷰 형태, PK 유일성, Computer 연결분, 값 보유율 |
| `maximo/ci-component-classifications.sql` | 네 유형의 ACTCI 분류 후보·스펙·적용 설정과 Computer 분류쌍 관계 규칙 |
```

- [x] **Step 3: 미결 갱신**

`open-issues.md` ISSUE-8의 「남은 결정」에서 해소된 항목을 정리한다. 네 유형을 독립 CI로 관리하기로 한 결정과 근거 링크를 넣는다.
ISSUE-11에 관계 규칙 부재, 미등록 속성 등 이번 조사에서 새로 드러난 항목을 추가한다.

- [x] **Step 4: 데이터 분석 README 갱신**

`docs/data-analysis/README.md`의 읽는 순서에 네 유형 설계 문서 링크를 넣는다. Computer 한 줄 옆에 둔다.

- [x] **Step 5: 링크 확인**

문서에서 참조한 상대 경로가 실재하는지 본다.

```bash
grep -rhoE '\]\(\.\.?/[^)]+\)' docs/data-analysis/design/ci/ docs/data-analysis/data-mapping/ci/types/ \
  | tr -d ']()' | sort -u
```

출력된 경로를 각 문서 기준으로 확인하고 깨진 링크를 고친다.

- [x] **Step 6: 커밋**

```bash
git add docs/data-analysis/README.md docs/data-analysis/open-issues.md \
        docs/data-analysis/data-mapping/ci/README.md docs/data-analysis/exploration-queries/README.md
git commit -m "$(cat <<'EOF'
docs: 네 유형 CI 진행 상태와 미결 갱신

유형별 진행표에 OS·Disk·Filesystem·IP를 추가하고 보류 사유를 적었다.
조사 쿼리 목록과 읽는 순서를 갱신했으며, ISSUE-8의 관리 단위 결정과
ISSUE-11의 새 미결 항목을 반영했다.

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>
EOF
)"
```

---

## 완료 조건

- 네 유형의 원천·분류 관측 문서가 있고, 모든 수치가 실제 조회 결과다.
- 네 유형의 수집 구성안이 있고 절 구성이 일치한다.
- 분류 후보가 1개인 유형의 매핑 문서가 있고, 그 조회 SQL이 두 서버에서 실행된다.
- 매핑 문서를 쓰지 않은 유형은 진행표에 보류 사유가 있다.
- `src/` 아래 변경이 없다.
- `git status`가 깨끗하다.


---

## 실행 기록 — 2026-09-15

전 8개 작업을 완료했다. 계획과 달랐던 점을 남긴다.

**실행기 제약이 계획과 달랐다.**

- Maximo 실행기는 `WITH`를 거부한다(`SELECT`/`VALUES`만 허용). Task 4의 계획 SQL은 전부 CTE였고 조인으로 다시 썼다. Device42는 반대로 `WITH`를 허용하고 `FROM` 절 서브쿼리를 거부한다. 차이를 `exploration-queries/README.md`에 정리했다.
- Device42 실행기는 첫 500에서 배치 전체를 중단한다. 미확인 뷰를 등록 파일에 함께 두면 뒤 블록이 실행되지 않는다. 뷰 실재는 한 블록짜리 임시 파일로 개별 확인했다.
- Device42 출력은 `.tsv`가 아니라 `|` 구분 `.txt`다. 계획의 `wc -l *.tsv` 확인 방법을 바꿨다.

**원천 사실이 계획 가정과 달랐다.**

- `view_part_v1`에 `type_name`이 없다. 디스크 필터에 `view_partmodel_v1` 조인이 필요하다.
- 문자열 컬럼에 NULL 대신 빈 문자열이 많다. 첫 집계를 `COUNT()`로 해서 보유율을 과대 계상했고, `COUNT(NULLIF(TRIM(x),''))`로 다시 세어 정정 커밋을 남겼다. 디스크 `firmware`는 전건 빈 문자열이었다.

**판단 규칙 적용 결과.**

- 네 유형 모두 ACTCI 적용 범용 분류가 정확히 하나씩 있어 매핑 문서까지 작성했다. IP는 분류가 명확하나 관계 경로가 미결이라 본체·속성까지만 썼다.
- 관계 규칙은 예상과 달리 대부분 존재했다. Disk·Filesystem은 `RELATION.CONTAINS`, OS는 `INSTALLEDON`·`RUNSON`이 있고 IP만 0건이다.
- 계획에 없던 작업으로 Disk·Filesystem 설계 문서에 관계 절을 추가했다(초안에서 누락).
