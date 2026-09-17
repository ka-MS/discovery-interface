# 연계 구조 리팩토링 설계

## 목표와 범위

현재 Device42 → Maximo CLI의 동작을 보존하면서 작은 실행 코어, 연계별 조회·매핑,
독립된 타겟 저장으로 분리한다. 단일 Gradle 모듈과 `com.itmsg.device42` 루트는 유지한다.
새 기능, Spring Batch, 의존성 업그레이드, 운영 DB 적재는 범위 밖이다.

## 책임과 의존 방향

| 패키지 | 책임 | 허용 의존 |
| --- | --- | --- |
| `runtime` | 작업 순차 실행, 기존 페이지 범위 반복 | JDK, 로깅 |
| `device42` | 연결과 DOQL/JDBC 실행 기술 | JDK, Spring |
| `maximo` | 타겟 DTO, Writer, MERGE, 정의 로딩·스냅샷 | JDK, Spring |
| `integration/d42maximo` | 수집 SQL·반환 모델·매핑·식별자·Job 조립 | 위 세 영역 |
| `cli` | 기존 인자에서 Job 실행 | runtime |

하위 기술/타겟 영역에서 연계로 역참조하지 않는다. 기능별 코드는 같은 패키지에
모으며, 공통 조회 모델이나 범용 Map 컨텍스트를 만들지 않는다. 인터페이스/상속은
현재 공통 실행 계약에 필요한 만큼만 사용한다.

## 이전 대응

| 기존 | 새 소유자 |
| --- | --- |
| IntegrationJob / JobRunner | runtime / cli |
| 영역별 Job와 Task 루프 | 연계별 Job + runtime TaskSequence |
| Device42ConnectionFactory / Device42DatabaseConfig | device42 |
| MaximoDatabaseConfig / dto.maximo | maximo |
| Asset·Conversion·Software Integrate의 putData와 MERGE | maximo 영역별 Writer |
| 각 Integrate의 조회와 원천 DTO | 연계의 기능별 Query·반환 모델 |
| 각 Integrate의 변환 | 연계의 기능별 Mapper 또는 작은 메서드 |
| CI Writer·DefinitionLoader/Cache | maximo.ci (정의 ID 목록은 호출자가 전달) |
| CI enum·SpecMapper·수집 조건 | integration.d42maximo.ci |
| 관계 enum과 원천 SQL | 연계의 관계 정의 (공통 관계 실행 유지) |
| 실행 클래스에 있던 공유 식별자·제외 조건 | 연계별 작은 규칙 클래스 |

### 전체 실행 흐름 대응

새 기능 경로는 `integration/d42maximo/` 아래다. `X*`는 XImport·XQuery·XMapper를 뜻하며,
Conversion과 NetDevice는 단순 변환을 Import 내부에 둔다. Writer 경로는 `maximo/` 아래다.
원천 모델은 각 기능 폴더에, 타겟 DTO는 Writer와 같은 영역에 있다.

| 기존 실행 클래스 | 새 기능/클래스 | 저장 |
| --- | --- | --- |
| DeployedAssetIntegrate | asset/device/DeployedAsset* | asset/DeployedAssetWriter |
| DpaComputerIntegrate | asset/computer/Computer* | asset/DpaComputerWriter |
| DpaNetDeviceIntegrate | asset/netdevice/NetDeviceImport・Query | asset/DpaNetDeviceWriter |
| DpaNetPrinterIntegrate | asset/netprinter/NetPrinter* | asset/DpaNetPrinterWriter |
| DpaCpuIntegrate | asset/cpu/Cpu* | asset/DpaCpuWriter |
| DpaDiskIntegrate | asset/disk/Disk* | asset/DpaDiskWriter |
| DpaNetAdapterIntegrate | asset/netadapter/NetAdapter* | asset/DpaNetAdapterWriter |
| DpaMediaAdapterIntegrate | asset/mediaadapter/MediaAdapter* | asset/DpaMediaAdapterWriter |
| DpaTcpIpIntegrate | asset/tcpip/TcpIp* | asset/DpaTcpIpWriter |
| DpaLogicalDriveIntegrate | asset/logicaldrive/LogicalDrive* | asset/DpaLogicalDriveWriter |
| DpaOsIntegrate | asset/os/Os* | asset/DpaOsWriter |
| DatabaseInstanceCiIntegrate | ci/databaseinstance/DatabaseInstanceCi* | ci/ActCiWriter |
| DeviceCiIntegrate | ci/device/DeviceCi* | ci/ActCiWriter |
| DiskCiIntegrate | ci/disk/DiskCi* | ci/ActCiWriter |
| FilesystemCiIntegrate | ci/filesystem/FilesystemCi* | ci/ActCiWriter |
| IpCiIntegrate | ci/ip/IpCi* | ci/ActCiWriter |
| OsCiIntegrate | ci/os/OsCi* | ci/ActCiWriter |
| CiRelationJob | ci/relation/CiRelationJob・CiRelationQuery・CiRelationSource | ci/ActCiRelationWriter |
| DpamManufacturerIntegrate | conversion/manufacturer/DpamManufacturerImport・Query | conversion/DpamManufacturerWriter |
| DpamManuVariantIntegrate | conversion/manufacturer/DpamManuVariantImport・Query | conversion/DpamManuVariantWriter |
| DpamOsIntegrate | conversion/os/DpamOsImport・Query | conversion/DpamOsWriter |
| DpamOsVariantIntegrate | conversion/os/DpamOsVariantImport・Query | conversion/DpamOsVariantWriter |
| DpamProcessorIntegrate | conversion/processor/DpamProcessorImport・Query | conversion/DpamProcessorWriter |
| DpamProcVariantIntegrate | conversion/processor/DpamProcVariantImport・Query | conversion/DpamProcVariantWriter |
| DpamAdapterIntegrate | conversion/adapter/DpamAdapterImport・Query | conversion/DpamAdapterWriter |
| DpamAdptVariantIntegrate | conversion/adapter/DpamAdptVariantImport・Query | conversion/DpamAdptVariantWriter |
| TloamSoftwareIntegrate | software/catalog/TloamSoftware* | software/TloamSoftwareWriter |
| DpaSoftwareIntegrate | software/installed/DpaSoftware* | software/DpaSoftwareWriter |

영역별 Job은 해당 영역의 루트에 둔다. 기존 네 Task 인터페이스는 제거하고 구체적인 Import를
메서드 참조/람다로 NamedTask에 조립한다. CI는 run의 지역 스냅샷을 각 람다에 전달한다.
새 흐름의 순서·선행 관계는 연계 Job이 결정하며 코어에 등록 코드를 추가하지 않는다.

## 보존할 동작

- 기존 CLI 이름, 인자 순서, 알 수 없는 이름 무시, CI 관계 단독/중복 호출 의미.
- 기존 작업 순서, 작업 실패 후 계속 실행, CI 공통 준비 실패 전파, 본체 실패 후 관계 실행.
- 각 조회의 COUNT, 정렬, LIMIT/OFFSET, 빈 페이지 이후에도 count 기준 반복.
- SQL 의미와 바인딩 순서·자료형, 식별자·MERGE 키·시퀀스·중복/관계 처리.
- NULL, 단위, 시간대, 페이지별 변경 시각 생성과 건별 변환/저장 실패 범위.
- 본체 성공/스펙 실패의 부분 적재 및 현재 auto-commit 의미. 트랜잭션을 추가하지 않는다.
- 기존 조회/매핑/적재 집계. 적재 건수만으로 새 상태나 exit code를 정의하지 않는다.

## 설계 결정

- 연계 전용 SQL과 원천 모델은 integration에 둔다. D42 문법을 사용해도 수집 범위는 연계 정책이다.
- 페이지 반복과 작업 순차 실행만 추출한다. JobPlan DSL·의존성 DAG·운영 기능은 만들지 않는다.
- 기존 배치 단위 매핑과 오류 경계를 유지한다. 간단한 변환에 전용 인터페이스를 만들지 않는다.
- CI 정의는 매 실행 생성한 읽기 전용 값이며 작업 객체에 실행 상태를 보관하지 않는다.
- SQL text block은 소유 클래스 가까이에 유지한다. SQL 리소스 로더 도입은 하지 않는다.

### 구현에서 확정한 경계

- `DoqlClient`는 연결·Statement·ResultSet의 수명만 관리한다. 기존 Statement/PreparedStatement
  선택을 유지하기 위해 query/preparedQuery 두 메서드를 둔다. SQL·건별 변환·예외 메시지는 Query의 책임이다.
  기존 접속 팩토리의 재시도를 그대로 사용하며 별도 재시도·예외 래핑을 추가하지 않는다.
- `PageLoop`는 lazy Iterable의 offset/limit만 제공한다. Import의 지역 집계·페이지별 변경 시각·예외 경계를
  보존하기 위해 공통 결과 객체나 callback 파이프라인을 만들지 않았다.
- `TaskSequence`는 기존 Job의 Exception 격리만 공통화한다. Error는 삼키지 않는다.
  결과에 사용되지 않던 failures 목록은 제거했으며 상태/exit code 정책을 새로 만들지 않았다.
- `maximo/ci/definition`의 로더는 분류 ID 목록을 받고, 스냅샷은 문자열 ID로 분류를 찾는다.
  `CiClassification`·스펙 enum·CiSpecMapper·SourceTimestamp는 연계의 `ci/mapping`에 둔다.
  시간대 처리와 추가 속성 판정 규칙은 그대로다.
- `ci/selection`의 CiSourceFilter·FilesystemSelection과 `software/mapping/SoftwareIdentity`는
  공유 규칙만 소유한다. Job이 있는 부모 패키지에 두지 않아 세부 패키지 사이에도 순환이 없다.
- Conversion은 네 원천군 manufacturer/os/processor/adapter별로 기준·변형 조회와 원천 모델을 모았다.
  변환을 위한 인터페이스·Mapper bean 8개는 추가하지 않았다.
- NetDevice도 단순 복사와 페이지 단위 시각 생성만 하므로 별도 Mapper를 두지 않았다.
  나머지 Asset 매핑은 단위/기본값/분류/식별자 등의 독립 규칙을 검증하는 경계로 남겼다.
- 관계는 기존 일곱 enum 정의와 한 Query/Job을 유지한다. 별도 관계 Mapper나 관계별 클래스는 없다.
- 로그의 작업 이름은 기존 이름을 유지하고 logger 카테고리는 새 책임 클래스 이름을 따른다.
- 기존 미사용 ViewDeviceV2 모델과 접속 검증 주석 등은 이번 변경과 무관하므로 제거/활성화하지 않았다.

### 기존 실행·실패 경계

| 단계 | 보존한 동작 |
| --- | --- |
| CLI | asset/ci/ci-relation/conversion/software, 입력 순서·중복 실행, 미등록 인자 무시 |
| CI 준비 | 매 실행 한 번 조회, 실패 시 본체/관계 모두 미실행·호출자에게 전파 |
| 본체 작업 | 작업 예외는 다음 작업으로 진행; CI 관계 단계는 계속 실행 |
| 페이지 | COUNT 기반 범위; 빈 페이지도 다음 offset 진행; 조회 예외는 현재 작업 중단 |
| 행 변환 | 기존 유형별 catch 위치 유지; Device 등의 건별 오류는 다음 행 진행 |
| Writer | 기존 건별 executeUpdate·예외 처리; 본체 실패 시 스펙 건너뜀, 스펙 실패 시 본체 유지 |
| 관계 | 정의별 실패 격리; ci-relation 단독 실행은 CI 정의를 조회하지 않음 |

Asset/Conversion/Software 순서는 기존 @Order와 일치한다. CI의 기존 미지정 순서는
기준 커밋의 Spring scanner에서 확인한 DatabaseInstance→Device→Disk→Filesystem→IP→OS로 명시했다.

### 확장 시 변경 위치

새 D42→Maximo 수집 유형은 해당 기능의 Query·모델·필요한 매핑과 연계 Job 조립에 추가한다.
새 Maximo 테이블이 필요할 때만 maximo 영역에 DTO/Writer를 추가한다.
새 타겟은 그 타겟 저장 영역과 별도의 `integration/d42<target>` 조립에 추가한다.
runtime·기존 Maximo Writer·기존 연계 SQL을 수정할 필요가 없으며, 지금 가상의 타겟 구현은 만들지 않았다.

## 검증과 완료 기준

1. CPU Asset·OS CI 시범 전환 후 모든 Asset/CI/관계/Conversion/Software 경로를 이전한다.
2. 기존 테스트의 검증 의미를 유지하고 오류·페이징·조립·패키지 경계 테스트로 보강한다.
3. 기준 커밋과 SQL·바인딩·매핑·실행 동작을 대조한다.
4. 최종 전체 테스트를 실제 실행하고 빌드를 확인한다. H2/Mock은 실제 DB 검증을 대신하지 않는다.
5. 임시 어댑터·옛 중복 경로·금지 의존·패키지 순환이 없어야 한다.
6. 매핑 문서의 구현 참조, README, CLAUDE.md를 최종 구조와 일치시킨다.
7. 진행 기록과 로컬 커밋, 최종 리뷰를 완료한다. push/merge/PR은 하지 않는다.

지속 회귀 검사는 DependencyBoundaryTest·JobCompositionTest·CLI/DoqlClient/TaskSequence/PageLoop 테스트와
이전한 기존 H2/Mock 테스트가 담당한다. 일회성 이전 대조는 `scripts/refactoring/check_baseline.py`로
기준 Git 객체의 SQL·바인딩·매핑·실행 본문, 모델/규칙/설정, 기존 테스트 이름을 검사한다.
구조 치환만 정규화하며 SQL 리터럴·JDBC 실행 방식은 비교 대상이다. 소스 대조는 실제 DB 실행 검증을 대신하지 않는다.
