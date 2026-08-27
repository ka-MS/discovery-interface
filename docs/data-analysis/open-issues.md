# 미결 사항

판단과 의견은 이 문서에만 둔다. 다른 문서는 관측된 값과 매핑 규칙만 기술한다.

## ISSUE-1 SOURCEID 가 서버 간 불일치

**상태:** 정책 정의 대기. 기록만 한다.

`DEPLOYEDASSET` 의 MERGE 키는 `(SOURCEID, IMPORTSOURCE)` 이고 `SOURCEID` 에는
Device42 `device_pk` 가 들어간다. `device_pk` 는 수집 서버가 다르거나
재수집하면 값이 바뀐다. 동일 장비가 서버에 따라 다른 `SOURCEID` 로 적재된다.

동일 장비 확인 근거는 `serial_no` 와 `uuid` 다. 두 값은 서버가 달라도 같다.

`DEPLOYEDASSET.TLOAMNRSUUID` 에 Device42 `uuid` 가 이미 적재된다. 다만
`uuid` 는 전건 존재하지 않는다. 충전율은
`knowledge/device42/servers.md` 의 식별자 충전율 표를 참조한다.

## ISSUE-2 스위치가 두 레코드로 분리됨

**상태:** 정책 확정. DPANETDEVICE 매핑에 반영.

네트워크 장비가 Device42 에서 두 레코드로 나뉜다.

| 레코드 | 보유 | 미보유 |
| --- | --- | --- |
| `type = 'cluster'` | netport, ipaddress | serial_no |
| `type = 'physical'`, 이름에 ` - Switch N` 접미 | serial_no | netport, ipaddress |

현행 필터는 `type IN ('virtual','physical')` 이므로 physical 쪽만 적재된다.
포트와 IP 는 cluster 쪽에 있어 `DPANETDEVICE.NETWORKADDRESS` 와
`NETMACADDR` 의 원천이 없다.

두 레코드를 잇는 FK 는 없다. `host_chassis_device_fk`,
`virtual_host_device_fk`, `vm_manager_device_fk`, `chassisslot_fk` 가 모두
비어 있다. 이름 접미사 규칙 외에 연결 수단이 확인되지 않았다.

확정 정책은 다음과 같다.

- 현재 적재 대상인 `type = 'physical'` 레코드만 DPANETDEVICE 원천으로 사용한다.
- 이름 접미사에 의존한 `cluster` 추정 조인은 하지 않는다.
- 실제 IP·MAC·포트는 `cluster` 레코드에 있으나 연결 FK가 없어 추적할 수 없으므로
  `DPANETDEVICE.NETWORKADDRESS`, `NETMACADDR`는 `원천없음`으로 처리한다.

## ISSUE-3 PDU 가 COMPUTER 로 분류됨

**상태:** 수집 정책에 반영.

`physicalsubtype = 'PDU'` 인 장비가 현행 판정 순서에서 3번 분기로 떨어져
COMPUTER 가 된다. Maximo 에 PDU 용 ASSETCLASS 와 DPA 테이블이 없다.
수집 대상에서 제외하는 방향으로 정책에 반영한다.

판정 순서는 `knowledge/device42/device-types.md` 참조.

## ISSUE-4 자식 태스크의 조회 조건이 부모와 다르다

**상태:** 기록만 한다.

`DeployedAssetIntegrate` 와 `DpaComputerIntegrate` 의 원천 조회 조건이 서로 다르다.

| 태스크 | 조건 |
| --- | --- |
| DeployedAssetIntegrate | `type IN ('virtual','physical')` AND `virtualsubtype_id IS NULL OR <> 15` |
| DpaComputerIntegrate | `network_device` 거짓 AND `physicalsubtype <> 'Network Printer'` AND `type` 이 `unknown` 아님 |

두 조건의 차이를 관측 기준으로 대조한 결과다.

| 부모 | 자식 | 대상 | 건수 |
| --- | --- | --- | --- |
| 제외 | 포함 | virtual / Docker Container | 12 |
| 포함 | 제외 | physical / Generic (network_device) | 2 |
| 포함 | 제외 | physical / Network Printer | 1 |

`포함/제외` 3건은 정상이다. NETDEVICE·NETPRINTER 로 분류된 자산이라 DPACOMPUTER 대상이 아니다.

`제외/포함` 12건이 문제다. 부모가 적재하지 않은 Docker Container 를 자식이 대상으로 잡아, 매 실행마다 교차키 조회에 실패하고 경고 로그만 남긴다. 데이터가 잘못 들어가지는 않지만 불필요한 조회와 로그가 발생한다.

자식 태스크를 새로 만들 때 부모와 같은 필터를 쓰도록 맞춰야 한다.

## ISSUE-5 DPA 자식 대리키 채번 규칙 미정

**상태:** 구현 규칙 정의 대기.

`DPACPU.CPUID`, `DPADISK.DISKID`, `DPALOGICALDRIVE.LOGICALDRIVEID`,
`DPANETADAPTER.ADAPTERID`, `DPAMEDIAADAPTER.ADAPTERID`, `DPAOS.OSID`,
`DPATCPIP.TCPIPID` 는 원천 pk 를 그대로 쓸 수 없다. Device42의 `part_pk`,
`mountpoint_pk`, `netport_pk`, `deviceos_pk`, `ipaddress_pk` 는 재수집 시
바뀌기 때문이다.

각 타겟 테이블 내 전역 연번과 노드 내 연번 중 어떤 범위로 채번할지, 재실행 시
동일 원천 행의 키를 어떻게 유지할지 구현 규칙을 정해야 한다.

`DPANETPRINTER` 는 해당 없다. 이 테이블만 기본키가 `NODEID` 라 대리키가 없다.

## ISSUE-6 1:1 자식에 원천이 복수일 때의 선택 규칙 미정

**상태:** 구현 규칙 정의 대기.

`DPANETPRINTER` 는 기본키가 `NODEID` 라 노드당 1행만 가능하다. 그런데 원천인
프린터 장비는 포트와 IP 를 여러 개 가질 수 있다.

관측 대상 1장비는 포트 2건 중 MAC 보유 1건, IP 1건이라 규칙 없이도 값이
하나로 정해진다. `hwaddress` 가 빈 포트(`Loopback Interface`)를 제외하는
조건까지가 현재 문서화된 규칙이다.

MAC 또는 IP 가 2건 이상인 프린터가 들어오면 `NETMACADDR` 과
`NETWORKADDRESS` 에 어느 값을 쓸지 정해야 한다. 원천 pk 순서에 의존하는
선택은 쓸 수 없다.

`DPATCPIP` 는 이 문제가 없다. `(TCPIPID, NODEID)` 유일 인덱스와 비유일
`NODEID` 인덱스라 IP 1건당 1행을 적재하면 된다.

## ISSUE-5 1:N 자식 테이블의 MERGE 매칭 키

**상태:** 정책 정의 대기.

자체 ID 를 쓰는 10개 테이블은 ID 를 시퀀스로 발번한다. 발번은 해결됐지만
재실행 멱등성에는 같은 원천 행을 다시 찾아낼 매칭 키가 따로 있어야 한다.
없으면 실행할 때마다 행이 늘어난다.

`DEPLOYEDASSET` 이 참고 형태다. `(SOURCEID, IMPORTSOURCE)` 로 매칭하고
`NOT MATCHED` 분기에서만 시퀀스를 호출한다.

### 자연키가 이미 있는 테이블

별도 조치가 필요 없다.

| 테이블 | 자연키 후보 |
| --- | --- |
| DPATCPIP | `TCPIPADDRESS` |
| DPALOGICALDRIVE | `MOUNT` |
| DPANETADAPTER | `NETMACADDR1` |
| DPASOFTWARE | `TLOAMSOFTWAREID` (기존 수집분 13031/13031 채움) |
| DPAOS | `NODEID` 단독으로 충분한지 확인 필요 |

### 자연키가 없는 테이블

DPACPU, DPADISK, DPAMEDIAADAPTER, DPADISPLAY.

`SERIALNUMBER` 를 매칭 키 컨테이너로 쓰는 방안이 있다. 근거는 기존 수집분의
`DPACPU` 다. 57/57 이 `Source ID: <n>` 형식이고 값이 전부 상이하다.

다만 전례는 `DPACPU` 하나뿐이다. 같은 컬럼을 가진 다른 7개 테이블은 전건
NULL 이다. `DPALOGICALDRIVE` 와 `DPATCPIP` 에는 컬럼 자체가 없다.

컨테이너를 정해도 담는 값이 불안정하면 문제가 남는다. Device42 `part_pk` 를
넣으면 ISSUE-1 을 그대로 물려받는다. 재수집 시 값이 바뀌어 매칭이 깨진다.
원천의 안정적 값을 직렬화해 넣어야 한다.

| 테이블 | 안정값 후보 | 실측 |
| --- | --- | --- |
| DPACPU | `view_part_v1.slot` | 43/43 보유, `(device_fk, slot)` 43/43 유일 |
| DPADISK | 없음 | `slot` 0/6, `serial_no` 1/6 |
| DPAMEDIAADAPTER | 미조사 | |
| DPADISPLAY | 원천 없음 | |

`DPADISK` 가 미해결이다. `(NODEID, MAKEMODEL)` 은 관측 6건에서 유일하지만
모델명이 `sda 300 GB` 처럼 용량만 담는 경우가 있어 같은 디스크를 여러 개 단
장비에서 충돌한다.

`SERIALNUMBER` 는 한글명이 "일련 번호" 다. 매칭 키를 넣으면 의미가 어긋난다.
전용 컬럼을 쓸지, 기존 `DPACPU` 관례를 따를지 정해야 한다.

## 처리 완료

| 항목 | 결론 |
| --- | --- |
| 가상 장비 vendor 없음 | 이슈 아님. `MAXATTRIBUTE.DEFAULTVALUE` 가 `UNKNOWN` 으로 정의되어 있어 정상 결과다 |
