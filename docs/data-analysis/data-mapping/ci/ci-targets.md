# CI 대상

Device42에서 독립 Actual CI로 적재할 개체의 범위를 정한다.

> 관측 2026-09-04 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35

수치는 `.68 / .35` 순이다.

DB·DB Instance의 유형별 매핑 정본은 [Database](types/database.md),
[Database Instance](types/database-instance.md)에 있다. 이 문서는 원천 후보와 관측 근거를 유지한다.

## 1. 판정 단위

View가 아니라 개체를 판정한다.

| 분류 | 의미 | Maximo Target |
| --- | --- | --- |
| 본체 | 독립 CI 후보의 대표 행 | `ACTCI` |
| 속성 | 본체의 분류별 상세값 | `ACTCISPEC` |
| 관계 | 두 CI 사이의 연결 | `ACTCIRELATION` |
| 비대상 | 이력, 기준정보, 중복 표현 | 적재하지 않음 |

본체 후보는 독립 PK와 대표정보를 가진 View에서 시작한다. 독립 PK가 있어도 다른
본체와 같은 개체를 표현하면 별도 `ACTCI`로 만들지 않는다.

## 2. 본체 후보

18개 View가 양쪽 서버에서 모두 조회됐다. 각 View의 PK는 전건 NOT NULL이며
View 안에서 유일하다.

| 후보 유형 | 본체 후보 Source | 행 `.68 / .35` | 연결 또는 상세 Source | 현재 판정 |
| --- | --- | ---: | --- | --- |
| Application Component | `view_appcomp_v1` | 34 / 6 | Device 또는 Resource FK | 본체 후보 |
| Application Group | `view_applicationgroup_v2` | 32 / 2 | 구성항목·관계 View | 본체 후보. 상태 필터 미결 |
| Business Service | `view_businessservice_v2` | 5 / 0 | 구성요소·연결 View | 본체 후보 |
| Cloud Infrastructure | `view_cloudinfrastructure_v2` | 1 / 1 | Resource, VRF | 본체 후보 |
| Cloud Instance | `view_cloudinstance_v1` | 8 / 58 | `view_device_v2` | Device와 1:1. 별도 CI 여부 미결 |
| Database | `view_resource_v2` | 10 / 0 | `view_database_v2` | Resource의 전용 상세 |
| Database Instance | `view_databaseinstance_v2` | 1 / 0 | `view_resource_v2`, `view_appcomp_v1` | 독립 CI; 동일 Resource는 중복 적재하지 않음 |
| Device | `view_device_v2` | 100 / 85 | Device 상세·관계 View | 본체 후보 |
| Kubernetes Cluster | `view_resource_v2` | 1 / 2 | `view_k8scluster_v2` | Resource의 전용 상세 |
| Kubernetes Deployment | `view_resource_v2` | 24 / 200 | `view_k8sdeployment_v2` | Resource의 전용 상세 |
| Kubernetes Node | `view_resource_v2` | 4 / 5 | `view_k8snode_v2` | Resource의 전용 상세 |
| Kubernetes Service | `view_resource_v2` | 41 / 237 | `view_k8sservice_v2` | Resource의 전용 상세 |
| Resource | `view_resource_v2` | 820 / 4,472 | 유형별 전용 View | 유형 필터가 필요한 본체 후보 |
| Service Instance | `view_serviceinstance_v2` | 8,545 / 2,025 | Device, Service | 본체 후보. 적재 단위 미결 |
| Storage Array | `view_resource_v2` | 0 / 0 | `view_storagearray_v2` | 양쪽 표본 없음 |
| Subnet | `view_subnet_v1` | 33 / 25 | VLAN, VRF, 부모 Subnet | 본체 후보 |
| VLAN | `view_vlan_v1` | 11 / 11 | Subnet, Switch, Netport | 본체 후보 |
| VRF | `view_vrfgroup_v1` | 3 / 4 | Cloud Infrastructure, Subnet | 본체 후보 |

`본체 후보`는 적재 확정이 아니다. 포함 범위는 `../../open-issues.md` ISSUE-8에서
결정한다.

## 3. 중복 표현

### Resource와 전용 View

다음 전용 View는 `view_resource_v2`와 식별값이 전건 일치한다. 별도 CI가 아니라
같은 Resource의 상세 표현이다.

PK로 조인한 동일 행에서 이름·identifier 일치를 재검증했다. 이름만으로 조인한 결과가 아니다.

| 전용 View | 일치 기준 | 일치 행 `.68 / .35` |
| --- | --- | ---: |
| `view_database_v2` | PK, 이름 | 10 / 0 |
| `view_k8scluster_v2` | PK, identifier, 이름 | 1 / 2 |
| `view_k8sdeployment_v2` | PK, identifier, 이름 | 24 / 200 |
| `view_k8snode_v2` | PK, identifier, 이름 | 4 / 5 |
| `view_k8sservice_v2` | PK, identifier, 이름 | 41 / 237 |

`view_resource_v2`를 본체 식별 Source로 사용하고 전용 View를 유형별 속성 Source로
사용한다.

### Device와 Cloud Instance

Cloud Instance는 `.68` 8행, `.35` 58행이며 모든 행의 `device_fk`가 실제 Device와
일치한다. 서버별 Cloud Instance 수와 서로 다른 Device 수가 같다. 다만
`cloudinstance_pk`와 `device_pk`, 이름은 전건 동일하지 않다.

별도 CI로 만들지 Device 속성으로 둘지는 미결이다.

### Application Component와 Database Instance

Database Instance는 `.68`에 1행, `.35`에 0행이다. `.68`의 1행은
`appcomp_fk`로 Application Component와 연결되지만 PK와 이름은 같지 않다.

DB Instance는 독립 CI로 매핑한다. 같은 PK·이름의 Resource가 1건 있으며 동일 본체다.
연결된 Application Component 자체의 포함·중복 적재 처리는 ISSUE-8이다.

## 4. 관계 커버리지

| 관계 | Source `.68 / .35` | FK 일치 `.68 / .35` |
| --- | ---: | ---: |
| Database → Database Instance | 10 / 0 | 9 / 0 |
| Database Instance → Application Component | 1 / 0 | 1 / 0 |
| Database Instance → Application Component → Device | 1 / 0 | 0 / 0 |
| Application Component → Device | 34 / 6 | 33 / 6 |
| Application Component → Resource | 34 / 6 | 0 / 0 |
| Cloud Instance → Device | 8 / 58 | 8 / 58 |
| Kubernetes Deployment → Cluster | 24 / 200 | 24 / 200 |
| Kubernetes Node → Cluster | 4 / 5 | 4 / 5 |
| Kubernetes Service → Cluster | 41 / 237 | 41 / 237 |
| Resource → Cloud Infrastructure | 820 / 4,472 | 56 / 55 |
| Resource → Root Resource | 820 / 4,472 | 658 / 4,207 |
| Service Instance → Device | 8,545 / 2,025 | 8,511 / 1,875 |
| Device → Virtual Host (FK 보유 행) | 24 / 67 | 24 / 67 |
| Subnet → VLAN | 33 / 25 | 1 / 1 |
| Subnet → VRF | 33 / 25 | 11 / 12 |
| VRF → Cloud Infrastructure | 3 / 4 | 2 / 2 |

DB Instance의 `database_type`은 Microsoft SQL이다. 연결된 DB는 9건이며 나머지
DB 1건은 databaseinstance_fk가 없지만 instance_id·root_resource_fk는 기존 Instance를 가리킨다.
참조 차이는 [원천 구조](../../knowledge/device42/database-model.md)에 있다. Instance의 `host_name`과 연결 Component의
`device_fk`도 비어 있어 이 경로로는 장치를 찾지 못한다.

Virtual Host가 일치한 자식 24 / 67건은 호스트 6 / 8건에 연결된다.
원천의 호스트–자식 관계는 1:N이다.

## 5. 본체 식별자·필수값 조사

필터 적용 전 전체 후보의 값 보유 건수다. NULL과 공백 문자열은 제외했다.

| 원천 필드 | 값 보유 `.68 / .35` | 확인 결과 |
| --- | ---: | --- |
| `view_device_v2.name` | 100 / 85 | 서버 내 유일; 최대 41자 |
| `view_device_v2.uuid` | 44 / 74 | 값 보유 행에서는 유일; 최대 64자 |
| `view_device_v2.last_discovered` | 99 / 85 | `.68` 1건 미보유 |
| `view_resource_v2.resource_name` | 820 / 4,472 | 고유 이름 647 / 3,269개; 최대 131자 |
| `view_resource_v2.identifier` | 820 / 4,472 | 서버 내 유일; 최대 145 / 146자 |
| `view_resource_v2.last_discovered` | 0 / 0 | 전건 NULL |
| `view_cloudinstance_v1.instance_id` | 8 / 58 | 서버 내 유일; 최대 19 / 36자 |
| `view_cloudinstance_v1.date_modified` | 0 / 0 | 전건 NULL |
| `view_serviceinstance_v2.last_updated` | 8,545 / 2,025 | 전건 보유 |

Device와 Resource의 숫자 PK가 67 / 15개 겹친다. 각 View 내부의 PK 유일성은
여러 유형을 합친 ACTCI의 키 유일성을 보장하지 않는다. Resource의 이름은
서버 내에서도 중복된다. `last_changed`는 Device·Resource 전건 보유하지만
발견 시각과 같은 의미인지는 확인하지 않았다.

## 6. 분류별 속성 원천 조사

| 원천 필드 | 한글 의미 | 값 보유 `.68 / .35` | 확인 결과 |
| --- | --- | ---: | --- |
| `view_device_v2.serial_no` | 일련번호 | 22 / 13 | 최대 54자 |
| `view_device_v2.total_cpus` | CPU 수 | 21 / 62 | 값 보유는 장치 일부 |
| `view_device_v2.ram` | 메모리 | 21 / 62 | 단위 `ram_size_type=GB` |
| `view_device_v2.cpu_speed` | CPU 속도 | 20 / 11 | 단위 `hz=GHz` |
| `view_database_v2.database_name` | DB 이름 | 10 / 0 | 최대 13자 |
| `view_database_v2.database_id` | DB ID | 10 / 0 | 표본 내 유일; Instance FK와 별도 |
| `view_database_v2.creation_date` | DB 생성 시각 | 10 / 0 | 문자열 표현 최대 26자 |
| `view_database_v2.collate / recovery_model / allocated_size` | 정렬 규칙 / 복구 모드 / 할당 크기 | 각 10 / 0 | 할당 크기 단위는 미확인 |
| `view_serviceinstance_v2.service_path` | 서비스 경로 | 424 / 224 | 최대 169 / 126자 |
| `view_subnet_v1.network / mask_bits` | 네트워크 주소 / Prefix 길이 | 각 33 / 25 | network 고유값 32 / 23개 |
| `view_vlan_v1.number / name` | VLAN 번호 / 이름 | 각 11 / 11 | 각각 고유값 10 / 10개 |

분류·속성 정의의 존재 여부는 [CI 분류 모델](../../knowledge/maximo/ci-classification.md),
관계 규칙은 [CI 모델](../../knowledge/maximo/ci-model.md) 참조.
포함 범위는 [ISSUE-8](../../open-issues.md#issue-8-actual-ci-대상-범위),
원천–분류·속성 대응과 누락값 처리는 [ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑)에 둔다.
