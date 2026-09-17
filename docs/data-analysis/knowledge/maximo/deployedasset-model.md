# DEPLOYEDASSET 모델

> 관측 2026-08-27 · DPATCPIP 적재키 설계 갱신 2026-09-17 · Maximo BLUDB
> 재조회 docs/data-analysis/exploration-queries/maximo/dpa-child-coverage.sql

배치된 자산(Deployed Assets). 수집 도구가 발견한 자산의 공통 헤더다.

## 계층

`DEPLOYEDASSET` 이 부모, `DPA*` 가 자식이다. 모든 `DPA*` 테이블은 `NODEID`
단일 컬럼으로 부모를 참조한다. `DPA*` 에는 `IMPORTSOURCE` 컬럼이 없다.
출처 구분은 부모를 조인해야 한다.

## ASSETCLASS 판별자

`DEPLOYEDASSET.ASSETCLASS` 가 어떤 자식 테이블에 행이 붙는지 결정한다.

| ASSETCLASS | 자식 테이블 |
| --- | --- |
| COMPUTER | DPACOMPUTER, DPAOS, DPASOFTWARE, DPACPU, DPADISK, DPALOGICALDRIVE, DPANETADAPTER, DPATCPIP, DPAMEDIAADAPTER, DPADISPLAY, DPASWSUITE |
| NETDEVICE | DPANETDEVICE |
| NETPRINTER | DPANETPRINTER |

관측 시점 기준 ASSETCLASS 교차 사례와 부모 없는 자식 행은 없었다.

## 카디널리티 판별

자식 테이블의 기본키가 부모와의 카디널리티를 결정한다.

| 기본키 | 카디널리티 | 테이블 |
| --- | --- | --- |
| `NODEID` | 1 : 1 | DPACOMPUTER, DPANETDEVICE, DPANETPRINTER |
| 자체 ID | 1 : N | DPACPU(`CPUID`), DPADISK(`DISKID`), DPADISPLAY(`DISPLAYID`), DPALOGICALDRIVE(`LOGICALDRIVEID`), DPAMEDIAADAPTER(`ADAPTERID`), DPANETADAPTER(`ADAPTERID`), DPAOS(`OSID`), DPASOFTWARE(`SOFTWAREID`), DPASWSUITE(`DPASWSUITEID`), DPATCPIP(`TCPIPID`) |

`NODEID` 가 기본키면 노드당 1행만 가능하다. 자체 ID 가 기본키면 `NODEID` 는
외래키일 뿐이라 노드당 여러 행이 허용된다.

1:N 이면 반드시 자체 ID 가 있다. 역은 일반적으로 성립하지 않는다. 자체 ID 를
두면서 `NODEID` 에 유니크 제약을 걸어 1:1 로 묶을 수 있기 때문이다. 다만 이
14개 테이블에는 그런 경우가 없다. 자체 ID 를 가진 10개 테이블의 유니크 인덱스는
모두 `(자체ID, NODEID)` 형태라 `NODEID` 중복을 막지 않는다.

자체 ID 를 쓰는 테이블에는 대응 시퀀스가 있다. 아래는 관측 당시 기존 수집분과
시퀀스 상태다. Device42 적재는 대부분 원천 PK를 직접 쓰지만 DPATCPIP은 공유 IP의
장비별 행을 구분하기 위해 `DPATCPIPSEQ`를 사용한다.

| 테이블 | ID 범위 | 시퀀스 | START |
| --- | --- | --- | --- |
| DPACPU | 1–57 | `DPACPUSEQ` | 58 |
| DPADISK | 1–144 | `DPADISKSEQ` | 145 |
| DPADISPLAY | 1–52 | `DPADISPLAYSEQ` | 53 |
| DPALOGICALDRIVE | 1–80 | `DPALOGICALDRIVESEQ` | 81 |
| DPAMEDIAADAPTER | 1–39 | `DPAMEDIAADAPTERSEQ` | 40 |
| DPANETADAPTER | 1–61 | `DPANETADAPTERSEQ` | 62 |
| DPAOS | 1–63 | `DPAOSSEQ` | 64 |
| DPASOFTWARE | 1–13031 | `DPASOFTWARESEQ` | 13032 |
| DPASWSUITE | 1–14 | `DPASWSUITESEQ` | 15 |
| DPATCPIP | 1–53 | `DPATCPIPSEQ` | 54 |

기존 수집분의 ID는 노드별 연번이 아니라 테이블 전역 연번이다.

`DPACOMPUTERSEQ`, `DPANETDEVICESEQ`, `DPANETPRINTERSEQ`도 존재하지만 세 테이블은
PK가 `NODEID`라 자기 시퀀스를 쓰지 않는다.

Device42 적재는 아래 표처럼 원천 PK를 Maximo ID로 직접 사용한다. DPATCPIP만
장비–IP 연결에 원천 단일 PK가 없으므로 Maximo 시퀀스를 사용한다.
`DISCOVERY.SOURCE_TARGET_MAP`은 사용하지 않는다.

| 대상 | Maximo ID | Device42 원천 |
| --- | --- | --- |
| `DEPLOYEDASSET`·1:1 자식 | `NODEID` | `view_device_v2.device_pk` |
| `DPACPU` | `CPUID` | `view_part_v1.part_pk` |
| `DPADISK` | `DISKID` | `view_part_v1.part_pk` |
| `DPALOGICALDRIVE` | `LOGICALDRIVEID` | `view_mountpoint_v2.mountpoint_pk` |
| `DPAMEDIAADAPTER` | `ADAPTERID` | `view_part_v1.part_pk` |
| `DPANETADAPTER` | `ADAPTERID` | `view_netport_v1.netport_pk` |
| `DPAOS` | `OSID` | `view_deviceos_v1.deviceos_pk` |
| `DPASOFTWARE` | `SOFTWAREID` | `view_softwareinuse_v1.softwareinuse_pk` |
| `DPATCPIP` | `TCPIPID` | `MAXIMO.DPATCPIPSEQ`. 자연키는 `(device_pk, HOST(ip_address))` |

모든 자식의 `NODEID`는 해당 원천 레코드의 `device_fk`다. 원천이 없는
`DPADISPLAY`와 `DPASWSUITE`는 적재하지 않는다.

재조회: `SYSCAT.SEQUENCES` 에서 `SEQSCHEMA = 'MAXIMO'`.

행 수가 노드 수와 같다고 해서 1:1 인 것은 아니다. 관측 시점에 노드당 1행이었을
뿐일 수 있다. DPATCPIP, DPAMEDIAADAPTER, DPASWSUITE 가 그런 경우다.

재조회: `SYSCAT.INDEXES` 의 `UNIQUERULE IN ('P','U')`, 또는
`MAXATTRIBUTE.PRIMARYKEYCOLSEQ > 0`.

## 자식 테이블 커버리지

| 테이블 | 노드 수 | 행 수 |
| --- | --- | --- |
| DPACOMPUTER | 68 | 68 |
| DPAOS | 61 | 63 |
| DPADISK | 61 | 144 |
| DPASOFTWARE | 53 | 13031 |
| DPATCPIP | 53 | 53 |
| DPACPU | 49 | 57 |
| DPALOGICALDRIVE | 49 | 80 |
| DPANETADAPTER | 39 | 61 |
| DPAMEDIAADAPTER | 39 | 39 |
| DPADISPLAY | 37 | 52 |
| DPANETDEVICE | 34 | 34 |
| DPASWSUITE | 14 | 14 |
| DPANETPRINTER | 4 | 4 |

부분 커버리지가 정상이다. 관측 대상이 없으면 행을 만들지 않는다.

관측 시점 기준 `DPA*` 행은 전부 `IMPORTSOURCE` 가 비어 있는 기존 수집분에
붙어 있다. Device42 적재분(COMPUTER 28, NETDEVICE 2, NETPRINTER 1)에는 자식
행이 없다. 부모만 적재되고 자식 적재는 아직 구현되지 않았다.

기존 수집분 기준으로 `DPACOMPUTER` 만 68/68 로 전건 존재한다. 부모 1건당
자식 1건인 확장 테이블이기 때문이다. 나머지 자식 테이블은 부분이다.

## 키

- `NODEID` 는 Device42 `view_device_v2.device_pk` 다.
- 현행 적재의 MERGE 키는 `NODEID` 다.
  근거: `DeployedAssetWriter.java` `MERGE_DEPLOYED_ASSET_QUERY`
- `SOURCEID` 에는 Device42 `device_pk` 가 들어간다.
