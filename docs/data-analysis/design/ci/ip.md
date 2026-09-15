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

**대조 기준은 `CI.IPADDRESS`(CCI00011) 6개다.** 적재 대상은 `NET.IPADDRESS`(22개 = `IPADDRESS_` 8 + `MODELOBJECT_` 14)다.
관측은 [분류 조사](../../knowledge/maximo/ci-component-classifications.md) 6절.

| ASSETATTRID | CI.IPADDRESS | 자료형 | 원천 | 채택 | 비고 |
| --- | :---: | --- | --- | --- | --- |
| IPADDRESS_DOTNOTATION | ○ | ALN | `HOST(i.ip_address)` | 채택 | 전건 |
| IPADDRESS_STRINGNOTATION | ○ | ALN | `HOST(i.ip_address)` | 채택 | CI 기준 속성. DOTNOTATION과 같은 값 |
| IPADDRESS_MANAGEDSYSTEMNAME | ○ | ALN | 연결 장비의 `d.name` | 채택 | 여러 장비면 `device_pk` 최소인 장비 |
| IPADDRESS_ADDRESSTYPE | ○ | NUMERIC | v4/v6 판정 가능 | 미채택 | **코드 규약 없음.** 아래 참조 |
| IPADDRESS_ADDRESSSPACE | ○ | ALN | – | 미채택 | 원천 없음 |
| IPADDRESS_BYTENOTATION | ○ | ALN | – | 미채택 | 원천 없음 |
| **MODELOBJECT_LABEL** | **✗** | ALN | `i.label` | **채택** | 55 / 59건. **CI 기준 밖 의도적 추가** |
| MODELOBJECT_CDMSOURCE·SOURCETOKEN | ✗ | ALN | 상수·원천 키 | 미채택 | 아래 참조 |
| 나머지 MODELOBJECT_ 11개 | ✗ | – | – | 미채택 | 원천 없음 |

### ADDRESSTYPE는 값을 정할 수 없다

v4/v6 판정 자체는 원천에서 된다. 그러나 `IPADDRESS_ADDRESSTYPE`은 NUMERIC인데
`ASSETATTRIBUTE.DOMAINID`가 비어 있고 `ACTCISPEC`·`CISPEC` 통틀어 기존 값이 **0건**이다.
코드 규약을 Maximo 메타데이터에서 알 수 없어 임의 값을 넣지 않는다.

분류를 `NET.IPV4ADDRESS`·`NET.IPV6ADDRESS`로 나누면 코드 없이도 구분이 표현된다.
9절 미결에 남긴다.

### MODELOBJECT_CDMSOURCE·SOURCETOKEN을 쓰지 않는 이유

네 유형 공통 결정이다. 2026-09-15 확인했다.

- **CI 계열 분류에 `MODELOBJECT_` 속성이 하나도 없다.** `CI.IPADDRESS` 0/6, `CI.OS` 0/7,
  `CI.FILESYSTEM` 0/16, `CI.COMPUTERSYSTEM` 0/19다. ACTCI 쪽은 모두 14개씩 갖는다.
  적재해도 승격에서 전달되지 않는다.
- 같은 정보를 이미 담고 있다. `ACTCINUM`이 `D42:IPADDRESS:<pk>`로 원천 키를,
  `CHANGEBY='Device42'`가 연계 출처를 나타낸다.
- `ACTCISPEC`·`CISPEC` 전체에 이 두 속성의 기존 값이 0건이다. 환경 선례가 없다.

`MODELOBJECT_LABEL`은 다르다. 원천 `i.label`에 실제 값이 있어 채택했다.
다만 위와 같은 이유로 승격 전달은 기대하지 않는다.

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

**네 유형 중 유일하게 Computer와 직접 연결하는 명시 규칙이 없다.**
`SYS.*COMPUTERSYSTEM`과 `NET.IPADDRESS` 사이에 `RELATIONRULES`가 양방향 모두 0건이다.

2026-09-15 보완: 이는 물리 INSERT 불가라는 뜻이 아니다. 현재 검증된 규칙을 쓰는 설계에서
직접 관계를 보류한다는 뜻이다. Interface 경로의 실제 연결·카디널리티 제한은
[관계 설계](relations.md)와 [관계 원천](../../knowledge/device42/computer-ci-relations.md)을 따른다.
Interface 하나의 도입만으로 공유 IP의 모든 장비 연결이 복구되지는 않는다.

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

## 7. 수집 대상 범위 — 장비 연결 전체

| 범위 | 건수 | 수집 |
| --- | ---: | --- |
| 전체 IP | 297 / 543 | |
| **장비 연결 있음** | **105 / 111** | **대상** |
| 그중 Computer 연결 | 50 / 97 | |
| 장비 연결 없음 | 192 / 432 | 제외 |

2026-09-15 결정이다. 처음에는 Computer 연결분만 수집했으나 범위를 넓혔다.

**IP는 장비에 종속된 개체가 아니라 독립 CI다.** 부모 유형으로 자식을 거르면
장비 유형을 추가할 때마다 IP 조회를 고쳐야 한다. 네트워크 장비·컨테이너를
CI로 추가하면 그 장비의 IP가 이미 들어와 있고 관계만 붙이면 된다.

IP가 붙은 장비를 유형별로 세면 다음과 같다.

| 장비 유형 | .68 | .35 | Computer 필터 대상 |
| --- | ---: | ---: | :---: |
| virtual VMWare / EC2 / Internal VM | 39 | 91 | ○ |
| physical Generic | 12 | 6 | ○ |
| type=unknown | 38 | 0 | ✗ |
| virtual Docker Container | 17 | 10 | ✗ |
| cluster (`network_device=true`) | 2 | 2 | ✗ |
| Network Printer · PDU | 2 | 2 | ✗ |

Computer 필터로는 `unknown` 38건과 Docker 27건이 빠진다. 실제 네트워크 장비
(`network_device=true`) IP는 이 표본에 cluster 2건뿐이다.

장비에 안 붙은 192 / 432건은 계속 제외한다. 서브넷에만 할당된 주소이고
관계를 만들 대상이 없다.

## 8. 미결

| 항목 | 상태 | 추적 |
| --- | --- | --- |
| **관계 경로** | Interface CI 도입 추천. **범위 확대라 사용자 결정 필요** | ISSUE-8 |
| 채울 속성이 주소 하나 | 독립 CI 유지 여부 재확인 | ISSUE-8 |
| 서브넷 마스크 적재 불가 | Subnet CI 필요. 이번 범위 밖 | ISSUE-8 |
| `IPADDRESS_ADDRESSTYPE` 코드 규약 | 미확인 | ISSUE-11 |
| 분류를 v4/v6로 나눌지 | `NET.IPV4ADDRESS`·`NET.IPV6ADDRESS`가 속성 22개 동일하고 승격 범위에도 등록돼 있다. 원천 v6는 0 / 2건 | ISSUE-8 |
| MODELOBJECT_LABEL 승격 전달 | CI 계열에 MODELOBJECT_ 속성이 없어 누락 가능. 미검증 | ISSUE-11 |
| 배열 `DISTINCT ON` | 실제 필요. 적용 확정 | ISSUE-9 |
