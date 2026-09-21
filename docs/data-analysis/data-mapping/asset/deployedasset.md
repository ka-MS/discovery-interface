# DEPLOYEDASSET

배치된 자산

> Target: MAXIMO.DEPLOYEDASSET · ASSETCLASS: COMPUTER, NETDEVICE, NETPRINTER · 구현: [DeployedAssetImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/asset/device/DeployedAssetImport.java) · [DeviceQuery](../../../../src/main/java/com/itmsg/device42/source/device42/asset/device/DeviceQuery.java) · [DeployedAssetMapper](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/asset/device/DeployedAssetMapper.java) · [DeployedAssetWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/asset/DeployedAssetWriter.java)

> 관측 2026-08-27 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35 · Maximo BLUDB
> 원천 건수는 서버별로 병기한다. 표기는 `.68 / .35` 순이다.

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 관계

- 계층의 루트. 부모 없음.
- `NODEID` 는 `view_device_v2.device_pk` 를 그대로 사용한다.
- 적재 대상 필터와 키 전략은 3번에 기술한다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2` | MAXIMO.DEPLOYEDASSET | – | 1:1 |
| `view_hardware_v2` | (보강) | `view_device_v2.hardware_fk = hardware_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_hardware_v2.vendor_fk = vendor_pk` | N:1 |

MERGE 키는 `NODEID` 다. `NODEID = device_pk` 이므로 재실행해도 멱등하다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 타입 한정 | `d.type IN ('virtual','physical')` | `cluster`, `unknown` 은 적재 대상이 아니다 |
| 컨테이너 제외 | `d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15` | Docker Container 는 자산으로 관리하지 않는다 |
| PDU 제외 | `d.physicalsubtype IS NULL OR d.physicalsubtype <> 'PDU'` | 대응 ASSETCLASS 와 DPA 테이블이 없다 |

근거: `MaximoSourcePolicy.ASSET_DEVICE`가 `DeviceQuery`에 전달하는 선택 조건.

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
| IMPORTSOURCE | 가져오기 소스 | ALN(128) | Y | 상수 | – | `'Device42'` |
| MAKEMODEL | 제조/모델 | ALN(128) | Y | 직접 | `view_hardware_v2.name` | `view_device_v2.hardware_fk` 조인. 가상 장비는 양쪽 서버 모두 전건 NULL (.68 VMWare 0/18, EC2 0/8 · .35 VMWare 0/55) |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_hardware_v2.vendor_fk` 조인. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N | 직접 | `view_device_v2.device_pk` | Maximo ID로 그대로 사용하며 MERGE 키로 삼는다 |
| NODEID2 | 노드 ID 2 | BIGINT(19) | Y | 원천없음 | – | 보조 키 컬럼. 현행 적재는 사용하지 않는다 |
| NODENAME | 노드 | ALN(128) | N | 직접 | `view_device_v2`.name | 비어 있으면 `UNKNOWN` |
| ORGID | 조직 | UPPER(8) | Y | 원천없음 | – | Maximo 조직 체계 값. 수집 원천이 아니다 |
| PLUSPCUSTOMER | 고객 | UPPER(12) | Y | 원천없음 | – |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 직접 | `view_device_v2.serial_no` | .68 VMWare 17/18, EC2 0/8 · .35 전체 12/69 |
| SITEID | 사이트 | UPPER(8) | Y | 원천없음 | – | Maximo 조직 체계 값. 수집 원천이 아니다 |
| SOURCEID | 소스 | ALN(128) | Y | 변환 | `view_device_v2`.device_pk | 문자열로 변환 |
| SOURCEID2 | Source2 | ALN(128) | Y | 원천없음 | – | 보조 키 컬럼. 현행 적재는 사용하지 않는다 |
| SUPPORTSSNMP | SNMP 지원 | YORN(1) | N | 상수 | – | `0` |
| SYSTEMROLE | 역할 | ALN(32) | Y | 변환 | ASSETCLASS와 동일 판정 | 현재 Mapper는 ASSETCLASS 값(COMPUTER·NETDEVICE·NETPRINTER)을 그대로 넣는다. Server 등의 별도 역할 분류는 구현하지 않음 |
| TLOAMHASH | 파티션 ID | UPPER(192) | Y | 원천없음 | – |  |
| TLOAMHWTYPE | 하드웨어 유형 | ALN(32) | Y | 원천없음 | – | 기존 수집분도 전건 NULL |
| TLOAMISPROMOTED | 승격 여부 | UPPER(8) | Y | 원천없음 | – | Maximo 내부 상태값 |
| TLOAMNRSGUID | 통합 ID | ALN(192) | Y | 원천없음 | – |  |
| TLOAMNRSHOSTSYSTEM | NRS 호스트 시스템 | ALN(128) | Y | 미결 | `view_device_v2.virtual_host_device_fk` | 가상 호스트의 이름. 자기참조 조인 필요. 적재 대상 중 호스트 관계를 가진 장비가 .68 은 0대, .35 는 55대다 |
| TLOAMNRSMANAGEDSYSTEMNAME | NRS 관리 대상 시스템 이름 | ALN(128) | Y | 원천없음 | – |  |
| TLOAMNRSMANUFACTURER | NRS 제조업체 | ALN(128) | Y | 직접 | `view_vendor_v1.name` | MANUFACTURER 와 동일 원천. 없으면 NULL(UNKNOWN 대체 없음) |
| TLOAMNRSMODEL | NRS 제조사/모델 | ALN(128) | Y | 미결 | `view_hardware_v2.name` | MAKEMODEL 과 동일 원천. 중복 적재 여부 미정 |
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
    d.device_pk,
    d.name,
    d.type,
    d.notes,
    d.serial_no,
    d.asset_no,
    d.uuid,
    d.network_device,
    d.physicalsubtype,
    h.name AS hardware_name,
    v.name AS vendor_name,
    d.in_service,
    d.last_discovered
FROM view_device_v2 d
LEFT JOIN view_hardware_v2 h
    ON d.hardware_fk = h.hardware_pk
LEFT JOIN view_vendor_v1 v
    ON h.vendor_fk = v.vendor_pk
WHERE
d.type IN ('virtual', 'physical')
AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('PDU'))
ORDER BY d.device_pk
LIMIT 1000 OFFSET 0
```

이 조회에는 RAM·CPU·BIOS 요약 컬럼이 없다. 해당 필드는 [DPACOMPUTER](dpacomputer.md)의 별도 조회·매핑이 소유한다.

## 6. 미결

- 현재 `SYSTEMROLE`은 ASSETCLASS를 복제한다. 별도 업무 역할 분류는 구현 범위 밖이다.
- `TLOAMNRSHOSTSYSTEM`, `TLOAMNRSMODEL`, `TLOAMNRSNAME`,
  `TLOAMNRSPRIMARYMACADDRESS`, `TLOAMNRSSERIALNUMBER`의 매핑 규칙이 미정이다.

## 실제 저장 SQL

아래는 현재 Writer의 SQL이다. `?`는 USING source 열 순서로 DTO 값을 바인딩한다.
INSERT에 없는 컬럼은 이 ETL이 신규 값을 지정하지 않으며 DB 기본값·제약에 따른다.
UPDATE에 없는 컬럼은 기존 값을 유지한다. 문서의 원천 미대응·미결 표기는 NULL로 덮어쓴다는 뜻이 아니다.

```sql
MERGE INTO MAXIMO.DEPLOYEDASSET AS target
USING (
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
) AS source (
    NODEID,
    SOURCEID,
    IMPORTSOURCE,
    NODENAME,
    DOMAINNAME,
    SERIALNUMBER,
    ASSETTAG,
    MAKEMODEL,
    MANUFACTURER,
    DESCRIPTION,
    HWLASTSCANDATE,
    HWDETECTIONTOOL,
    SUPPORTSSNMP,
    SYSTEMROLE,
    ASSETCLASS,
    CREATEDATE,
    CHANGEDATE,
    TLOAMSTATUS,
    TLOAMNRSMANUFACTURER,
    TLOAMNRSUUID
)
ON target.NODEID = source.NODEID
WHEN MATCHED THEN
    UPDATE SET
        NODENAME = source.NODENAME,
        DOMAINNAME = source.DOMAINNAME,
        SERIALNUMBER = source.SERIALNUMBER,
        ASSETTAG = source.ASSETTAG,
        MAKEMODEL = source.MAKEMODEL,
        MANUFACTURER = source.MANUFACTURER,
        DESCRIPTION = source.DESCRIPTION,
        HWLASTSCANDATE = source.HWLASTSCANDATE,
        HWDETECTIONTOOL = source.HWDETECTIONTOOL,
        SUPPORTSSNMP = source.SUPPORTSSNMP,
        SYSTEMROLE = source.SYSTEMROLE,
        ASSETCLASS = source.ASSETCLASS,
        CHANGEDATE = source.CHANGEDATE,
        TLOAMSTATUS = source.TLOAMSTATUS,
        TLOAMNRSMANUFACTURER = source.TLOAMNRSMANUFACTURER,
        TLOAMNRSUUID = source.TLOAMNRSUUID
WHEN NOT MATCHED THEN
    INSERT (
        NODEID,
        SOURCEID,
        IMPORTSOURCE,
        NODENAME,
        DOMAINNAME,
        SERIALNUMBER,
        ASSETTAG,
        MAKEMODEL,
        MANUFACTURER,
        DESCRIPTION,
        HWLASTSCANDATE,
        HWDETECTIONTOOL,
        SUPPORTSSNMP,
        SYSTEMROLE,
        ASSETCLASS,
        CREATEDATE,
        CHANGEDATE,
        TLOAMSTATUS,
        TLOAMNRSMANUFACTURER,
        TLOAMNRSUUID
    )
    VALUES (
        source.NODEID,
        source.SOURCEID,
        source.IMPORTSOURCE,
        source.NODENAME,
        source.DOMAINNAME,
        source.SERIALNUMBER,
        source.ASSETTAG,
        source.MAKEMODEL,
        source.MANUFACTURER,
        source.DESCRIPTION,
        source.HWLASTSCANDATE,
        source.HWDETECTIONTOOL,
        source.SUPPORTSSNMP,
        source.SYSTEMROLE,
        source.ASSETCLASS,
        source.CREATEDATE,
        source.CHANGEDATE,
        source.TLOAMSTATUS,
        source.TLOAMNRSMANUFACTURER,
        source.TLOAMNRSUUID
    )
```
