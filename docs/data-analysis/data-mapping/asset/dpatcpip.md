# DPATCPIP

배치된 자산 컴퓨터 TCP/IP

> Target: MAXIMO.DPATCPIP · ASSETCLASS: COMPUTER · 구현: DpaTcpIpIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : 1 DPATCPIP (관측 53노드/53행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_ipaddress_v1`, `view_subnet_v1` | MAXIMO.DPATCPIP |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DHCPSERVER | DHCP 서버 | ALN(100) | Y |  |  |  |
| DNSSERVER1 | DNS 서버 1 | ALN(100) | Y |  |  |  |
| DNSSERVER2 | DNS 서버 2 | ALN(100) | Y |  |  |  |
| DNSSERVER3 | DNS 서버 3 | ALN(100) | Y |  |  |  |
| GATEWAY | 게이트웨이 | ALN(32) | Y |  |  |  |
| HOST | 호스트 | ALN(128) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| PRIMARYWINS | 기본 WINS | ALN(32) | Y |  |  |  |
| SECONDARYWINS | 보조 WINS | ALN(32) | Y |  |  |  |
| TCPIPADDRESS | TCP/IP 주소 | ALN(39) | N |  |  |  |
| TCPIPDOMAIN | TCP/IP 도메인 | ALN(256) | Y |  |  |  |
| TCPIPID | TcpIp ID | BIGINT(19) | N |  |  |  |
| TCPIPNETMASK | 네트워크 마스크 | ALN(32) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

