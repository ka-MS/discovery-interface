# 데이터 매핑

테이블 단위로 정리한다. 문서 한 장이 구현 클래스 하나에 대응한다.

| 문서 | 구현 |
| --- | --- |
| `asset/deployedasset.md` | `integration/asset/DeployedAssetIntegrate.java` |
| `asset/dpacomputer.md` | `integration/asset/DpaComputerIntegrate.java` |
| `asset/dpaos.md` | `integration/asset/DpaOsIntegrate.java` |
| `software/dpasoftware.md` | `integration/software/DpaSoftwareIntegrate.java` |

## 실행 순서

`DEPLOYEDASSET` 이 `NODEID` 를 발번한 뒤에야 자식 테이블을 적재할 수 있다.

1. `DEPLOYEDASSET`
2. `DPACOMPUTER` (COMPUTER 는 부모와 1:1)
3. 나머지 자식 테이블

## ASSETCLASS 라우팅

`DEPLOYEDASSET.ASSETCLASS` 가 적재 대상 자식 테이블을 결정한다.
판별자 구조는 `../knowledge/maximo/deployedasset-model.md` 참조.

| ASSETCLASS | 자식 테이블 |
| --- | --- |
| COMPUTER | DPACOMPUTER, DPAOS, DPASOFTWARE, DPACPU, DPADISK, DPALOGICALDRIVE, DPANETADAPTER, DPATCPIP, DPAMEDIAADAPTER, DPADISPLAY, DPASWSUITE |
| NETDEVICE | DPANETDEVICE |
| NETPRINTER | DPANETPRINTER |

## 진행 현황

`구분` 열 채움 상태 기준이다. `작성 완료` 는 모든 행의 `구분` 이 채워졌다는 뜻이며, `미결` 로 남은 행은 각 문서의 6절에 사유가 있다.

| 테이블 | Device42 원천 | 컬럼 매핑 |
| --- | --- | --- |
| DEPLOYEDASSET | `view_device_v2` | 작성 완료 |
| DPACOMPUTER | `view_device_v2`, `view_part_v1`(RAM) | 작성 완료 |
| DPAOS | `view_deviceos_v1`, `view_os_v1` | 미작성 |
| DPASOFTWARE | `view_softwareinuse_v1`, `view_software_v1` | 미작성 |
| DPACPU | `view_part_v1`(CPU) | 작성 완료 |
| DPADISK | `view_part_v1`(Hard Disk) | 작성 완료 |
| DPALOGICALDRIVE | `view_mountpoint_v1` | 미작성 |
| DPANETADAPTER | `view_netport_v1` | 미작성 |
| DPATCPIP | `view_ipaddress_v1`, `view_subnet_v1` | 미작성 |
| DPAMEDIAADAPTER | `view_part_v1`(GPU) | 미작성 |
| DPANETDEVICE | 원천 미확정 | 미작성 |
| DPANETPRINTER | `view_device_v2`, `view_part_v1`(printer_*) | 미작성 |
| DPADISPLAY | 없음 | 해당 없음 |
| DPASWSUITE | 없음 | 해당 없음 |

`DPANETDEVICE` 의 원천 미확정 사유는 `../open-issues.md` ISSUE-2 참조.

## 원천이 없는 테이블

문서는 만들되 원천 없음과 사유를 기록한다. 문서가 없으면 미조사와
구분되지 않는다.

| 테이블 | 사유 |
| --- | --- |
| DPADISPLAY | 모니터 정보. Device42 에 대응 뷰와 데이터가 없다 |
| DPASWSUITE | 소프트웨어 스위트 묶음. Device42 에 suite 개념이 없다 |
