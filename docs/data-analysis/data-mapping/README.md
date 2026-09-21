# 데이터 매핑

기본은 테이블 단위이며 문서 한 장이 해당 연계의 조회·매핑과 타겟 Writer에 대응한다.
Query·원천 모델은 source/device42, Mapper·Import·수집 정책은 pipeline/d42maximo, 저장 SQL·DTO는 target/maximo가 소유한다.
현재 구현을 기준으로 작성한 **제출용 명세**이며 후속 가이드의 근거다. 코드 대조 기준과 검증 범위는 [정합성 점검 기록](documentation-audit.md)에 있다.
관측 날짜가 붙은 건수·메타데이터는 당시 DB 스냅샷이며 현재 운영 상태를 보증하지 않는다.
`원천없음`·`미결`은 SQL의 NULL 바인딩과 같은 뜻이 아니다. 각 문서의 실제 저장 SQL에서 INSERT/UPDATE 포함 여부를 확인한다.

CI는 [문서 예외 규칙](../README.md#ci-매핑-문서-예외)에 따라 공통 Target 규약과 유형별 매핑을 분리한다.

| 문서 | 구현 |
| --- | --- |
| `asset/deployedasset.md` | `source/device42/asset/device/` · `pipeline/d42maximo/asset/device/` · `target/maximo/asset/DeployedAssetWriter.java` |
| `asset/dpacomputer.md` | `source/device42/asset/computer/` · `pipeline/d42maximo/asset/computer/` · `target/maximo/asset/DpaComputerWriter.java` |
| `asset/dpaos.md` | `source/device42/asset/os/` · `pipeline/d42maximo/asset/os/` · `target/maximo/asset/DpaOsWriter.java` |
| `asset/dpacpu.md` | `source/device42/asset/cpu/` · `pipeline/d42maximo/asset/cpu/` · `target/maximo/asset/DpaCpuWriter.java` |
| `asset/dpadisk.md` | `source/device42/asset/disk/` · `pipeline/d42maximo/asset/disk/` · `target/maximo/asset/DpaDiskWriter.java` |
| `asset/dpalogicaldrive.md` | `source/device42/asset/logicaldrive/` · `pipeline/d42maximo/asset/logicaldrive/` · `target/maximo/asset/DpaLogicalDriveWriter.java` |
| `asset/dpanetadapter.md` | `source/device42/asset/netadapter/` · `pipeline/d42maximo/asset/netadapter/` · `target/maximo/asset/DpaNetAdapterWriter.java` |
| `asset/dpatcpip.md` | `source/device42/asset/tcpip/` · `pipeline/d42maximo/asset/tcpip/` · `target/maximo/asset/DpaTcpIpWriter.java` |
| `asset/dpamediaadapter.md` | `source/device42/asset/mediaadapter/` · `pipeline/d42maximo/asset/mediaadapter/` · `target/maximo/asset/DpaMediaAdapterWriter.java` |
| `asset/dpadisplay.md` | 미구현 (원천 없음) |
| `asset/dpaswsuite.md` | 미구현 (원천 없음) |
| `asset/dpanetdevice.md` | `source/device42/asset/netdevice/` · `pipeline/d42maximo/asset/netdevice/` · `target/maximo/asset/DpaNetDeviceWriter.java` |
| `asset/dpanetprinter.md` | `source/device42/asset/netprinter/` · `pipeline/d42maximo/asset/netprinter/` · `target/maximo/asset/DpaNetPrinterWriter.java` |
| `software/tloamsoftware.md` | `source/device42/software/catalog/` · `pipeline/d42maximo/software/catalog/` · `target/maximo/software/TloamSoftwareWriter.java` |
| `software/dpasoftware.md` | `source/device42/software/installed/` · `pipeline/d42maximo/software/installed/` · `target/maximo/software/DpaSoftwareWriter.java` |

CI는 [ci/README.md](ci/README.md)에서 시작한다. `ACTCI`, `ACTCISPEC`,
`ACTCIRELATION`의 Target 구조와 분류·속성 템플릿 조사는 완료했다.
CI 전체 [분류·스펙](ci/classstructure.md)과 [관계도·연결 SQL](ci/relations.md)을 먼저 확인한다.
Computer·VM·Switch·Network Cluster의 DPA와 독립된 수집·본체·스펙 매핑 및 SQL은 [Device](ci/types/device.md)에 있다. 저장 구현·D42 양 서버 검증과 [Switch·Printer 기준정보 설계](../design/ci/device-reference-data.md)는 완료했으며, MAS UI 적용과 실제 승격·UI 검증은 남아 있다.
DB·DB Instance의 전체 원천 컬럼별 사용처와 실제 SQL은 `ci/types/`에 있다.
DB Instance의 현재 구현·분류·관계·검증 범위는 [유형 매핑](ci/types/database-instance.md)을 따른다.
독립 Database 등 별도 미결은 각 유형 문서와 open-issues에서 구분한다.

## 변환 데이터

자식 테이블이 Maximo UI 에 보이려면 변환 변형에 값이 등록되어 있어야 한다.
뷰가 INNER 조인하기 때문이다. `conversion/README.md` 참조.

현재 실제 적재 구현은 `DeployedAssetImport`, `ComputerImport`,
`NetDeviceImport`, `NetPrinterImport`, `CpuImport`,
`OsImport`, `DiskImport`, `LogicalDriveImport`,
`NetAdapterImport`, `MediaAdapterImport`, `TcpIpImport`,
`TloamSoftwareImport`, `DpaSoftwareImport`다. 나머지 자식 구현 클래스는
아직 없다. 표는 현재 기능 패키지와 저장 구현을 가리킨다.

## 실행 순서

Device42 PK를 DPA 행의 Maximo ID로 직접 사용한다. `DEPLOYEDASSET.NODEID`는
`device_pk`다. 일반적인 1:N 자식(CPU·Disk·LogicalDrive·Adapter)은 원천 레코드 PK를 사용한다.
예외로 [DPATCPIP](asset/dpatcpip.md)은 `(NODEID,TCPIPADDRESS)`로 MERGE하고 `TCPIPID`를 시퀀스로 채번한다.
[DPASOFTWARE](software/dpasoftware.md)의 키·식별자는 해당 문서의 Writer 규약을 따른다.
`DISCOVERY.SOURCE_TARGET_MAP`은 사용하지 않는다. 전역 변환 데이터와
`TLOAMSOFTWARE`의 신규 ID는 각 Maximo 시퀀스로 발번한다.

1. `DEPLOYEDASSET`
2. `DPACOMPUTER` (COMPUTER 는 부모와 1:1)
3. 나머지 자식 테이블

자식의 `NODEID`에는 원천 `device_fk`를 직접 넣는다. 부모 조회는 하지 않지만
참조 대상 행을 먼저 만들기 위해 실행 순서는 유지한다.

운영 권장 호출 순서는 `conversion → asset → ci → software`다. CLI는 받은 인자 순서대로만 실행하며 자동으로 의존 작업을 추가하지 않는다. `software` 잡 안에서는
`TLOAMSOFTWARE → DPASOFTWARE` 순서로 적재한다.

## ASSETCLASS 라우팅

`DEPLOYEDASSET.ASSETCLASS` 가 적재 대상 자식 테이블을 결정한다.
판별자 구조는 `../knowledge/maximo/deployedasset-model.md` 참조.
PDU는 `DEPLOYEDASSET`과 COMPUTER 범위 조회에서 제외한다. 다만 `DPANETDEVICE`의 현재
`ASSET_NETWORK` 조건은 physical + network_device=true만 검사하며 PDU 제외식이 없다.
모든 자식이 부모와 완전히 같은 필터를 쓴다고 가정하지 않는다. 실제 조건은 각 Query SQL을 따른다.

| ASSETCLASS | 자식 테이블 |
| --- | --- |
| COMPUTER | DPACOMPUTER, DPAOS, DPASOFTWARE, DPACPU, DPADISK, DPALOGICALDRIVE, DPANETADAPTER, DPATCPIP, DPAMEDIAADAPTER; DPADISPLAY·DPASWSUITE는 구조상 자식이나 미구현 |
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
| TLOAMSOFTWARE | `view_softwareinuse_v1`, `view_software_v1`, `view_vendor_v1` | 작성 완료 |
| DPACPU | `view_part_v1`(CPU) | 작성 완료 |
| DPADISK | `view_part_v1`(Hard Disk) | 작성 완료 |
| DPALOGICALDRIVE | `view_mountpoint_v2` | 작성 완료 |
| DPANETADAPTER | `view_netport_v1` | 작성 완료 |
| DPATCPIP | `view_ipaddress_v2`, `view_subnet_v1` | 작성 완료 |
| DPAMEDIAADAPTER | `view_part_v1`(GPU) | 작성 완료 |
| DPANETDEVICE | `view_device_v2`(`physical`), `view_netport_v1`, `view_ipaddress_v2` | 작성 완료 |
| DPANETPRINTER | `view_device_v2`, `view_netport_v1`, `view_ipaddress_v2`, `view_part_v1`(printer_input) | 작성 완료 |
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
