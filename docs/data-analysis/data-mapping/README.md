# 데이터 매핑

테이블 단위로 정리한다. 문서 한 장이 구현 클래스 하나에 대응한다.

| 문서 | 구현 |
| --- | --- |
| `asset/deployedasset.md` | `integration/asset/DeployedAssetIntegrate.java` |
| `asset/dpacomputer.md` | `integration/asset/DpaComputerIntegrate.java` |
| `asset/dpaos.md` | `integration/asset/DpaOsIntegrate.java` |
| `asset/dpacpu.md` | `integration/asset/DpaCpuIntegrate.java` |
| `asset/dpadisk.md` | `integration/asset/DpaDiskIntegrate.java` |
| `asset/dpalogicaldrive.md` | `integration/asset/DpaLogicalDriveIntegrate.java` |
| `asset/dpanetadapter.md` | `integration/asset/DpaNetAdapterIntegrate.java` |
| `asset/dpatcpip.md` | `integration/asset/DpaTcpIpIntegrate.java` |
| `asset/dpamediaadapter.md` | `integration/asset/DpaMediaAdapterIntegrate.java` |
| `asset/dpadisplay.md` | `integration/asset/DpaDisplayIntegrate.java` |
| `asset/dpaswsuite.md` | `integration/asset/DpaSwSuiteIntegrate.java` |
| `asset/dpanetdevice.md` | `integration/asset/DpaNetDeviceIntegrate.java` |
| `asset/dpanetprinter.md` | `integration/asset/DpaNetPrinterIntegrate.java` |
| `software/dpasoftware.md` | `integration/software/DpaSoftwareIntegrate.java` |

현재 실제 적재 구현은 `DeployedAssetIntegrate`와 `DpaComputerIntegrate`뿐이다.
`DpaOsIntegrate`와 `DpaSoftwareIntegrate`는 자리표시자이며 나머지 자식 구현
클래스는 아직 없다. 표는 문서 한 장이 대응할 구현 클래스를 가리킨다.

## 실행 순서

`DEPLOYEDASSET` 이 `NODEID` 를 발번한 뒤에야 자식 테이블을 적재할 수 있다.

1. `DEPLOYEDASSET`
2. `DPACOMPUTER` (COMPUTER 는 부모와 1:1)
3. 나머지 자식 테이블

## ASSETCLASS 라우팅

`DEPLOYEDASSET.ASSETCLASS` 가 적재 대상 자식 테이블을 결정한다.
판별자 구조는 `../knowledge/maximo/deployedasset-model.md` 참조.
PDU 는 `DEPLOYEDASSET` 과 모든 자식의 조회 대상에서 제외한다.

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
| DPAOS | `view_deviceos_v1`, `view_os_v1` | 작성 완료 |
| DPASOFTWARE | `view_softwareinuse_v1`, `view_software_v1` | 작성 완료 |
| DPACPU | `view_part_v1`(CPU) | 작성 완료 |
| DPADISK | `view_part_v1`(Hard Disk) | 작성 완료 |
| DPALOGICALDRIVE | `view_mountpoint_v1` | 작성 완료 |
| DPANETADAPTER | `view_netport_v1` | 작성 완료 |
| DPATCPIP | `view_ipaddress_v1`, `view_subnet_v1` | 작성 완료 |
| DPAMEDIAADAPTER | `view_part_v1`(GPU) | 작성 완료 |
| DPANETDEVICE | `view_device_v2`(`physical`), `view_netport_v1`, `view_ipaddress_v1` | 작성 완료 |
| DPANETPRINTER | `view_device_v2`, `view_netport_v1`, `view_ipaddress_v1`, `view_part_v1`(printer_input) | 작성 완료 |
| DPADISPLAY | 없음 | 작성 완료 (원천 없음) |
| DPASWSUITE | 없음 | 작성 완료 (원천 없음) |

`DPANETDEVICE` 는 `physical` 레코드를 적재 대상으로 삼고, 분리된 `cluster` 의
네트워크 정보는 `view_netport_v1.second_device_fk` 로 연결해 가져온다. 이름
기반 추정 조인이 아니라 데이터에 있는 FK 다. `../open-issues.md` ISSUE-2 참조.

## 원천이 없는 테이블

문서는 만들되 원천 없음과 사유를 기록한다. 문서가 없으면 미조사와
구분되지 않는다.

| 테이블 | 사유 |
| --- | --- |
| DPADISPLAY | 모니터 정보. Device42 에 대응 뷰와 데이터가 없다 |
| DPASWSUITE | 소프트웨어 스위트 묶음. Device42 에 suite 뷰가 없고 `view_software_v1` 에도 묶음 컬럼이 없다 |
