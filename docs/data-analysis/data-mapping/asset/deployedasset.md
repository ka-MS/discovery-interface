# DEPLOYEDASSET

배치된 자산

> Target: MAXIMO.DEPLOYEDASSET · ASSETCLASS: COMPUTER, NETDEVICE, NETPRINTER · 구현: DeployedAssetIntegrate.java

> 관측 2026-08-27 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35 · Maximo BLUDB
> 원천 건수는 서버별로 병기한다. 표기는 `.68 / .35` 순이다.

## 1. 관계

- 계층의 루트. 부모 없음.
- `NODEID` 는 `MAXIMO.DEPLOYEDASSETSEQ` 로 발번한다.
- 적재 대상 필터와 키 전략은 3번에 기술한다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2` | MAXIMO.DEPLOYEDASSET | – | 1:1 |
| `view_hardware_v1` | (보강) | `view_device_v2.hardware_fk = hardware_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_hardware_v1.vendor_fk = vendor_pk` | N:1 |

MERGE 키는 `(SOURCEID, IMPORTSOURCE)` 다. 재실행해도 멱등하다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 타입 한정 | `d.type IN ('virtual','physical')` | `cluster`, `unknown` 은 적재 대상이 아니다 |
| 컨테이너 제외 | `d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15` | Docker Container 는 자산으로 관리하지 않는다 |
| PDU 제외 | `d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU'` | 대응 ASSETCLASS 와 DPA 테이블이 없다 |

근거: `DeployedAssetIntegrate.java` `DEVICE_FILTER`.

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETCLASS | 자산 클래스 | ALN(32) | N | 변환 | `view_device_v2`.network_device, .physicalsubtype | network_device 면 NETDEVICE, physicalsubtype=Network Printer 면 NETPRINTER, 그 외 COMPUTER. PDU 는 조회 대상에서 제외 |
| ASSETTAG | 자산 태그 | ALN(64) | Y | 직접 | `view_device_v2`.asset_no |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각. MERGE MATCHED 시 갱신하지 않는다 |
| DESCRIPTION | 설명 | ALN(256) | Y | 직접 | `view_device_v2`.notes |  |
| DOMAINNAME | 도메인 | ALN(128) | N | 상수 | – | `'UNKNOWN'`. 원천에 도메인 정보가 없다 |
| GUID | 발견 ID | ALN(192) | Y | 원천없음 | – |  |
| HWDETECTIONTOOL | 하드웨어 검색 도구 | ALN(256) | Y | 상수 | – | `'Device42'` |
| HWLASTSCANDATE | 하드웨어 최종 스캔 날짜 | DATETIME(10) | Y | 직접 | `view_device_v2`.last_discovered |  |
| IMPORTSOURCE | 가져오기 소스 | ALN(128) | Y | 상수 | – | `'Device42'`. MERGE 키의 일부 |
| MAKEMODEL | 제조/모델 | ALN(128) | Y | 직접 | `view_hardware_v1.name` | `view_device_v2.hardware_fk` 조인. 가상 장비는 양쪽 서버 모두 전건 NULL (.68 VMWare 0/18, EC2 0/8 · .35 VMWare 0/55) |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_hardware_v1.vendor_fk` 조인. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | `MAXIMO.DEPLOYEDASSETSEQ`. INSERT 시에만 발번, MERGE MATCHED 시 유지 |
| NODEID2 | 노드 ID 2 | BIGINT(19) | Y | 원천없음 | – | 보조 키 컬럼. 현행 적재는 사용하지 않는다 |
| NODENAME | 노드 | ALN(128) | N | 직접 | `view_device_v2`.name | 비어 있으면 `UNKNOWN` |
| ORGID | 조직 | UPPER(8) | Y | 원천없음 | – | Maximo 조직 체계 값. 수집 원천이 아니다 |
| PLUSPCUSTOMER | 고객 | UPPER(12) | Y | 원천없음 | – |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 직접 | `view_device_v2.serial_no` | .68 VMWare 17/18, EC2 0/8 · .35 전체 13/70 |
| SITEID | 사이트 | UPPER(8) | Y | 원천없음 | – | Maximo 조직 체계 값. 수집 원천이 아니다 |
| SOURCEID | 소스 | ALN(128) | Y | 변환 | `view_device_v2`.device_pk | 문자열로 변환. MERGE 키의 일부 |
| SOURCEID2 | Source2 | ALN(128) | Y | 원천없음 | – | 보조 키 컬럼. 현행 적재는 사용하지 않는다 |
| SUPPORTSSNMP | SNMP 지원 | YORN(1) | N | 상수 | – | `0` |
| SYSTEMROLE | 역할 | ALN(32) | Y | 미결 | `view_device_v2`.type, .virtualsubtype, .physicalsubtype | 기존 수집분은 13종(Server, Network PC, Unix Box 등)을 쓴다. D42 타입 체계를 여기에 대응시킬 자리이나 값 대응 규칙 미정 |
| TLOAMHASH | 파티션 ID | UPPER(192) | Y | 원천없음 | – |  |
| TLOAMHWTYPE | 하드웨어 유형 | ALN(32) | Y | 원천없음 | – | 기존 수집분도 전건 NULL |
| TLOAMISPROMOTED | 승격 여부 | UPPER(8) | Y | 원천없음 | – | Maximo 내부 상태값 |
| TLOAMNRSGUID | 통합 ID | ALN(192) | Y | 원천없음 | – |  |
| TLOAMNRSHOSTSYSTEM | NRS 호스트 시스템 | ALN(128) | Y | 미결 | `view_device_v2.virtual_host_device_fk` | 가상 호스트의 이름. 자기참조 조인 필요. 적재 대상 중 호스트 관계를 가진 장비가 .68 은 0대, .35 는 55대다 |
| TLOAMNRSMANAGEDSYSTEMNAME | NRS 관리 대상 시스템 이름 | ALN(128) | Y | 원천없음 | – |  |
| TLOAMNRSMANUFACTURER | NRS 제조업체 | ALN(128) | Y | 직접 | `view_vendor_v1.name` | MANUFACTURER 와 동일 원천. 없으면 NULL(UNKNOWN 대체 없음) |
| TLOAMNRSMODEL | NRS 제조사/모델 | ALN(128) | Y | 미결 | `view_hardware_v1.name` | MAKEMODEL 과 동일 원천. 중복 적재 여부 미정 |
| TLOAMNRSNAME | NRS 이름 | ALN(128) | Y | 미결 | `view_device_v2`.name | NODENAME 과 동일 원천. 중복 적재 여부 미정 |
| TLOAMNRSPRIMARYMACADDRESS | NRS MAC 주소 | ALN(17) | Y | 미결 | `view_netport_v1.hwaddress` | 대표 포트 선정 규칙 필요. 가상 포트(veth/docker/br-) 제외 시 .68 은 85대 중 64대, .35 는 79대 중 56대가 포트 1개라 모호하지 않다. 나머지는 규칙이 필요하다 |
| TLOAMNRSSERIALNUMBER | NRS 일련 번호 | ALN(128) | Y | 미결 | `view_device_v2`.serial_no | SERIALNUMBER 와 동일 원천. 중복 적재 여부 미정 |
| TLOAMNRSSIGNATURE | NRS 특성 | ALN(128) | Y | 원천없음 | – |  |
| TLOAMNRSSYSTEMBOARDUUID | NRS 시스템 보드 UUID | ALN(64) | Y | 원천없음 | – |  |
| TLOAMNRSUUID | NRS 가상 머신 UUID | ALN(64) | Y | 직접 | `view_device_v2.uuid` | .68 의 EC2 8대는 값이 없다 |
| TLOAMNRSVMID | NRS VMID | ALN(128) | Y | 원천없음 | – |  |
| TLOAMSTATUS | 상태 | UPPER(20) | Y | 변환 | `view_device_v2`.in_service | 참이면 `ACTIVE`, 거짓이면 `INACTIVE` |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    d.device_pk, d.name, d.type, d.notes, d.serial_no, d.asset_no, d.uuid,
    d.network_device, d.physicalsubtype, d.in_service, d.last_discovered,
    h.name AS hardware_name,
    v.name AS vendor_name
FROM view_device_v2 d
LEFT JOIN view_hardware_v1 h ON d.hardware_fk = h.hardware_pk
LEFT JOIN view_vendor_v1 v ON h.vendor_fk = v.vendor_pk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU')
ORDER BY d.device_pk
```

`ram`, `ram_size_type`, `total_cpus`, `core_per_cpu`, `bios_version`, `bios_release_date` 도 함께 조회되지만 이 테이블에는 적재되지 않는다. DPACOMPUTER 가 같은 값을 별도 조회로 다시 가져간다.

## 6. 미결

- 적재 대상 규모가 서버별로 크게 다르다. .68 은 31대(COMPUTER 28 / NETDEVICE 2 / NETPRINTER 1),
  .35 는 70대(COMPUTER 67 / NETDEVICE 2 / NETPRINTER 1)다.
