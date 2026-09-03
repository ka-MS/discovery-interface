# CI 대상

Device42에서 독립 Actual CI로 적재할 개체의 범위를 정한다.

> 관측 2026-08-31 · Device42 **양쪽 서버** 192.168.2.68 / 192.168.1.35
> 재조회 `../../exploration-queries/device42/ci-target-candidates.sql`
> 재조회 `../../exploration-queries/device42/ci-target-relations.sql`
> 재조회 `../../exploration-queries/device42/ci-target-profiles.sql`
> 재조회 `../../exploration-queries/device42/ci-target-overlaps.sql`

수치는 `.68 / .35` 순이다.

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
| Database Instance | `view_databaseinstance_v2` | 1 / 0 | `view_appcomp_v1` | Application Component와 1:1. 별도 CI 여부 미결 |
| Device | `view_device_v2` | 95 / 85 | Device 상세·관계 View | 본체 후보 |
| Kubernetes Cluster | `view_resource_v2` | 1 / 2 | `view_k8scluster_v2` | Resource의 전용 상세 |
| Kubernetes Deployment | `view_resource_v2` | 24 / 200 | `view_k8sdeployment_v2` | Resource의 전용 상세 |
| Kubernetes Node | `view_resource_v2` | 4 / 5 | `view_k8snode_v2` | Resource의 전용 상세 |
| Kubernetes Service | `view_resource_v2` | 41 / 237 | `view_k8sservice_v2` | Resource의 전용 상세 |
| Resource | `view_resource_v2` | 820 / 4,472 | 유형별 전용 View | 유형 필터가 필요한 본체 후보 |
| Service Instance | `view_serviceinstance_v2` | 8,507 / 2,025 | Device, Service | 본체 후보. 적재 단위 미결 |
| Storage Array | `view_resource_v2` | 0 / 0 | `view_storagearray_v2` | 양쪽 표본 없음 |
| Subnet | `view_subnet_v1` | 27 / 25 | VLAN, VRF, 부모 Subnet | 본체 후보 |
| VLAN | `view_vlan_v1` | 11 / 11 | Subnet, Switch, Netport | 본체 후보 |
| VRF | `view_vrfgroup_v1` | 3 / 4 | Cloud Infrastructure, Subnet | 본체 후보 |

`본체 후보`는 적재 확정이 아니다. 포함 범위는 `../../open-issues.md` ISSUE-8에서
결정한다.

## 3. 중복 표현

### Resource와 전용 View

다음 전용 View는 `view_resource_v2`와 식별값이 전건 일치한다. 별도 CI가 아니라
같은 Resource의 상세 표현이다.

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

별도 CI로 만들지 Application Component 속성으로 둘지는 미결이다.

## 4. 관계 커버리지

| 관계 | Source `.68 / .35` | FK 일치 `.68 / .35` |
| --- | ---: | ---: |
| Database → Database Instance | 10 / 0 | 9 / 0 |
| Database Instance → Application Component | 1 / 0 | 1 / 0 |
| Application Component → Device | 34 / 6 | 33 / 6 |
| Application Component → Resource | 34 / 6 | 0 / 0 |
| Cloud Instance → Device | 8 / 58 | 8 / 58 |
| Kubernetes Deployment → Cluster | 24 / 200 | 24 / 200 |
| Kubernetes Node → Cluster | 4 / 5 | 4 / 5 |
| Kubernetes Service → Cluster | 41 / 237 | 41 / 237 |
| Resource → Cloud Infrastructure | 820 / 4,472 | 56 / 55 |
| Resource → Root Resource | 820 / 4,472 | 658 / 4,207 |
| Service Instance → Device | 8,507 / 2,025 | 8,473 / 1,875 |
| Subnet → VLAN | 27 / 25 | 1 / 1 |
| Subnet → VRF | 27 / 25 | 11 / 12 |
| VRF → Cloud Infrastructure | 3 / 4 | 2 / 2 |

FK가 없는 행은 해당 경로로 관계를 만들 수 없다. 원천 행의 포함 여부는 별도로
결정한다.

## 5. 범위 결정이 필요한 후보

- `view_resource_v2`는 `.68` 37종 820행, `.35` 40종 4,472행이다. Kubernetes,
  Database, AWS, VMware, Storage 유형이 함께 있으므로 유형별 포함 기준이 필요하다.
- Application Group은 `.68`에서 Active 10행, Suggested 20행, Invalidated 2행이며
  `.35`의 2행은 모두 Suggested다. 상태별 포함 기준이 필요하다.
- Service Instance는 Device보다 행이 많고 한 Device에 여러 행이 연결된다. 서비스
  종류와 상태를 기준으로 적재 단위를 정해야 한다.
- Network 논리 개체인 Subnet, VLAN, VRF를 Actual CI로 관리할지 결정이 필요하다.

정책 결정은 `../../open-issues.md` ISSUE-8에 둔다.
