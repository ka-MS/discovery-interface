# 네트워크 CI 모델 — 인터페이스·IP·포트

> 관측 2026-09-17 · Maximo BLUDB 메타데이터 · Device42 192.168.2.68 / 192.168.1.35.
> 서버-IP 연결을 어떻게 표현할지 정하려고 조사했다. 결정과 사유는
> [CI 관계 설계](../../design/ci/relations.md)에 둔다.

## 1. CDM은 3층이다

```
SYS.COMPUTERSYSTEM ─RELATION.CONTAINS (1:N, containment=1)─> NET.L2INTERFACE
SYS.COMPUTERSYSTEM ─RELATION.CONTAINS (1:N, containment=1)─> NET.IPINTERFACE
NET.IPINTERFACE ─RELATION.BINDSTO (1:1)─> NET.L2INTERFACE
NET.IPINTERFACE ─RELATION.BINDSTO (1:1)─> NET.IPADDRESS
NET.TCPPORT · NET.UDPPORT ─RELATION.BINDSTO (1:1)─> NET.IPINTERFACE
<프로세스 계열> ─RELATION.ACCESSEDVIA (1:N)─> NET.TCPPORT
```

`CONTAINS` 두 규칙은 `SYS.COMPUTERSYSTEM`·`SYS.VIRTUALCOMPUTERSYSTEM`·`SYS.GENERICSWITCH`
세 분류 모두에 있다. **`NET.L2INTERFACE → NET.IPADDRESS` 직접 규칙은 없다.**
IP를 장비에 잇는 CDM 경로는 `NET.IPINTERFACE` 경유뿐이다.

IP는 NIC에 박히는 값이 아니라 바인딩이다. NIC 하나에 IP 여럿, 본딩이면 NIC 여럿에 IP 하나다.
`NET.IPINTERFACE`가 그 바인딩을 나타내며 CIM의 `IPProtocolEndpoint`에 대응한다.
속성이 비어 있는 것이 이 계층의 성격이다.

| 분류 | ACTCI 속성 | CI 속성 | 성격 |
| --- | ---: | ---: | --- |
| NET.L2INTERFACE | 31 | 13 | `HWADDRESS`·`NAME`·`SPEED`·`MTU`·`DUPLEX` 등 실체값 |
| NET.IPINTERFACE | 19 | 2 | `PORTLIST`·`STATUS` 외 관리값. 연결용 계층 |
| NET.TCPPORT · NET.UDPPORT | 각 18 | — | 전송 포트 |

## 2. D42 netport는 L2 인터페이스다

D42 UI가 "포트"로 표시하지만 실체는 NIC다. 근거 셋이다.

- `view_netport_v1`의 `port_type`이 전건 `physical`이고 `hwaddress`에 MAC이 있다
  (`.68` 256/306, `.35` 344/378).
- 이름이 `ens160`·`ens192`·`virbr0`·`veth5d1daf6@if14`·`vxlan1020`·`WAN Miniport (IP)`처럼
  OS 레벨 인터페이스 이름이다. `verbose_name`은 `ens160 - 00:0c:29:d0:c9:76 @ proxy.itmsg.co.kr` 형식이다.
- `NET.L2INTERFACE`의 속성 구성이 netport 컬럼과 그대로 겹친다.

D42는 서버의 NIC와 스위치의 포트를 같은 `view_netport_v1`에 담는다. 둘 다 L2 포트라
`NET.L2INTERFACE` 한 분류로 대응한다. `NET.TCPPORT`와는 다른 개체다.

**IPINTERFACE에 대응하는 D42 개체는 없다.** 원천은 `netport ← ipaddress` 2층이고
`view_ipaddress_v2.netport_fk`가 IP를 포트에 직결한다.

## 3. 원천 커버리지 — 2026-09-17

| 항목 | `.68` | `.35` |
| --- | ---: | ---: |
| 수집 범위 (IP, Computer) 쌍 | 51 | 118 |
| 그중 고유 IP | 50 | 97 |
| 수집 범위 IP 중 `netport_fk` 보유 | 42 (84%) | 87 (90%) |
| `netport_fk` 기준 (IP, Computer) 쌍 | 43 | 107 |
| IP가 붙은 netport | 37 | 70 |
| 수집 범위 장비의 netport 전체 | 88 | 277 |

`.35`의 netport 277개 중 207개는 IP가 없는 컨테이너 veth다. `MTU`·`SPEED`는 두 서버 전건 NULL이다.
한 포트에 IP가 여럿인 사례가 있다 — `.35`에 12개가 붙은 포트 1개, 3개 2개, 2개 2개.

IPINTERFACE를 만들 때 키를 IP 기준으로 잡으면 공유 IP의 인터페이스 하나를 Computer 둘이
`CONTAINS`하게 되고, (장비, IP) 기준으로 잡으면 `BINDSTO` 1:1이 깨진다. 어느 쪽이든
선언된 카디널리티와 어긋난다.

## 4. 접두어 없는 관계 — 로컬 확장

`RELATION.*` 102개가 CDM 계열이고, 접두어 없는 28개는 Maximo 자체 자산 관계 목록이다
(`AFFECTS`, `BACKED UP BY`, `INCLUDES`, `SPLITS FROM` 등 선형 자산용이 섞여 있다).
그중 규칙이 등록된 것은 둘뿐이다.

| RELATIONNUM | 규칙 | 등록 분류쌍 |
| --- | ---: | --- |
| `USES` | 2 | `SYS.COMPUTERSYSTEM`·`SYS.VIRTUALCOMPUTERSYSTEM` → `NET.IPADDRESS`, `N:N` |
| `VIRTUALIZES` | 2 | Host → VM, `1:N` |

둘 다 이 프로젝트에서 등록한 로컬 확장이다. CDM 쪽 대응(`RELATION.VIRTUALIZES`는
Computer→Computer `1:1`, IP는 IPINTERFACE 경유)이 카디널리티나 원천 때문에 맞지 않아
선택했다. 표준 이탈이므로 IBM 디스커버리를 병행 도입할 때 재검토 대상이다.

## 5. 서버-스위치 물리 연결

`view_netport_v1.remote_netport_fk`에 서버-스위치 링크가 있다. `.68` 5건, `.35` 2건이다.
다만 상대 포트가 전부 `Vlan1`(SVI)이라 실제 결선이 아니라 MAC 학습 기반 연관으로 보인다.
상대 장비도 스위치 스택(`cluster`)이고 우리가 CI로 올리는 물리 멤버가 아니다.

Maximo에는 Computer-Switch 직접 규칙이 없다. `NET.NETWORKCONNECTION`을 CI로 만들고
양 끝을 `RELATION.CONNECTEDFROM`·`RELATION.CONNECTEDTO`로 잇는 모델이다.

**우리가 ACTCI로 올린 물리 스위치는 netport가 0개다.** 포트는 전부 cluster 객체에 있다.

| 장비 | 유형 | netport | MAC 보유 |
| --- | --- | ---: | ---: |
| ITMSG_L3_SW1 | cluster | 33 / 32 | 30 / 29 |
| ITMSG_L2_SW1 | cluster | 28 / 28 | 27 / 27 |
| ITMSG_L3_SW1 - Switch 1 | physical | 0 | 0 |
| ITMSG_L2_SW1 - Switch 1 | physical | 0 | 0 |

수치는 `.68 / .35` 순이다. 사업 범위의 네트워크 행에 "Network Interface 정보"가 명시돼 있으므로
스위치 인터페이스 수집은 요구사항이다. 그러려면 스위치 CI를 물리 멤버로 둘지 cluster로 둘지
먼저 정해야 한다. [ISSUE-11](../../open-issues.md)에서 관리한다.
