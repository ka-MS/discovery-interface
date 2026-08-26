# Device 타입 체계

> 관측 2026-08-27 · Device42 192.168.1.35
> 재조회 docs/data-analysis/exploration-queries/device42/device-type-distribution.sql

`view_device_v2.type` 과 서브타입은 적재 대상 판정에 쓰인다. ASSETCLASS 결정에는
쓰이지 않는다.

## virtualsubtype_id

| id | virtualsubtype |
| --- | --- |
| 2 | Amazon EC2 Instance |
| 11 | VMWare |
| 14 | Hyper-V |
| 15 | Docker Container |

현행 필터는 `type IN ('virtual', 'physical') AND (virtualsubtype_id IS NULL OR
virtualsubtype_id <> 15)` 다. `type` 절에서 `cluster`·`unknown` 이 제외되고,
`virtualsubtype_id` 조건에서 Docker Container 가 제외된다.
근거: `DeployedAssetIntegrate.java` `DEVICE_FILTER`

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
Hyper-V, physical Generic 이 모두 COMPUTER 로 합쳐진다.
