# DPAOS

배치된 자산 컴퓨터 운영 체제

> Target: MAXIMO.DPAOS · ASSETCLASS: COMPUTER · 구현: DpaOsIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPAOS (기존 수집분 61노드/63행)
- 선행: DEPLOYEDASSET

`DPAOS_NDX1` 이 `(OSID, NODEID)` 유일이고 `NODEID` 단독 인덱스는 비유일이라
노드당 여러 행이 허용된다. 다만 Device42 원천은 장비당 최대 1건이다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_deviceos_v1` | MAXIMO.DPAOS | – | 1:1 (deviceos 1건 = 행 1건) |
| `view_os_v1` | (보강) | `view_deviceos_v1.os_fk = os_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_os_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_deviceos_v1.device_fk = device_pk` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_deviceos_v1.device_fk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

COMPUTER 대상 67장비 중 59장비/59행이다. 나머지 8장비는 OS 원천이 없다.
장비당 deviceos 가 2건 이상인 사례는 없다.

`view_deviceos_v1.os_name` 은 59/59 전건 `view_os_v1.name` 과 같은 값이다.
`NAME` 은 조인 없이 `os_name` 에서 얻고, 조인은 제조사에만 쓴다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')` | 다른 ASSETCLASS의 자식을 만들지 않는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| BUILD | 빌드 | ALN(64) | Y | 직접 | `view_deviceos_v1.os_version_no` | 관측 6/59, 최대 22자. 커널·빌드 식별자 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CHARACTERSET1 | 문자 세트 | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/63 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 원천없음 | – | `view_deviceos_v1` 에 설명 컬럼이 없다 |
| LANGUAGE | 언어 | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분의 `UNKNOWN` 은 다른 도구의 관례다 |
| LICENSEDORG | 라이센스가 부여된 조직 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| LICENSEDUSER | 라이센스가 부여된 사용자 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_os_v1.vendor_fk` 조인. 관측 5/59, 최대 9자. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| NAME | 운영 체제 | ALN(256) | N | 직접 | `view_deviceos_v1.os_name` | 관측 59/59, 최대 151자. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| OSID | 운영 체제 ID | BIGINT(19) | N | 채번 | – | 대리키. 원천 `deviceos_pk` 는 재수집 시 바뀌므로 쓰지 않는다 |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다. `os_license_key` 계열은 관측 0/59이고 일련번호도 아니다 |
| SERVICEPACK | 서비스 팩 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| VERSION | 버전 | ALN(128) | Y | 직접 | `view_deviceos_v1.os_version` | 관측 9/59, 최대 7자 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

`view_deviceos_v1.os_arch_name`(관측 6/59, `64-bit`)은 대응 타겟 컬럼이 없어
적재하지 않는다.

## 5. 조회 쿼리

```sql
SELECT
    o.device_fk,
    o.os_name,
    o.os_version,
    o.os_version_no,
    v.name AS vendor_name
FROM view_deviceos_v1 o
JOIN view_device_v2 d ON d.device_pk = o.device_fk
LEFT JOIN view_os_v1 s ON s.os_pk = o.os_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY o.device_fk
```

## 6. 미결

- ISSUE-5 — `OSID` 채번 범위와 재실행 시 키 유지 규칙이 미정이다.
