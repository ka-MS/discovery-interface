# DPATCPIP

배치된 자산 컴퓨터 TCP/IP

> Target: MAXIMO.DPATCPIP · ASSETCLASS: COMPUTER · 구현: [TcpIpImport](../../../../src/main/java/com/itmsg/device42/integration/d42maximo/asset/tcpip/TcpIpImport.java) · [TcpIpQuery](../../../../src/main/java/com/itmsg/device42/integration/d42maximo/asset/tcpip/TcpIpQuery.java) · [TcpIpMapper](../../../../src/main/java/com/itmsg/device42/integration/d42maximo/asset/tcpip/TcpIpMapper.java) · [DpaTcpIpWriter](../../../../src/main/java/com/itmsg/device42/maximo/asset/DpaTcpIpWriter.java)
> 관측 2026-08-31, 설계 갱신 2026-09-17 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPATCPIP. IP가 여러 장비에 연결되면 장비–IP 쌍마다 한 행이다
- 선행: DEPLOYEDASSET
- MERGE 자연키: `(NODEID, TCPIPADDRESS)` = `(device_pk, HOST(ip_address))`
- 기술 PK: `TCPIPID = NEXT VALUE FOR MAXIMO.DPATCPIPSEQ`

기존 수집분은 노드당 1행이었지만 스키마는 N을 허용한다. Device42 는 장비당
IP가 여럿이고 `device_fks` 하나가 장비 여럿을 가리킬 수도 있으므로
장비–IP 연결 1건당 DPATCPIP 한 행을 적재한다. 대표 IP나 대표 장비 선택 규칙은 두지 않는다.

`device_fks` 배열은 `= ANY(...)` 조인으로 모두 펼친다. 2026-09-17 재조회 결과는
`.35` 97 IP / 118 장비–IP 쌍, `.68` 50 IP / 51쌍이며
`(device_pk, HOST(ip_address))` 중복은 양쪽 모두 0건이다. `ipaddress_pk`를
`TCPIPID`로 직접 사용하면 공유 IP의 두 번째 장비 행부터 PK가 충돌하므로,
`TCPIPID`는 신규 INSERT에서만 Maximo 시퀀스로 채번한다. 전환 시 기존
DPATCPIP 데이터는 전량 삭제 후 다시 적재한다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_ipaddress_v2` | MAXIMO.DPATCPIP | – | IP–장비 연결 1건 = 행 1건. 공유 IP는 여러 행 |
| `view_subnet_v1` | (보강) | `view_ipaddress_v2.subnet_fk = subnet_pk` | N:1 |
| `view_device_v2` | (부모·대상 판정·HOST) | `device_pk = ANY(view_ipaddress_v2.device_fks)` | 장비–IP 연결을 행으로 펼침 |

아래 장비당 IP 건수 분포는 2026-08-31 관측값이다. 2.68 에는 IP 를 9개 가진
장비가 있어 1:N 이 관측으로도 분명하다.

| IP 건수 | 192.168.1.35 | 192.168.2.68 |
| --- | --- | --- |
| 0 | 7 | 1 |
| 1 | 52 | 16 |
| 2 | 4 | 10 |
| 3 | 2 | – |
| 4 | 1 | – |
| 9 | – | 1 |
| 합계 | 66장비 / 70행 | 28장비 / 45행 |

아래 컬럼 매핑의 수치는 `1.35 / 2.68` 순으로 적는다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer','PDU'))` | 다른 ASSETCLASS와 PDU의 자식을 만들지 않는다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DHCPSERVER | DHCP 서버 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/53 |
| DNSSERVER1 | DNS 서버 1 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/53 |
| DNSSERVER2 | DNS 서버 2 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다 |
| DNSSERVER3 | DNS 서버 3 | ALN(100) | Y | 원천없음 | – | 대응 원천이 없다 |
| GATEWAY | 게이트웨이 | ALN(32) | Y | 직접 | `view_subnet_v1.gateway` | 관측 0/70 · 0/45. 두 서버 모두 전건 비어 있어 현재는 NULL 이다 |
| HOST | 호스트 | ALN(128) | Y | 직접 | `view_device_v2.name` | 관측 70/70 · 45/45, 최대 41자. 같은 장비라도 서버에 따라 이름이 다를 수 있다 |
| NODEID | 노드 ID | BIGINT(19) | N | 변환 | `view_ipaddress_v2.device_fks` | 배열이라 조인한 `view_device_v2.device_pk` 를 쓴다. 부모 DEPLOYEDASSET와 동일한 ID다 |
| PRIMARYWINS | 기본 WINS | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| SECONDARYWINS | 보조 WINS | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다 |
| TCPIPADDRESS | TCP/IP 주소 | ALN(39) | N | 변환 | `view_ipaddress_v2.ip_address` | `HOST(ip_address)`. inet 타입이라 그냥 캐스팅하면 `/32` 접미가 붙는다. 관측 70/70 · 45/45, 접미 포함 최대 18자. IPv6 는 양쪽 0건 |
| TCPIPDOMAIN | TCP/IP 도메인 | ALN(256) | Y | 원천없음 | – | 대응 원천이 없다. 장비명에 FQDN 이 섞여 있으나 도메인 컬럼이 아니다 |
| TCPIPID | TcpIp ID | BIGINT(19) | N | 채번 | `MAXIMO.DPATCPIPSEQ` | `WHEN NOT MATCHED`의 INSERT에서만 `NEXT VALUE FOR`로 채번한다 |
| TCPIPNETMASK | 네트워크 마스크 | ALN(32) | Y | 변환 | `view_subnet_v1.mask_bits` | 비트 수를 점 표기 넷마스크로 변환한다(`24` → `255.255.255.0`). `mask_bits = 0` 은 catch-all 서브넷이므로 NULL |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

`mask_bits` 관측 분포는 아래와 같다. catch-all 서브넷(`mask_bits = 0`)의
비중이 서버마다 다르다.

| mask_bits | 192.168.1.35 | 192.168.2.68 |
| --- | --- | --- |
| 24 | 47 | 12 |
| 22 | 10 | 13 |
| 20 | 5 | 5 |
| 0 (catch-all) | 8 | 15 |

`TCPIPNETMASK` 는 기존 수집분이 0/53 이라 표기 선례가 없다. 점 표기는 컬럼
한글명(네트워크 마스크)과 길이 ALN(32)에 맞춘 선택이다.

## 5. 조회 쿼리

```sql
SELECT
    i.ipaddress_pk,
    d.device_pk AS device_fk,
    d.name AS device_name,
    HOST(i.ip_address) AS ip_address,
    i.ip_hybrid,
    i.label,
    i.subnet_fk,
    i.type_id,
    i.type,
    i.available,
    i.is_public,
    i.resource_fk,
    i.notes,
    i.first_added,
    i.last_edited,
    i.tags,
    i.netport_fk,
    i.details,
    i.last_changed,
    i.last_discovered,
    i.is_shared,
    i.cloudinfrastructure_fk,
    b.gateway,
    b.mask_bits
FROM view_ipaddress_v2 i
JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
ORDER BY i.ipaddress_pk, d.device_pk
```

## 6. 미결

- 현행 MERGE는 신규·변경만 반영한다. 이후 Device42의 `device_fks`에서 제거된
  장비–IP 연결을 Maximo에서도 삭제하는 동기화 정책은 별도로 정해야 한다.
