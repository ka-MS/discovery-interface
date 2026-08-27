# DPANETPRINTER

배치된 자산 네트워크 프린터

> Target: MAXIMO.DPANETPRINTER · ASSETCLASS: NETPRINTER · 구현: DpaNetPrinterIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : 1 DPANETPRINTER (PK 가 `NODEID` 단독. 관측 4노드/4행)
- 선행: DEPLOYEDASSET

기본키가 `NODEID` 다. 대리키가 없어 노드당 1행만 존재할 수 있다.
`DPANETPRINTERSEQ` 가 존재하지만 이 테이블은 쓰지 않고 부모에서 받은 `NODEID`
를 그대로 쓴다. MERGE 매칭 키도 `NODEID` 하나로 끝나므로 ISSUE-5 대상이
아니다. 근거는 `../knowledge/maximo/deployedasset-model.md` 참조.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_device_v2` | MAXIMO.DPANETPRINTER | – | 1:1 (장비 1건 = 행 1건) |
| `view_netport_v1` | (보강) | `view_netport_v1.device_fk = device_pk` | N:1 |
| `view_ipaddress_v1` | (보강) | `view_ipaddress_v1.device_fk = device_pk` | N:1 |
| `view_part_v1` | (용지함 수) | `view_part_v1.device_fk = device_pk`, `view_partmodel_v1.type_name = 'printer_input'` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_device_v2.device_pk AND IMPORTSOURCE = 'Device42'` → `NODEID` | 1:1 |

프린터 본체 정보는 `view_device_v2` 에 있다. `view_part_v1` 에 달린 27건은
토너·드럼·롤러 같은 소모품(`printer_marker`)과 급지·배지 트레이
(`printer_input`, `printer_output`)라 프린터 사양이 아니다. 이 중
`printer_input` 건수만 용지함 수로 쓴다.

관측 대상은 서버별로 1장비/1행이다.

서버 간 차이는 아래와 같다. 적재 대상 값은 모두 같다.

| 항목 | 192.168.1.35 | 192.168.2.68 |
| --- | --- | --- |
| `device_pk` | 285 | 10 |
| `name` | `192.168.1.3` | `SEC842519C49C88` |
| 포트명 | `Loopback Interface` 외 무명 1건 | `Embedded Ethernet Controller...` 외 `Loopback Interface` |
| MAC / IP / 용지함 | `842519C49C88` / `192.168.1.3` / 3 | 동일 |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| NETPRINTER만 | `(d.network_device = false OR d.network_device IS NULL) AND d.physicalsubtype = 'Network Printer'` | ASSETCLASS 판정에서 `network_device` 가 `physicalsubtype` 보다 우선한다 |
| MAC 보유 포트만 | `n.hwaddress <> ''` | 관측 2포트 중 `Loopback Interface` 는 MAC 이 비어 있다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| COLORDEPTHBIT | 색상 수(비트) | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 4/4 전건 `0` |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CURRENTRAM | 현재 RAM 크기 | DECIMAL(10,2) | Y | 직접 | `view_device_v2.ram` | 관측 1/1, 값 `2.048`. 소수 2자리 |
| HORIZONTALDPI | 가로 DPI | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 전건 `0` |
| MAXLENGTH | 최대 용지 길이 | DECIMAL(10,2) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 전건 `0.00` |
| MAXRAM | 최대 RAM | DECIMAL(10,2) | Y | 원천없음 | – | 대응 원천이 없다. `view_device_v2.ram` 은 현재값이다 |
| MAXWIDTH | 최대 용지 너비 | DECIMAL(10,2) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 전건 `0.00` |
| NETMACADDR | 네트워크 MAC 주소 | ALN(17) | Y | 변환 | `view_netport_v1.hwaddress` | `UPPER(hwaddress)`. 구분자 없는 12자리이며 DPANETADAPTER·기존 수집분과 형식이 같다 |
| NETWORKADDRESS | 네트워크 주소 | ALN(39) | Y | 변환 | `view_ipaddress_v1.ip_address` | `HOST(ip_address)`. inet 타입이라 그냥 캐스팅하면 `/32` 접미가 붙는다 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. 이 테이블의 기본키이기도 하다 |
| NUMBEROFTRAYS | 용지함 수 | INTEGER(12) | Y | 변환 | `view_part_v1`, `view_partmodel_v1.type_name` | `type_name = 'printer_input'` 인 파트 건수. 관측 3(Tray 1, Tray 2, MP Tray) |
| RAMUNIT | RAM 단위 | ALN(16) | Y | 직접 | `view_device_v2.ram_size_type` | 관측 `GB`. 기존 수집분의 `KB` 는 다른 도구의 관례다 |
| SIZEUNIT | 크기 단위 | ALN(16) | Y | 원천없음 | – | `MAXLENGTH`·`MAXWIDTH` 원천이 없어 단위만 단독 적재하지 않는다 |
| VCURRENTRAMSIZE | 현재 RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VERTICALDPI | 세로 DPI | INTEGER(12) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 전건 `0` |
| VMAXLENGTH | 최대 길이 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VMAXRAMSIZE | 최대 RAM 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VMAXWIDTH | 최대 너비 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

`view_part_v1` 의 `printer_marker` 27건 중 21건은 토너·드럼·롤러 잔량이다.
`details` 에 `marker_capacity` 와 `marker_current_level` 이 있으나 대응 타겟
컬럼이 없어 적재하지 않는다.

## 5. 조회 쿼리

```sql
SELECT
    d.device_pk,
    d.ram,
    d.ram_size_type,
    (SELECT UPPER(n.hwaddress)
     FROM view_netport_v1 n
     WHERE n.device_fk = d.device_pk AND n.hwaddress <> ''
     LIMIT 1) AS hwaddress,
    (SELECT HOST(i.ip_address)
     FROM view_ipaddress_v1 i
     WHERE i.device_fk = d.device_pk
     LIMIT 1) AS ip_address,
    (SELECT COUNT(*)
     FROM view_part_v1 p
     JOIN view_partmodel_v1 pm ON pm.partmodel_pk = p.partmodel_fk
     WHERE p.device_fk = d.device_pk AND pm.type_name = 'printer_input') AS tray_cnt
FROM view_device_v2 d
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND d.physicalsubtype = 'Network Printer'
ORDER BY d.device_pk
```

## 6. 미결

- MAC 과 IP 가 각각 2건 이상인 프린터에서 어느 값을 쓸지 규칙이 없다. 기본키가
  `NODEID` 라 노드당 1행만 가능한데 원천은 포트와 IP 를 여럿 가질 수 있다.
  두 서버를 다 봐도 프린터는 같은 1장비뿐이고 MAC·IP 가 각 1건이라 현재는
  드러나지 않는다.
