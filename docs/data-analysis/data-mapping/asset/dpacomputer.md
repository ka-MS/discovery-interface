# DPACOMPUTER

배치된 자산 컴퓨터

> Target: MAXIMO.DPACOMPUTER · ASSETCLASS: COMPUTER · 구현: DpaComputerIntegrate.java

> 관측 2026-08-27 · Device42 192.168.1.35 · Maximo BLUDB
> 본문의 원천 건수는 이 서버 기준이다. 192.168.2.68 은 파트·소프트웨어·마운트가 더 넓다.

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : 1 DPACOMPUTER (PK 가 `NODEID` 단독. 관측 68노드/68행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2` | MAXIMO.DPACOMPUTER | – | 1:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = device_pk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

MERGE 키는 `NODEID` 단독이다. 노드당 1행이므로 부모와 1:1 이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 네트워크 장비 제외 | `d.network_device = false OR d.network_device IS NULL` | NETDEVICE 는 DPANETDEVICE 로 간다 |
| 프린터 제외 | `d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer'` | NETPRINTER 는 DPANETPRINTER 로 간다 |
| 타입 미상 제외 | `d.type IS NOT NULL AND TRIM(d.type) <> '' AND LOWER(TRIM(d.type)) <> 'unknown'` | |

근거: `DpaComputerIntegrate.java` `DEVICE_FILTER`.

**이 조건은 부모의 조건과 일치하지 않는다.** 부모가 제외하는 Docker Container 를 여기서는 제외하지 않아, 관측 기준 12건이 대상으로 잡히고 전부 교차키 조회에 실패해 경고 로그만 남긴다. ISSUE-4 참조.

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| BIOSDATE | BIOS 날짜 | DATETIME(10) | Y | 변환 | `view_device_v2`.bios_release_date | ISO_LOCAL_DATE 우선, 실패 시 `MM/dd/yyyy`. 둘 다 실패하면 NULL |
| BIOSNAME | BIOS | ALN(64) | Y | 원천없음 | – | `view_device_v2`.bios_vendor_fk 가 있으나 관측 전건 NULL |
| BIOSPNP | PNP | YORN(1) | N | 상수 | – | `0` |
| BIOSVERSION | BIOS 버전 | ALN(32) | Y | 직접 | `view_device_v2`.bios_version |  |
| CAPACITYMODEL1 | 용량 모델 | ALN(128) | Y | 원천없음 | – | 메인프레임 용량 지표. 수집 대상이 아니다 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| LOGONNAME | 로그온 | ALN(64) | Y | 원천없음 | – |  |
| MIPS1 | MIPS 용량 | INTEGER(12) | Y | 원천없음 | – | 메인프레임 용량 지표. 수집 대상이 아니다 |
| MOBOASSETTAG | 자산 태그 | ALN(64) | Y | 원천없음 | – |  |
| MOBOCHIPSET | 칩셋 | ALN(128) | Y | 원천없음 | – |  |
| MOBODESCRIPTION | 마더보드 설명 | ALN(256) | Y | 원천없음 | – |  |
| MOBOMAKEMODEL | 제조/모델 | ALN(128) | Y | 원천없음 | – |  |
| MOBOMANUFACTURER | 제조업체 | ALN(128) | Y | 원천없음 | – |  |
| MOBOSERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – |  |
| MSUS1 | MSU 용량 | INTEGER(12) | Y | 원천없음 | – | 메인프레임 용량 지표. 수집 대상이 아니다 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회. 교차키가 없으면 로그만 남기고 건너뛴다 |
| NUMCORETOTAL | 총 코어 | INTEGER(12) | Y | 변환 | `view_device_v2`.total_cpus, .core_per_cpu | 두 값의 곱. 하나라도 NULL 이면 NULL |
| NUMCPUCONFIG1 | 구성된 프로세서 수 | INTEGER(12) | Y | 원천없음 | – |  |
| NUMCPUTOTAL1 | 총 프로세서 수 | INTEGER(12) | Y | 직접 | `view_device_v2`.total_cpus |  |
| PLANTCODE1 | 제조 공장 | ALN(32) | Y | 원천없음 | – |  |
| RAMDESCRIPTION | RAM 설명 | ALN(256) | Y | 미결 | `view_partmodel_v1.name` | 예: `DRAM 16384 MB DIMM`. 슬롯 여러 개일 때 규칙 미정 |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y | 변환 | `view_device_v2`.ram | 소수 2자리 반올림(HALF_UP) |
| RAMTOTALSLOTS | RAM 총 슬롯 | INTEGER(12) | Y | 미결 | `view_part_v1`(RAM) 건수 | 파트 건수로 유도 가능. 미장착 슬롯은 알 수 없다 |
| RAMTYPE | RAM 유형 | ALN(32) | Y | 미결 | `view_partmodel_v1.ramtype` | RAM 파트 조인으로 얻을 수 있다. 슬롯이 여러 개일 때 대표값 선정 규칙 미정 |
| RAMUNIT | RAM 단위 | ALN(16) | Y | 직접 | `view_device_v2`.ram_size_type |  |
| RAMUNUSEDSLOTS | RAM 미사용 슬롯 | INTEGER(12) | Y | 원천없음 | – | Device42 는 미장착 슬롯을 수집하지 않는다 |
| SMBIOS | SMBIOS | YORN(1) | N | 상수 | – | `0` |
| SUPPORTSWMI | WMI 지원 | YORN(1) | N | 상수 | – | `0` |
| SWDETECTIONTOOL | 소프트웨어 검색 도구 | ALN(256) | Y | 원천없음 | – | DPASOFTWARE 적재 시 함께 정한다 |
| SWLASTSCANDATE | 소프트웨어 최종 스캔 날짜 | DATETIME(10) | Y | 원천없음 | – | 소프트웨어 스캔 시각. `view_softwareinuse_v1.last_updated` 가 있으나 장비 단위 값이 아니다 |
| TLOAMAUTHUSERID | 인증된 사용자 ID | ALN(256) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMCARRIER | 모바일 네트워크 사업자 | ALN(64) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMDEVOWNERSHIP | 모바일 디바이스 소유권 | UPPER(50) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMDEVPHONENUM | 전화번호 | ALN(20) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMDEVPWDCOMPL | 비밀번호 준수 | YORN(1) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMDEVPWDENBLD | 비밀번호 사용 | YORN(1) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMIMEI | IMEI | ALN(64) | Y | 원천없음 | – | 모바일 단말 속성. 수집 대상이 아니다 |
| TLOAMPARENTID | 상위 노드 Id | BIGINT(19) | Y | 미결 | `view_device_v2`.virtual_host_device_fk | 가상 호스트의 NODEID. 기존 수집분은 0/68 미사용 |
| TLOAMPARENTNAME | 상위 | ALN(128) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| TLOAMPLATFORMBASE | 플랫폼 | UPPER(20) | Y | 원천없음 | – | 기존 수집분도 전건 NULL |
| VRAMSIZE | RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    d.device_pk,
    d.bios_version,
    d.bios_release_date,
    d.ram,
    d.ram_size_type,
    d.total_cpus,
    d.core_per_cpu
FROM view_device_v2 d
WHERE (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
  AND d.type IS NOT NULL
  AND TRIM(d.type) <> ''
  AND LOWER(TRIM(d.type)) <> 'unknown'
ORDER BY d.device_pk
```

## 6. 미결

- ISSUE-4 — 이 태스크의 조회 조건이 부모 DEPLOYEDASSET 의 조건과 달라, 부모가 없는 장비를 매 실행마다 조회하고 건너뛴다.
- RAM 상세(`RAMTYPE`, `RAMDESCRIPTION`, `RAMTOTALSLOTS`)는 `view_part_v1`(RAM) 조인으로 얻을 수 있으나 슬롯이 여러 개일 때의 대표값 규칙이 정해지지 않았다.
