# Computer CI 관계 원천

> 관측: 2026-09-15 · D42 192.168.2.68 / 192.168.1.35, 각각 독립 조회.
> Host→VM 제품 매핑 재검증: 2026-09-16.
> 재조회: [computer-ci-relations.sql](../../exploration-queries/device42/computer-ci-relations.sql).
> 원문 결과: 로컬 `local/db-access-kit/work/ci-relations-20260915/d42-{68,35}/`,
> `local/db-access-kit/work/host-vm-20260916/d42-{68,35}/`.
> 구현 대조: `7a29d30`의 CiSourceFilter·각 CiIntegrate. 건수는 검증 표본이며 운영 상수로 사용하지 않는다.

## 범위와 식별자

현재 Computer 필터에 들어오는 장비와 그에 연결된 OS·Disk·Filesystem·IP를 대상으로 한다.
Filesystem은 현재 코드와 같이 overlay·devtmpfs·squashfs·efivarfs를 제외한다.
전체 자산의 카테고리별 행수나 속성 값 보유율을 조사한 문서가 아니다.

| 개체 | 본체 식별자 | 연결 근거 |
| --- | --- | --- |
| Computer | D42:DEVICE:<device_pk> | 자기 참조 FK |
| OS | D42:DEVICEOS:<deviceos_pk> | view_deviceos_v1.device_fk |
| Disk | D42:PART:<part_pk> | view_part_v1.device_fk, partmodel.type_name='Hard Disk' |
| Filesystem | D42:MOUNTPOINT:<mountpoint_pk> | view_mountpoint_v2.device_fks 배열 |
| IP | D42:IPADDRESS:<ipaddress_pk> | view_ipaddress_v2.device_fks 배열, netport_fk |
| Interface 후보 | 미구현. D42:NETPORT:<netport_pk> 제안 | view_netport_v1.device_fk |

## 관계 쌍 보존 확인

아래 값은 .68 / .35 순서다. `component-edges`의 각 (kind, child_pk, computer_pk)는 중복이 없다.

| 연결 | 본체 수 | 서로 다른 연결 쌍 수 | 해석 |
| --- | ---: | ---: | --- |
| Disk–Computer | 23 / 19 | 23 / 19 | 단일 device_fk |
| OS–Computer | 26 / 63 | 26 / 63 | 단일 device_fk |
| Filesystem–Computer | 69 / 60 | 69 / 60 | 이번 표본은 한 본체당 장비 하나. 배열 구조 자체는 유지 |
| IP–Computer | 50 / 97 | 51 / 118 | 본체 하나에 복수 장비가 실제 존재 |

IP의 다중 장비 연결은 1 / 16개 주소에서 확인됐고, 한 주소의 범위 내 장비 수는 최대 2 / 7이다.
현재 IpCiIntegrate의 `DISTINCT ON(ipaddress_pk)`는 본체를 한 번 적재하기 위한 조건이다.
그 결과의 단일 `device_fk`만 관계에 사용하면 1 / 21개 연결 쌍을 잃는다.
본체의 DISTINCT ON은 유지할 수 있지만 관계는 모든 실제 연결 쌍을 보존해야 한다.
Filesystem도 이후 다중 배열이 들어올 수 있으므로 같은 원칙을 적용한다.

## Device–Device

| 원천 FK | .68 / .35 연결 | 관측 |
| --- | ---: | --- |
| virtual_host_device_fk | 3 / 55 | 모두 virtual → physical 또는 virtual. 양쪽 모두 현재 Computer 범위 |
| host_chassis_device_fk | 0 / 0 | 현재 범위의 표본 없음. 섀시 관계 부재를 일반화하지 않는다 |
| vm_manager_device_fk | 0 / 4 | .35는 physical → virtual 관리 장비. 실행 호스트 FK와 다름 |

virtual_host 연결의 호스트 수는 2 / 5, 호스트 하나의 VM은 최대 2 / 17이다.
자기 자신을 호스트로 가리키는 행은 없고, 위 세 FK가 채워졌으나 대상 device 행이 없는 사례도 없었다.
조회는 자기 연결 여부까지만 검사했으며 임의 깊이의 순환 검사는 수행하지 않았다.
현재 모델은 호스트가 physical이라고 가정하면 virtual → virtual 연결을 누락한다.
관리 장비 FK를 실행 호스트 FK 대신 쓰지 않는다.

2026-09-16 제품과 같은 매핑 SELECT는 이 FK 방향을 뒤집어 Host를 SOURCECI, VM을 TARGETCI로
반환했으며 `.68` 3건, `.35` 55건이다. 관계 코드는 신규 `VIRTUALIZES`다.

## Interface를 거치는 IP 경로

`ip-interface-paths`는 주소의 장비 배열을 전개하고,
`netport_fk → netport_pk → device_fk`가 가리키는 장비와 각각 비교한다.

| 항목 | .68 / .35 |
| --- | ---: |
| 수집 범위 내 IP | 50 / 97 |
| 실제 포트와 그 부모 Computer가 모두 확인되는 IP | 42 / 87 |
| 이 경로가 없는 IP | 8 / 10 |
| 배열에서 얻은 Computer–IP 쌍 | 51 / 118 |
| 포트 경로와 일치하는 Computer–IP 쌍 | 42 / 87 |

포트 부모는 연결된 IP의 device_fks 중 하나와 모두 일치했다. 그러나 한 개의 netport_fk로
나머지 공유 장비 연결까지 증명할 수는 없다. IP와 같은 장비에 있는 모든 포트를 연결하지 않는다.
현재 포트 경로로 설명되지 않는 배열 연결 쌍은 9 / 31이며, 그 자체가 데이터 오류라는 뜻은 아니다.

포트 하나에 복수 IP가 연결되는 사례도 있다: 4 / 5개 포트, 최대 3 / 8개 IP.
Maximo의 등록된 BINDSTO 1:1 설정과 대조가 필요하다.
현재 Computer 범위에 속한 포트는 88 / 265개이며, second_device_fk가 있는 포트는 없었다.
따라서 이 필드로 Computer끼리 네트워크 연결을 만드는 표본 근거는 확보하지 못했다.

## 자동으로 만들지 않을 연결

- OS와 Filesystem이 같은 device를 참조한다는 사실은 BOOTSFROM의 근거가 아니다.
  `os-filesystem-candidates`는 같은 장비 후보만 반환한다. 부팅 볼륨임을 증명하지 않는다.
- mountpoint의 filesystem 문자열과 Disk의 모델·장비 키만으로 Disk–Filesystem을 연결하지 않는다.
  확인한 mountpoint 컬럼에 part_pk를 가리키는 FK는 없다.
- IP의 장비 배열 연결을 곧바로 CONTAINS/BINDSTO 관계로 해석하지 않는다.
  종류·방향의 판정은 [관계 설계](../../design/ci/relations.md)에서 한다.

## 검증

두 서버에서 조사 6블록과 실제 매핑 SELECT 2블록을 실행했다.
매핑 SELECT가 반환하는 논리키 쌍은 원천 연결 쌍과 일치하며,
OS·Disk·Filesystem에서 관계 키 (SOURCECI, TARGETCI, RELATIONNUM) 중복은 없었다.
Maximo에 본체 또는 관계를 쓰지 않았다.
