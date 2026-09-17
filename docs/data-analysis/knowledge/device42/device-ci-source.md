# Device 통합 CI 수집 원천 조사

> 관측: 2026-09-15 · D42 192.168.2.68 / 192.168.1.35 각각 읽기 전용 조회.
> 재조회: [컬럼·유형](../../exploration-queries/device42/device-ci-source-shapes.sql), [기존 조인 투영·포트 범위](../../exploration-queries/device42/device-ci-projection.sql), [JSON 키](../../exploration-queries/device42/device-ci-json-keys.sql).
> 로컬 원본: `local/db-access-kit/work/device-ci-unification-20260915/d42-{68,35}/`.
> 구현 대조: `DeviceCiImport`, `DeviceSource`, 본체용 `CiSourceFilter.DEVICE`와 자식·관계용 `CiSourceFilter.COMPUTER`.
> 1차 재검증에서 제품 조회를 양 서버에 읽기 전용으로 실행했으며 Maximo DB 값은 변경하지 않았다.

## 1. 조사 경계

Computer의 기존 조회에 쓰는 여섯 뷰만 대상으로 했다.

| 원천 | 현재 연결·행 제한 | 확보 가능한 정보 |
| --- | --- | --- |
| `view_device_v2 d` | 본체 | 식별·분류·상태·위치·OS 요약·자원 요약·BIOS·장비 연결 FK·JSON |
| `view_hardware_v2 h` | `d.hardware_fk=h.hardware_pk` | 모델·모델 제조사 FK·파트 번호·크기·전력·EOL/EOS |
| `view_vendor_v1 v/b` | 하드웨어 제조사와 BIOS 제조사로 두 번 사용 | 제조사 이름·홈페이지. CPU/포트 제조사는 이 조인의 대상이 아님 |
| `view_part_v1 p` | CPU CTE 안, `p.device_fk=d.device_pk` | CPU 파트 수량·슬롯·시리얼·펌웨어·JSON 등 |
| `view_partmodel_v1 pm` | `pm.partmodel_pk=p.partmodel_fk`, **`type_name='CPU'`** | CPU 모델·코어·스레드·속도·단위 등 |
| `view_netport_v1 n` | 대표 포트 CTE 안, `n.device_fk=d.device_pk`, **`is_default=true`** | 기본 포트의 MAC·이름·속도·MTU·연결 키 등 |

초기 조사에는 새 뷰나 별도 Device 자기 조인을 추가하지 않았다. CPU·기본 포트는 장비별 집계 후 LEFT JOIN했다.
보강 투영의 37 / 73행은 각각 고유 `device_pk` 37 / 73개로, 이번 표본에서 조인 증폭이 없었다.
조사 SELECT는 전체 표본을 조회하며 제품 페이지 처리나 적재를 실행한 것은 아니다.

1차 제품 구현에서는 같은 `view_netport_v1`의 `second_device_fk`와 `view_device_v2`의 cluster 행을
장비별로 집계하는 자기 조인을 추가했다. Computer·VM과 물리 network 장비는 36 / 72행이다.
2026-09-17 Network Cluster 본체 2 / 2개를 추가해 현재 제품 후보는 38 / 74행이다.

## 2. Device 범위 시뮬레이션

기존 Computer 조건에 물리 네트워크 장비·물리 Network Printer를 더한 후보 범위다.
이 표는 분류 매핑 승인이나 적재 성공 건수가 아니다.

| 후보 종류 | .68 | .35 |
| --- | ---: | ---: |
| 기존 물리 Computer | 3 | 5 |
| 기존 가상 Computer | 31 | 65 |
| 물리 `network_device=true` | 2 | 2 |
| 물리 Network Printer, 네트워크 플래그 false/NULL | 1 | 1 |
| 합계 | 37 | 73 |
| 후보 중 `last_discovered` 보유 | 36 | 73 |

네트워크 플래그 true와 Network Printer subtype이 겹치는 후보는 0 / 0이다.
전체 원천의 제외 표본은 unknown 39 / 0, Docker Container 25 / 14, PDU 1 / 1,
network cluster 2 / 2다. 미래 서브타입의 전체 목록을 뜻하지 않는다.

1차 지원 범위는 위 후보에서 Network Printer 1 / 1을 뺀 36 / 72행이다.
기존 Computer·VM은 34 / 70행이고 새 Switch는 2 / 2행이다. 각 서버에서 조회 행 수와
고유 `device_pk` 수가 같았다.

후속 Network Cluster 지원은 `type='cluster'`, `network_device=true`,
`details->>'fw_device_type'='Switch'`인 2 / 2개를 더한다. 현재 지원 범위는 38 / 74행이다.

## 3. 네트워크·프린터에서 실제 확보한 정보

- 물리 네트워크 장비는 양쪽 모두 모델 `C9200L-24P-4G`, `WS-C3750-24PS-S` 각 1개다.
  장비 플래그 `d.network_device=true`인데 **연결된 하드웨어의 `h.network_device=false`**다.
  분류 판정 시 이 두 값을 교체해서 쓰면 안 된다.
- 같은 모델의 subtype은 .68에서 Rackable, .35에서 Generic이다. subtype은 Switch/Router 판별자가 아니다.
- 네트워크 후보는 `second_device_fk`로 연결된 cluster가 장비마다 정확히 하나이고,
  비어 있지 않은 `details->>'fw_device_type'`도 `Switch` 한 종류다. Router 표본은 두 서버 모두 없다.
- 네트워크 후보 2 / 2개 모두 이름·시리얼·모델·제조사·OS 이름/버전·발견 시각을 확보했다.
  RAM·CPU 요약·BIOS·기본 포트 MAC은 없다.
- 프린터는 양쪽 모두 `X3220NR` 1개이며 이름·시리얼·모델·제조사·OS 이름/버전·RAM을 확보했다.
  RAM은 원문 `2.048 GB`다. 제조사 사양을 추정해 2GB로 반올림하지 않는다.
  CPU 모델도 기존 CPU CTE에서 얻지만 BIOS·기본 포트 MAC은 없다.
- 모델의 EOL/EOS·파트 번호·전력은 통합 후보 전체에서 비어 있다. **원천 컬럼은 존재한다**.
  현재 빈값이 수집 대상에서 영구 제외할 이유는 아니다.

## 4. 같은 뷰가 있어도 현재 조건으로 얻지 못하는 정보

| 포트 관측 | .68 | .35 |
| --- | ---: | ---: |
| 물리 네트워크 장비에 직접 소속된 포트 | 0 | 0 |
| network cluster에 소속된 포트 | 61 | 60 |
| 그중 `second_device_fk` 보유 | 54 | 54 |
| 프린터에 소속된 포트 | 2 | 2 |
| 프린터 기본 포트 | 0 | 0 |

초기 조건 `n.device_fk=d.device_pk AND n.is_default=true`로는 네트워크·프린터의 대표 MAC을
얻을 수 없다. 1차 제품 조회는 Switch에 한해 `second_device_fk=physical device_pk`인 cluster 포트의
비어 있지 않은 MAC 중 MIN을 대표 MAC으로 사용한다. 이는 기존 DPA에서 검증한 물리 멤버 연결 규칙과
양 서버 표본을 따른다. Printer의 기본 포트 선정은 아직 정하지 않았다.

프린터 트레이도 `view_part_v1`에 원천은 있지만 현재 CPU CTE의 `type_name='CPU'` 밖이다.
동일 뷰를 사용한다는 이유로 현재 집계에서 트레이 수를 얻는다고 설명하지 않는다.
장비 IP는 `d.ip_addresses` 컬럼이 존재하지만 복수 표현의 선택·주소 정규화는 미검증이다.
IP 뷰·Subnet을 새로 조인하거나 주소 하나를 임의 선택하지 않았다.

## 5. 추가 JOIN 없이 읽을 연결 정보

- Device: `virtual_host_device_fk`, `vm_manager_device_fk`, `host_chassis_device_fk`.
  후보 범위의 비NULL 건수는 각각 3 / 55, 0 / 4, 0 / 0이다.
- Device의 OS·위치·모델 FK도 확보 가능하지만 관계 코드나 독립 CI 도입을 의미하지 않는다.
- Netport에는 `remote_netport_fk`, `second_device_fk`, `module_device_fk`, `part_fk`,
  `parent_part_fk`, `primary_vlan_fk`가 있다. 이들은 **포트 수준 키**다.
  기본 포트 집계만으로 전체 포트 연결을 보존할 수 없다.
- 현재 Computer 범위 전체 포트의 `remote_netport_fk`는 3 / 2개, 기본 포트 투영에서는 2 / 0개다.
  이전 조사에서 `second_device_fk` 표본이 없었던 사실을 네트워크 연결 원천 부재로 일반화할 수 없다.
  `remote_netport_fk`의 상대 소속 장비·연결 의미는 이번에 추적하지 않았다.

관계는 본체 DTO를 보관하지 않고 기존 관계 단계에서 연결 키를 재조회한다.
위 키 확보가 관계 코드·방향 확정을 뜻하지 않는다.

## 6. JSON의 추가 정보와 해석 경계

키만 조회했고 라이선스·담당자 등 비밀값·개인정보를 문서에 복사하지 않았다.

- 물리 네트워크 장비 details에는 `switch_id`, `ports`가 있다. `switch_id`는 네 장비 모두 문자열 `1`이다.
  장비 PK를 가리키는 FK로 해석할 근거가 없다.
- cluster details에는 `fw_device_type=Switch`, `snmp_class=SNMP::Info::Layer3::CatalystStacked`,
  `contact`, `description`, `snmp_location`, `layers`, `advertised_dp_info` 등이 있다.
  물리 Device의 JSON과 구분해야 한다. 해당 물리 장비로 옮기려면 별도 연결 검증이 필요하다.
- 프린터 details에는 `d42_device_classification=printer`, `device_type=printer`,
  `fw_device_type=Printer`, `full_os`, `ports`가 있다.
- Computer 계열에는 `domain`, `dns_name`, `guest_hostname`, `private_dns_name`, `firmware`,
  `last_boot_time`, `power_state` 등 발견 방식별 키가 있다. 한 키를 전 유형 공통으로 가정하지 않는다.
- 조회한 네트워크·프린터 JSON 키에서 `sysName` 전용 원천을 확인하지 못했다.
  `device.name`이 SNMP sysName과 동일하다는 검증은 없다.
- `vendor_custom_fields`는 관측 시 VM의 용도·담당자·이용기간·AWS 태그 계열이다.
  고정 DTO 필드를 무제한 늘리거나 이를 EXTENDEDATTRIBUTES에 통째로 적재하는 정책은 정하지 않았다.

## 7. 검증 범위

두 서버에서 6개 원천 뷰 헤더·분류 분포·보강 투영·포트 범위·JSON 키를 실조회했다.
1차 제품 Count·페이지 SQL은 36 / 72행이었고, Network Cluster 확장 후에는 38 / 74행과
고유 PK 38 / 74개를 확인했다. Switch 2 / 2개는 cluster·종류 수 1과 대표 MAC을,
Network Cluster 2 / 2개는 `fw_device_type=Switch`를 확인했다. 비정형 필드의 `has_*`는 **비NULL 여부만**
나타내며 빈 JSON/문자열을 제외한 충전율이 아니다. Maximo 관계·스펙의 실제 저장이나 새 분류 승격은 실행하지 않았다.
선택·매핑 제안과 미결은 [Device 통합 설계](../../design/ci/device.md)를 따른다.
