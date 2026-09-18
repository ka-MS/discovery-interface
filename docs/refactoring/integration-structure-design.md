# 연계 구조 설계

## 현재 목표와 범위

Device42는 고정 원천이며 설정으로 한 타겟의 연계 조립부를 선택한다. 실제 운영 타겟은 Maximo 하나다.
기존 Maximo 수집·변환·저장·실패 의미를 유지하고 타겟 선택만 추가한다.
단일 Gradle 모듈과 `com.itmsg.device42` 루트를 유지한다.

이전 구조 리팩터링의 완료 이력은 [이전 진행 기록](integration-structure-progress.md)에 보존한다.
이번 결정 상세는 [타겟 교체 설계](target-pluggable-design.md),
검증·기준 커밋·단계 이력은 [이번 진행 기록](target-pluggable-progress.md)에 있다.

## 책임과 의존 방향

| 패키지 | 책임 | 허용 내부 의존 |
| --- | --- | --- |
| runtime | IntegrationJob, TaskSequence, PageLoop, TargetModule 계약 | runtime |
| source.device42 | 접속·DoqlClient, D42 조회 SQL·원천 모델·조건의 SQL 표현·연결 사실 | source.device42 |
| target.maximo | 타겟 DTO·Writer·MERGE SQL, CI 정의 조회·스냅샷 | target.maximo |
| pipeline.d42maximo | 수집 정책, Mapper·식별자, Import·Job, 준비·원천 조회·타겟 조립 | runtime, source, target, 자체 pipeline |
| cli | 타겟 ID 검증과 선택된 Job 맵에서 기존 인자 실행 | runtime |

Source/Target은 Pipeline을 역참조하지 않는다. Source에는 Maximo DTO·관계 코드·식별자 상수가 없다.
타겟으로 번역하는 Mapper는 Pipeline에 둔다. 원천 모델은 D42에 충실하며 범용 중립 모델을 만들지 않는다.
기술적 상속·범용 컨텍스트·매핑 DSL·JobPlan 프레임워크는 도입하지 않는다.

## 타겟 선택과 조립

- 설정 키는 `integration.target`, 생략 시 `maximo`다. 환경변수 `INTEGRATION_TARGET`으로도 지정할 수 있다.
- Application은 Pipeline 아래 `TargetModule` 구현 설정만 탐색한다. 각 모듈은 조건부 활성화한다.
- `MaximoTargetModule`은 선택됐을 때만 Maximo 컴포넌트, D42 접근, `MaximoQueries`, DataSource 설정을 가져온다.
- 전역 DataSource 자동 설정은 제외한다. 따라서 다른 타겟은 Maximo 접속정보·Bean·DataSource를 요구하지 않는다.
- `TargetModule.id()`와 `jobs()`만 공통 계약이다. Job 맵 자체가 지원 작업 목록이다.
- `JobRunner`는 활성 모듈이 정확히 하나이고 설정 ID와 같은지 확인한다. 미등록 타겟은 작업 전에 오류다.
- 선택된 Job 맵에서만 CLI 인자를 실행한다. 기존 명령 순서·중복 실행·미등록 인자 무시를 유지한다.
- 각 타겟이 같은 다섯 명령을 제공해야 하는 것은 아니다. CLI는 Maximo 작업 종류를 모른다.

## 원천 조회와 수집 정책

`MaximoSourcePolicy`가 실제 수집 값을 선택하고 `MaximoQueries`가 원천 Query 생성자에 전달한다.
Query에는 Maximo 기본 수집 정책이 없다. `DeviceSelection`과 `FilesystemFilter`는 D42 필드 조건을 SQL로 표현한다.
조건은 COUNT와 PAGE에 동일하게 적용하며 Mapper 사후 필터로 옮기지 않는다.
D42 PK/FK 조인·배열 전개·원천 속성 해석은 Source가 소유한다.

기준정보의 같은 원천 조회를 타겟의 기준/변형 두 Import가 공유한다. 여덟 작업은 모두 남고 순서·실행 횟수도 같다.
카탈로그의 누락값 동치 그룹과 기준정보 보충 이름은 연계 정책으로 전달한다.
이들은 DISTINCT/UNION/정렬/COUNT에 영향을 주므로 SQL에서 처리해야 기존 페이지를 보존할 수 있다.
카탈로그 Query는 누락 동치 그룹을 NULL로 반환하고 최종 UNKNOWN 값은 Mapper가 생성한다.
Printer MAC 대문자 표현도 Mapper가 생성한다.

## CI 관계와 식별자

- `Device42Relation`: 기존 일곱 D42 연결 사실과 SQL. 정책은 Selection으로 전달한다.
- `RelationSource`: 출발·도착 원천 PK 문자열만 반환한다.
- `CiRelationSource`: 연결 사실과 Maximo 관계 코드의 대응, 기존 순서를 유지한다.
- `CiRelationMapper`: 원천 끝점을 타겟 식별자로 변환한다.
- `MaximoCiIdentity`: 여섯 CI 본체 Mapper와 관계 Mapper가 공유하는 식별자 규칙이다.
- PK는 CAST(... AS varchar)로 정렬한다. 각 조회의 고정 접두어를 제거해도 기존 문자열 정렬과 같은 순서다.
- 파일시스템 관계의 배열 모든 쌍, 중복, 방향, 일곱 코드, 관계별 실패 격리를 보존한다.
- 관계별 별도 Job/Writer 클래스는 만들지 않는다.

## 기존 연계 경로 대응

각 Import는 `pipeline.d42maximo`, Query는 `source.device42`, Writer는 `target.maximo` 아래다.
원천 모델은 Query의 기능 패키지, 타겟 DTO는 Writer 패키지, Mapper는 Import의 기능 패키지에 둔다.

| 기존과 같은 실행 흐름 | 기능 패키지 | 원천 조회 | 타겟 저장 |
| --- | --- | --- | --- |
| ComputerImport | asset.computer | ComputerQuery | DpaComputerWriter |
| CpuImport | asset.cpu | CpuQuery | DpaCpuWriter |
| DeployedAssetImport | asset.device | DeviceQuery | DeployedAssetWriter |
| DiskImport | asset.disk | DiskQuery | DpaDiskWriter |
| LogicalDriveImport | asset.logicaldrive | LogicalDriveQuery | DpaLogicalDriveWriter |
| MediaAdapterImport | asset.mediaadapter | MediaAdapterQuery | DpaMediaAdapterWriter |
| NetAdapterImport | asset.netadapter | NetAdapterQuery | DpaNetAdapterWriter |
| NetDeviceImport | asset.netdevice | NetDeviceQuery | DpaNetDeviceWriter |
| NetPrinterImport | asset.netprinter | NetPrinterQuery | DpaNetPrinterWriter |
| OsImport | asset.os | OsQuery | DpaOsWriter |
| TcpIpImport | asset.tcpip | TcpIpQuery | DpaTcpIpWriter |
| DatabaseInstanceCiImport | ci.databaseinstance | DatabaseInstanceCiQuery | ActCiWriter |
| DeviceCiImport | ci.device | DeviceCiQuery | ActCiWriter |
| DiskCiImport | ci.disk | DiskCiQuery | ActCiWriter |
| FilesystemCiImport | ci.filesystem | FilesystemCiQuery | ActCiWriter |
| IpCiImport | ci.ip | IpCiQuery | ActCiWriter |
| OsCiImport | ci.os | OsCiQuery | ActCiWriter |
| DpamAdapterImport | conversion.adapter | AdapterModelsQuery | DpamAdapterWriter |
| DpamAdptVariantImport | conversion.adapter | AdapterModelsQuery | DpamAdptVariantWriter |
| DpamManufacturerImport | conversion.manufacturer | ManufacturerNamesQuery | DpamManufacturerWriter |
| DpamManuVariantImport | conversion.manufacturer | ManufacturerNamesQuery | DpamManuVariantWriter |
| DpamOsImport | conversion.os | OperatingSystemNamesQuery | DpamOsWriter |
| DpamOsVariantImport | conversion.os | OperatingSystemNamesQuery | DpamOsVariantWriter |
| DpamProcessorImport | conversion.processor | ProcessorModelsQuery | DpamProcessorWriter |
| DpamProcVariantImport | conversion.processor | ProcessorModelsQuery | DpamProcVariantWriter |
| TloamSoftwareImport | software.catalog | SoftwareCatalogQuery | TloamSoftwareWriter |
| DpaSoftwareImport | software.installed | InstalledSoftwareQuery | DpaSoftwareWriter |
| CiRelationJob | ci.relation | CiRelationQuery + Device42Relation | ActCiRelationWriter |

기존 `device42` 기술 클래스는 `source.device42`, `maximo`는 `target.maximo`로 이동했다.
`integration.d42maximo`의 Query/원천 모델은 Source로, 나머지 Mapper/Import/Job은 Pipeline으로 이동했다.
단순 변환은 Import 메서드로 유지한다. 미사용 ViewDeviceV2는 원천에 보존하고 임의로 제거하지 않았다.

## 보존한 실행 의미

| 단계 | 동작 |
| --- | --- |
| CLI | asset/ci/ci-relation/conversion/software, 입력 순서·중복 실행, 미등록 인자 무시 |
| CI 준비 | 매 실행 한 번 정의 로딩, 실패 시 본체/관계 미실행 및 호출자에게 전파 |
| 본체 작업 | 작업 예외 후 다음 작업 계속; 본체 실패 여부와 무관하게 관계 단계 실행 |
| 페이지 | COUNT 기반 범위, 빈 페이지도 다음 offset 진행, 조회 예외는 현재 작업 중단 |
| 행 변환 | 기존 유형별 catch 위치 유지 |
| Writer | 기존 바인딩·자료형·MERGE·건별 실행 유지; 본체 실패 시 스펙 생략, 스펙 실패 시 본체 유지 |
| 관계 | 정의별 실패 격리; 단독 ci-relation은 CI 정의를 조회하지 않음 |

Asset/Conversion/Software 순서는 기존과 같다. CI는 DatabaseInstance→Device→Disk→Filesystem→IP→OS→관계다.
NULL·단위·시간대·페이지별 변경 시각, retry·auto-commit·부분 적재·로그 집계 의미를 유지한다.
트랜잭션·재시도·새 상태/exit code 정책을 추가하지 않는다.

## 새 타겟 추가 절차

1. `target.<name>`에 실제 대상의 DTO·Writer·필요한 설정을 구현한다.
2. `pipeline.d42<name>`에 기존 D42 원천 모델을 받는 Mapper와 해당 타겟 작업을 조립한다.
3. 해당 Pipeline 안에 `TargetModule`을 구현한 조건부 Configuration을 추가한다.
   `integration.target=<name>`일 때만 활성화하고 자기 컴포넌트·접속·필요한 Query를 가져온다.
4. 설정을 바꾸고 그 모듈이 제공하는 작업 이름을 기존 CLI 인자로 전달한다.

기존 조회로 충분하다면 CLI/runtime/Maximo 구현 변경이 없다.
새 D42 데이터가 필요하면 Source 조회를 확장하는 것은 정상적인 확장이다.
등록하지 않은 시스템을 설정 한 줄만으로 자동 지원한다는 뜻은 아니다.
테스트 전용 `pipeline.d42test.AlternativeTarget`이 동일한 탐색·설정 경로의 예다. 운영 JAR에는 포함하지 않는다.

## 검증과 완료 기준

- 기존 테스트 의미 유지, 전체 test/bootJar 실제 실행.
- `SourceSqlParityTest`: 기준 커밋의 27개 조회 경로와 7개 관계 COUNT/PAGE 동결 SQL 대조.
- `SelectionEquivalenceTest`: 장비 조건 10,800 조합 및 파일시스템 조건 동등성.
- `RelationMappingEquivalenceTest`: 일곱 관계의 코드·식별자·정렬·중복·NULL·페이지 결과.
- `ProjectionEquivalenceTest`: 카탈로그 기본값 동치 그룹·COUNT·정렬·페이지 및 Printer MAC 표현.
- `TargetSelectionTest`: 기본/명시/잘못된 타겟과 실제 모듈 탐색을 통한 대체 타겟의 원천 재사용·비선택 타겟 비활성화.
- `DependencyBoundaryTest`, `MappingDocumentationTest`: 금지 의존·패키지 순환·구현 링크와 경로 누락 검사.
- `python3 scripts/refactoring/check_target_baseline.py --artifact`: 기존 본문·테스트 보존과 JAR 경로 대조.

이 검증들은 H2/Mockito 및 소스·산출물 검사다. 실제 DOQL/DB2 엔진·운영 적재는 이번 작업에서 수행하지 않는다.
