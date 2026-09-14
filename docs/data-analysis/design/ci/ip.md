# IP CI 수집 설계

> 상태: 추천안 · 작성 2026-09-15. 관계 경로가 미결이라 네 유형 중 결정 부담이 가장 크다.
> 원천 관측: [OS·Disk·Filesystem·IP 원천 조사](../../knowledge/device42/ci-component-inventory.md)
> 타겟 관측: [OS·Disk·Filesystem·IP 분류 조사](../../knowledge/maximo/ci-component-classifications.md)
> 확정 매핑: [IP 매핑](../../data-mapping/ci/types/ip.md)

수치는 `.68 / .35` 순이다.

## 1. 관리 단위 — 독립 CI

2026-09-15 사용자 합의다. Computer 하나에 IP가 여럿이고, IP 하나가 여러 Computer에 걸리기도 한다
(`device_fks` 최대 3 / 7개). Computer 스펙으로 담을 수 없다.

사업 범위 발췌에 IP를 독립 CI로 관리하라는 요구는 명시돼 있지 않다.
「네트워크 — Network Interface 정보 등」이 가장 가까운 항목이다. 6절과 함께 볼 사안이다.

## 2. 분류 선택 — NET.IPADDRESS

| 후보 | CLASSSTRUCTUREID | 스펙 | 판정 |
| --- | --- | ---: | --- |
| **NET.IPADDRESS** | CCI10810 | 22 | **추천** |
| NET.IPINTERFACE | CCI10490 | 19 | 다른 개체. 6절 참조 |

`NET.IPADDRESS`는 주소 자체를 뜻한다. 원천 `view_ipaddress_v2` 한 행이 주소 한 개이므로 대응한다.
`NET.IPINTERFACE`는 네트워크 인터페이스이며 원천의 `view_netport_v1`에 해당하는 별개 개체다.

`NET.IPADDRESS`는 후보 중 스펙이 가장 적다. 고유 속성 8개에 `MODELOBJECT_` 14개다.

## 3. 스펙 대조표

| ASSETATTRID | 자료형 | 원천 | 채택 | 비고 |
| --- | --- | --- | --- | --- |
| IPADDRESS_DOTNOTATION | ALN | `HOST(i.ip_address)` | 채택 | 전건. `192.168.2.127` |
| IPADDRESS_STRINGNOTATION | ALN | `HOST(i.ip_address)` | 보류 | DOTNOTATION과 중복 |
| IPADDRESS_ADDRESSTYPE | NUMERIC | IPv4 / IPv6 구분 | 보류 | **코드 규약 미확인** |
| IPADDRESS_ADDRESSSPACE | ALN | 대응 없음 | 미채택 | |
| IPADDRESS_BYTENOTATION | ALN | 대응 없음 | 미채택 | |
| MODELOBJECT_LABEL | ALN | `i.label` | 검토 | 55 / 59건. 공백 제외 |
| MODELOBJECT_CDMSOURCE | ALN | 상수 `Device42` | 검토 | |
| MODELOBJECT_SOURCETOKEN | ALN | `D42:IPADDRESS:<pk>` | 검토 | |
| 나머지 | – | – | 미채택 | 원천 대응 없음 |

**채울 수 있는 속성이 사실상 주소 하나다.** 네 유형 중 가장 빈약하다.

`CAST(i.ip_address AS VARCHAR)`는 `192.168.2.127/32`를 낸다. 실제 프리픽스가 아닌 `/32`가
붙으므로 쓰지 않는다. `HOST()`로 주소만 뽑는다.

### 서브넷 마스크가 들어갈 자리가 없다

원천 `view_subnet_v1.mask_bits`는 전건 보유인데 `NET.IPADDRESS`에 대응 속성이 없다.
서브넷은 `NET.IPNETWORK`(CCI10390, 25개)가 별도 분류이고
`NET.IPADDRESS → NET.IPNETWORK`에 `RELATION.MEMBEROF`(1:1) 규칙이 있다.

마스크를 담으려면 Subnet을 별도 CI로 만들어야 한다. 이번 범위 밖이다.
`gateway`는 컬럼은 있으나 두 서버 모두 값이 0건이다.

## 4. 본체 필드

| ACTCI 컬럼 | 원천 | 비고 |
| --- | --- | --- |
| ACTCINUM | `D42:IPADDRESS:<ipaddress_pk>` | 5절 |
| ACTCINAME | `HOST(i.ip_address)` | 전건 |
| CLASSSTRUCTUREID | NET.IPADDRESS 조회값 | |
| DESCRIPTION | `i.notes` | 값 분포 미확인 |
| LASTSCANDT | `i.last_discovered` | **전건 보유.** 네 유형 중 유일하게 자체 발견 시각이 있다 |
| CHANGEBY / LANGCODE / CHANGEDATE / HASLD | Computer와 동일 규약 | |

## 5. 식별자

`ACTCINUM = D42:IPADDRESS:<ipaddress_pk>`. 두 서버 모두 전건 유일하다.

`device_fks`는 실제로 다중이다(최대 3 / 7). 원천 PK 하나당 CI 하나를 만들고
Computer 연결은 관계 N개로 표현한다. 조회에서 Computer를 조인하면 같은 `ipaddress_pk`가
여러 행이 되므로 `DISTINCT ON (ipaddress_pk)`가 **실제로 필요하다**. 근거는 ISSUE-9.

## 6. 관계 — 직접 규칙이 없다

**네 유형 중 유일하게 Computer와 직접 연결할 수 없다.**
`SYS.*COMPUTERSYSTEM`과 `NET.IPADDRESS` 사이에 `RELATIONRULES`가 양방향 모두 0건이다.

CDM이 정의한 경로는 인터페이스를 거친다.

```text
Computer ──CONTAINS(1:N)──> NET.IPINTERFACE ──BINDSTO(1:1)──> NET.IPADDRESS
                                                                   │
                                                          MEMBEROF(1:1)
                                                                   ▼
                                                            NET.IPNETWORK
```

원천도 같은 모양이다. `view_ipaddress_v2.netport_fk` → `view_netport_v1.device_fk`가
이 경로이고, `subnet_fk`가 `NET.IPNETWORK`에 대응한다.

문제는 커버리지다. `netport_fk`를 가진 IP는 78 / 103건뿐이다. Computer에 연결된 IP 50 / 97건 중
인터페이스를 거치는 것이 몇 건인지는 추가 확인이 필요하다.

### 선택지

1. **Network Interface를 CI로 함께 도입한다.** `view_netport_v1`을 `NET.IPINTERFACE`로 적재하고 Computer→Interface→IP 두 관계를 만든다. 모델이 정확하고 사업 범위 「네트워크 — Network Interface 정보」에도 대응한다. 대신 이번 범위에 유형이 하나 늘어난다.
2. **IP를 본체·속성만 적재하고 관계를 보류한다.** 고아 CI가 되지만 주소 목록은 남는다.
3. **IP를 이번 범위에서 제외한다.** Interface 도입과 함께 다음 단계로 넘긴다.

추천은 **1**이다. 관계 없는 IP CI는 「어느 서버의 주소인가」를 답하지 못해 쓸모가 크게 준다.
Interface는 원천이 88 / 241건으로 충분하고 `device_fk` 직접 연결이라 수집이 단순하다.
다만 **범위 확대이므로 사용자 결정이 필요하다.** 결정 전까지 IP 매핑 문서는 본체·속성까지만 쓴다.

3도 방어 가능하다. IP 속성이 주소 하나뿐이라 Interface 없이 얻는 정보가 적기 때문이다.

## 7. 수집 대상 범위

| 범위 | 건수 | 비율 |
| --- | ---: | ---: |
| 전체 IP | 297 / 543 | 100% |
| 장비 연결 있음 | 105 / 111 | 35% / 20% |
| Computer 연결 | 50 / 97 | 17% / 18% |

**IP의 3분의 2는 어떤 장비에도 붙어 있지 않다**(192 / 432건). 서브넷에만 할당된 주소다.

추천은 **Computer 연결분만**이다. 장비 미연결 IP는 관계를 만들 대상이 없고,
사업 범위의 수집 구분과도 연결되지 않는다. IP 주소 관리 자체가 목적이라면
Subnet CI와 함께 별도로 설계할 사안이다.

## 8. 미결

| 항목 | 상태 | 추적 |
| --- | --- | --- |
| **관계 경로** | Interface CI 도입 추천. **범위 확대라 사용자 결정 필요** | ISSUE-8 |
| 채울 속성이 주소 하나 | 독립 CI 유지 여부 재확인 | ISSUE-8 |
| 서브넷 마스크 적재 불가 | Subnet CI 필요. 이번 범위 밖 | ISSUE-8 |
| `IPADDRESS_ADDRESSTYPE` 코드 규약 | 미확인 | ISSUE-11 |
| 수집 대상 범위 | Computer 연결분 추천 | ISSUE-8 |
| 배열 `DISTINCT ON` | 실제 필요. 적용 확정 | ISSUE-9 |
