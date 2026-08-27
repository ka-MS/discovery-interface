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
`DPANETADAPTER.ADAPTERID`, `DPAMEDIAADAPTER.ADAPTERID` 는 원천 pk 를 그대로
쓸 수 없다. Device42의 `part_pk`, `mountpoint_pk`, `netport_pk` 는 재수집 시
바뀌기 때문이다.

각 타겟 테이블 내 전역 연번과 노드 내 연번 중 어떤 범위로 채번할지, 재실행 시
동일 원천 행의 키를 어떻게 유지할지 구현 규칙을 정해야 한다.

## 처리 완료

| 항목 | 결론 |
| --- | --- |
| 가상 장비 vendor 없음 | 이슈 아님. `MAXATTRIBUTE.DEFAULTVALUE` 가 `UNKNOWN` 으로 정의되어 있어 정상 결과다 |
