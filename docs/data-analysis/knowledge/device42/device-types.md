# Device 타입 체계

> 관측 2026-09-11 · 양쪽 서버
> 재조회 docs/data-analysis/exploration-queries/device42/subtype-census.sql

`view_device_v2.type` 과 서브타입은 적재 대상 판정에 쓰인다. ASSETCLASS 결정에는
쓰이지 않는다.

## type

마스터 뷰가 없다. `type_id` 와 `type` 이 1:1 이다.

| type_id | type | 192.168.2.68 | 192.168.1.35 |
| ---: | --- | ---: | ---: |
| 1 | unknown | 39 | – |
| 2 | physical | 7 | 9 |
| 3 | virtual | 56 | 79 |
| 4 | cluster | 2 | 2 |

`.68` 의 unknown 39건은 이름이 IP 주소이고 `hardware_fk` 와 `serial_no` 가 비어
있다. 현행 필터의 `type` 절에서 `unknown` 과 `cluster` 가 제외된다.

## virtualsubtype_id

마스터 뷰가 없다. `_v1`·`_v2` 모두 없다. `view_device_v2` 의 `virtualsubtype_id`
와 `virtualsubtype` 컬럼 쌍이 유일한 출처라서, 데이터에 등장한 값만 확인된다.
아래는 전체 목록이 아니다.

| id | virtualsubtype | 192.168.2.68 | 192.168.1.35 |
| ---: | --- | ---: | ---: |
| 1 | Internal VM | 1 | – |
| 2 | Amazon EC2 Instance | 8 | 5 |
| 11 | VMWare | 22 | 58 |
| 14 | Hyper-V | – | 2 |
| 15 | Docker Container | 25 | 14 |

id 3–10·12·13 은 미관측이다. 현행 필터는 `virtualsubtype_id <> 15` 로 Docker
Container 하나만 제외한다.

## physicalsubtype

`view_physicalsubtype_v2` 가 전량 15종을 준다. 양쪽 서버가 전건 동일하고 15종
모두 `system_generated` 가 참이다. 건수는 `view_device_v2` / `view_hardware_v2`
각각의 `physicalsubtype_fk` 기준이다.

| pk | 이름 | 플래그 | 장비 .68 | 장비 .35 | HW .68 | HW .35 |
| ---: | --- | --- | ---: | ---: | ---: | ---: |
| 1 | Generic | A | 3 | 7 | 4 | 5 |
| 2 | Rackable | B | 2 | – | 3 | – |
| 3 | Blade | C | – | – | – | – |
| 4 | PDU | D | 1 | 1 | 1 | 1 |
| 5 | Access Point | E | – | – | – | – |
| 6 | CRAC | F | – | – | – | – |
| 7 | UPS | D | – | – | – | – |
| 8 | TAP | D | – | – | – | – |
| 9 | Branch Circuit Power Meter | D | – | – | – | – |
| 10 | Power Unit | D | – | – | – | – |
| 11 | WorkStation | D | – | – | – | – |
| 12 | ThinClient | D | – | – | – | – |
| 13 | Network Printer | D | 1 | 1 | 1 | 1 |
| 14 | Laptop | D | – | – | – | – |
| 15 | Environment Monitor | F | – | – | – | – |

플래그는 배치 가능 위치다. 서명이 여섯 가지뿐이다.

| 플래그 | storage_room | server_room | building | rack | chassis |
| --- | --- | --- | --- | --- | --- |
| A | t | f | t | f | f |
| B | t | f | t | t | f |
| C | t | f | t | f | t |
| D | t | t | t | t | f |
| E | t | t | t | t | t |
| F | t | t | f | t | f |

서명 `D` 는 PDU·UPS·TAP·Branch Circuit Power Meter·Power Unit 과
WorkStation·ThinClient·Network Printer·Laptop 이 공유한다. 플래그만으로는 전력·
설비 계열을 가려낼 수 없다.

`Generic`·`Rackable`·`Blade` 는 장비 종류가 아니라 형태다. 같은 값이
`network_device` 참·거짓 양쪽에 걸린다. 같은 하드웨어 모델이 서버마다 다른
서브타입에 붙는다. `WS-C3750-24PS-S` 와 `C9200L-24P-4G` 는 `.68` 에서 `Rackable`,
`.35` 에서 `Generic` 이다.

전력·설비 계열은 `view_assettype_v1` 에도 별개로 있다. 목록과 관계는
`views.md` 마스터 뷰 절 참조.

적재 제외 대상은 [이슈 #1](https://github.com/ka-MS/discovery-interface/issues/1)
참조.

## 분포

값이 있는 조합만이다.

| type | subtype | virtual_host | network_device | .68 | .35 |
| --- | --- | --- | --- | ---: | ---: |
| virtual | VMWare | f | f | 16 | 50 |
| virtual | VMWare | t | f | 6 | 8 |
| virtual | Docker Container | f | f | 25 | 14 |
| virtual | Amazon EC2 Instance | f | f | 8 | 5 |
| virtual | Hyper-V | f | f | – | 2 |
| virtual | Internal VM | t | f | 1 | – |
| physical | Generic | t | f | 2 | 5 |
| physical | Generic | f | f | 1 | – |
| physical | Generic | f | t | – | 2 |
| physical | Rackable | f | t | 2 | – |
| physical | Network Printer | f | f | 1 | 1 |
| physical | PDU | f | f | 1 | 1 |
| cluster | – | f | t | 2 | 2 |
| unknown | – | f | f | 39 | – |

## ASSETCLASS 판정

현행 코드는 타입이 아니라 플래그로 판정한다.
근거: `DeployedAssetMapper.java` `mapData()`

| 순서 | 조건 | ASSETCLASS |
| --- | --- | --- |
| 1 | `network_device = true` | NETDEVICE |
| 2 | `physicalsubtype = 'Network Printer'` | NETPRINTER |
| 3 | 그 외 | COMPUTER |

`type` 과 `virtualsubtype` 은 판정에 쓰이지 않는다. VMWare, Amazon EC2,
Hyper-V, Internal VM, physical Generic 이 모두 COMPUTER 로 합쳐진다. PDU 는
판정 전에 조회 대상에서 제외된다.
