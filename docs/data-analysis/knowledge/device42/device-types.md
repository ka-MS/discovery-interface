# Device 타입 체계

> 관측 2026-08-27 · Device42 192.168.1.35
> 재조회 docs/data-analysis/exploration-queries/device42/device-type-distribution.sql
> 서브타입 절 2026-08-31 · 양쪽 서버
> 재조회 docs/data-analysis/exploration-queries/device42/view-version-probe.sql

`view_device_v2.type` 과 서브타입은 적재 대상 판정에 쓰인다. ASSETCLASS 결정에는
쓰이지 않는다.

## virtualsubtype_id

마스터 뷰가 없다. `view_device_v2` 의 `virtualsubtype_id` 와 `virtualsubtype`
컬럼 쌍이 유일한 출처라서, 데이터에 등장한 값만 확인된다. 아래는 전체 목록이
아니다. `physicalsubtype` 은 `view_physicalsubtype_v2` 로 전량을 볼 수 있다.

| id | virtualsubtype | 192.168.2.68 | 192.168.1.35 |
| --- | --- | ---: | ---: |
| 2 | Amazon EC2 Instance | 8 | 5 |
| 11 | VMWare | 18 | 55 |
| 14 | Hyper-V | – | 2 |
| 15 | Docker Container | 25 | 12 |

현행 필터는 `type IN ('virtual', 'physical')`, Docker Container 제외, PDU 제외다.
`type` 절에서 `cluster`·`unknown` 이 제외된다.
근거: `DeployedAssetIntegrate.java` `DEVICE_FILTER`

## physicalsubtype

`view_physicalsubtype_v2` 가 전량 15종을 준다. `building` 은 건물 배치 가능
여부다.

| pk | 이름 | building | 관측 .68 | 관측 .35 |
| ---: | --- | --- | ---: | ---: |
| 1 | Generic | t | 2 | 6 |
| 2 | Rackable | t | 2 | – |
| 3 | Blade | t | – | – |
| 4 | PDU | t | – | 1 |
| 5 | Access Point | t | – | – |
| 6 | CRAC | **f** | – | – |
| 7 | UPS | t | – | – |
| 8 | TAP | t | – | – |
| 9 | Branch Circuit Power Meter | t | – | – |
| 10 | Power Unit | t | – | – |
| 11 | WorkStation | t | – | – |
| 12 | ThinClient | t | – | – |
| 13 | Network Printer | t | 1 | 1 |
| 14 | Laptop | t | – | – |
| 15 | Environment Monitor | **f** | – | – |

15종 중 데이터에 등장한 것은 네 종뿐이다.

`Generic`·`Rackable`·`Blade` 는 장비 종류가 아니라 형태다. 같은 값이
`network_device` 참·거짓 양쪽에 걸린다. `.68` 의 `Rackable` 2건은 참,
`.35` 의 `Generic` 6건은 참 2 · 거짓 4다.

적재 제외 대상은 `../../open-issues.md` ISSUE-10 참조.

## 분포

| type | virtualsubtype | physicalsubtype | virtual_host | network_device | device_cnt |
| --- | --- | --- | --- | --- | --- |
| virtual | VMWare | - | f | f | 49 |
| virtual | Docker Container | - | f | f | 12 |
| virtual | VMWare | - | t | f | 6 |
| virtual | Amazon EC2 Instance | - | f | f | 5 |
| physical | - | Generic | t | f | 4 |
| virtual | Hyper-V | - | f | f | 2 |
| cluster | - | - | f | t | 2 |
| physical | - | Generic | f | t | 2 |
| unknown | - | - | t | f | 1 |
| physical | - | PDU | f | f | 1 |
| physical | - | Network Printer | f | f | 1 |

## ASSETCLASS 판정

현행 코드는 타입이 아니라 플래그로 판정한다.
근거: `DeployedAssetIntegrate.java` `mapData()`

| 순서 | 조건 | ASSETCLASS |
| --- | --- | --- |
| 1 | `network_device = true` | NETDEVICE |
| 2 | `physicalsubtype = 'Network Printer'` | NETPRINTER |
| 3 | 그 외 | COMPUTER |

`type` 과 `virtualsubtype` 은 판정에 쓰이지 않는다. VMWare, Amazon EC2,
Hyper-V, physical Generic 이 모두 COMPUTER 로 합쳐진다. PDU 는 판정 전에
조회 대상에서 제외된다.
