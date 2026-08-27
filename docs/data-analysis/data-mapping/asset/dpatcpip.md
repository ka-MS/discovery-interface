# DPATCPIP

배치된 자산 컴퓨터 TCP/IP

> Target: MAXIMO.DPATCPIP · ASSETCLASS: COMPUTER · 구현: DpaTcpIpIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPATCPIP (기존 수집분 53노드/53행)
- 선행: DEPLOYEDASSET

기존 수집분은 노드당 1행이지만 스키마는 N을 허용한다. `DPATCPIP_NDX1` 이
`(TCPIPID, NODEID)` 유일이고 `NODEID` 단독 인덱스(`DPATCPIP_NDX3`)는
비유일이다. Device42 는 장비당 IP 가 여럿이므로 IP 1건당 1행을 적재한다.
1행으로 줄이기 위한 대표 IP 선택 규칙은 두지 않는다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_ipaddress_v1` | MAXIMO.DPATCPIP | – | 1:1 (IP 1건 = 행 1건) |
| `view_subnet_v1` | (보강) | `view_ipaddress_v1.subnet_fk = subnet_pk` | N:1 |
| `view_device_v2` | (대상 판정·HOST) | `view_ipaddress_v1.device_fk = device_pk` | N:1 |
| MAXIMO.DEPLOYEDASSET | (교차키) | `SOURCEID = view_ipaddress_v1.device_fk AND IMPORTSOURCE = 'Device42'` → `NODEID` | N:1 |

COMPUTER 대상 67장비 중 60장비/71행이다. 장비당 IP 건수 분포는 아래와 같다.

| IP 건수 | 장비 수 |
| --- | --- |
| 0 | 7 |
| 1 | 53 |
| 2 | 4 |
| 3 | 2 |
| 4 | 1 |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')` | 다른 ASSETCLASS의 자식을 만들지 않는다 |
| 부모 존재 | 교차키 조회 결과가 있는 것만 | 부모가 없으면 적재할 수 없다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DHCPSERVER | DHCP 서버 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/53 |
| DNSSERVER1 | DNS 서버 1 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/53 |
| DNSSERVER2 | DNS 서버 2 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다 |
| DNSSERVER3 | DNS 서버 3 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다 |
| GATEWAY | 게이트웨이 | ALN(32) | Y | 직접 | `view_subnet_v1.gateway` | 관측 0/71이라 현재는 전건 NULL |
| HOST | 호스트 | ALN(128) | Y | 직접 | `view_device_v2.name` | 관측 71/71, 최대 41자 |
| NODEID | 노드 ID | BIGINT(19) | N | 채번 | – | 부모 DEPLOYEDASSET.NODEID. `(SOURCEID, IMPORTSOURCE)` 로 조회 |
| PRIMARYWINS | 기본 WINS | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| SECONDARYWINS | 보조 WINS | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| TCPIPADDRESS | TCP/IP 주소 | ALN(39) | N | 변환 | `view_ipaddress_v1.ip_address` | `HOST(ip_address)`. inet 타입이라 그냥 캐스팅하면 `/32` 접미가 붙는다. 관측 71/71, 접미 포함 최대 16자. IPv6 0/71 |
| TCPIPDOMAIN | TCP/IP 도메인 | ALN(256) | Y | 원천없음 | – | 대응 원천이 없다. 장비명에 FQDN 이 섞여 있으나 도메인 컬럼이 아니다 |
| TCPIPID | TcpIp ID | BIGINT(19) | N | 채번 | – | 대리키. 원천 `ipaddress_pk` 는 재수집 시 바뀌므로 쓰지 않는다 |
| TCPIPNETMASK | 네트워크 마스크 | ALN(32) | Y | 변환 | `view_subnet_v1.mask_bits` | 비트 수를 점 표기 넷마스크로 변환한다(`24` → `255.255.255.0`). `mask_bits = 0` 은 catch-all 서브넷이므로 NULL |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

`mask_bits` 관측 분포는 `24` 48행, `22` 10행, `20` 5행, `0`(`undefined`
서브넷) 8행이다. `TCPIPNETMASK` 는 기존 수집분이 0/53 이라 표기 선례가 없다.
점 표기는 컬럼 한글명(네트워크 마스크)과 길이 ALN(32)에 맞춘 선택이다.

## 5. 조회 쿼리

```sql
SELECT
    i.device_fk,
    d.name AS device_name,
    HOST(i.ip_address) AS ip_address,
    b.gateway,
    b.mask_bits
FROM view_ipaddress_v1 i
JOIN view_device_v2 d ON d.device_pk = i.device_fk
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype <> 'Network Printer')
ORDER BY i.device_fk, i.ip_address
```

## 6. 미결

- ISSUE-5 — `TCPIPID` 채번 범위와 재실행 시 키 유지 규칙이 미정이다.
