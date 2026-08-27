# DPANETADAPTER

배치된 자산 컴퓨터 네트워크 어댑터

> Target: MAXIMO.DPANETADAPTER · ASSETCLASS: COMPUTER · 구현: DpaNetAdapterIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPANETADAPTER (PK 는 `ADAPTERID`. 관측 39노드/61행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_netport_v1` | MAXIMO.DPANETADAPTER | – | 1:1 (포트 1건 = 행 1건) |
| `view_vendor_v1` | (보강) | `view_netport_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_netport_v1.device_fk = device_pk` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_netport_v1.device_fk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

COMPUTER 대상 원천은 관측 66장비/142행이다. MAC 주소는 133/142,
포트명은 76/142, 속도와 프로토콜은 각각 5/142행에 값이 있다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')` | 다른 ASSETCLASS의 자식을 만들지 않는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ADAPTERID | 어댑터 | BIGINT(19) | N | 채번 | – | 대리키. 원천 `netport_pk` 는 재수집 시 바뀌므로 쓰지 않는다 |
| ADAPTERTYPE | 어댑터 유형 | ALN(32) | Y | 상수 | – | `'Network Adapter'`. 기존 수집분도 61/61 전건 동일 |
| ASSETTAG | 자산 태그 | ALN(64) | Y | 원천없음 | – | `view_netport_v1`에 자산 태그가 없다 |
| BANDWIDTH | 대역폭 | DECIMAL(10,2) | Y | 변환 | `view_netport_v1.port_speed` | 첫 공백 앞 숫자를 소수 2자리로 변환. 값이 없거나 숫자가 아니면 NULL |
| BANDWIDTHUNIT | 대역폭 단위 | ALN(16) | Y | 변환 | `view_netport_v1.port_speed` | 첫 공백 뒤 단위. 관측값 `Mbps`; 단위가 없으면 NULL |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CHIPSET | 칩셋 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 직접 | `view_netport_v1.description` | 관측 5/142 |
| FIRMWAREVERSION | 펌웨어 버전 | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| MAKEMODEL | 제조/모델 | ALN(128) | N | 상수 | – | `'UNKNOWN'`. 포트 뷰에 어댑터 모델이 없다. DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `vendor_fk` 조인. 관측 0/142이므로 현재는 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| NETMACADDR1 | MAC 주소 1 | ALN(17) | Y | 변환 | `view_netport_v1.hwaddress` | `UPPER(hwaddress)`. 관측값은 구분자 없는 12자리이며 기존 수집분 형식과 같다 |
| NETMACADDR2 | MAC 주소 2 | ALN(17) | Y | 변환 | `view_netport_v1.hwaddress2` | `UPPER(hwaddress2)`. 관측 0/142 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| PORT | 포트 | ALN(16) | Y | 변환 | `view_netport_v1.port` | `LEFT(port, 16)`. 관측 최대 71자로 타겟 길이에 맞춘다 |
| PROTOCOL | 프로토콜 | ALN(64) | Y | 직접 | `view_netport_v1.global_type` | 관측 5/142, 값 `ethernet` |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다 |
| VBANDWIDTH | 대역폭 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    n.device_fk,
    n.port,
    n.description,
    n.hwaddress,
    n.hwaddress2,
    n.port_speed,
    n.global_type,
    v.name AS vendor_name
FROM view_netport_v1 n
JOIN view_device_v2 d ON d.device_pk = n.device_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = n.vendor_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY n.device_fk, n.netport_pk
```

## 6. 미결

- ISSUE-5 — `ADAPTERID` 채번 범위와 재실행 시 키 유지 규칙이 미정이다.
