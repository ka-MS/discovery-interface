# Computer 연관 수집 원천

> 관측 2026-09-11 · Device42 192.168.2.68 / 192.168.1.35 · 읽기 전용 DOQL 재조회.
> 표의 수치는 `.68 / .35` 순서다. 빈 문자열·공백·NULL을 제외한 값 보유 건수이며, 값의 정확성 검증과는 다르다.
> 재조회: [수집 항목 조사](../../exploration-queries/device42/computer-collection-inventory.sql),
> [식별자·RAM 검증](../../exploration-queries/device42/computer-identity-memory.sql).
> 결과: `local/db-access-kit/work/computer-inventory-20260911-{68,35}/`.

## 1. 조사 범위

D42 원천의 Computer와 연결된 정보 목록이다. DPA 테이블·적재 결과·변환 규칙에 의존하지 않는다.
각 행은 수집 정보의 묶음이며 독립 CI 하나를 뜻하지 않는다.
CI 진행 상태는 [Computer](../../data-mapping/ci/types/computer.md), 표현·범위 결정은 [ISSUE-8](../../open-issues.md#issue-8-actual-ci-대상-범위)에서 관리한다.

이번 조사 표본은 `view_device_v2`에 다음 조건을 적용했다. CI 운영 필터를 확정한 것은 아니다.

```sql
type IN ('physical', 'virtual')
AND (network_device = false OR network_device IS NULL)
AND (virtualsubtype IS NULL OR virtualsubtype <> 'Docker Container')
AND (physicalsubtype IS NULL OR physicalsubtype NOT IN
    ('Network Printer', 'PDU', 'CRAC', 'UPS',
     'Branch Circuit Power Meter', 'Power Unit', 'Environment Monitor'))
```

표본은 **34 / 70대**다. physical은 3 / 5대, virtual은 31 / 65대다.
관측된 물리 서브타입은 Generic, 가상 서브타입은 VMWare·Amazon EC2 Instance·Internal VM·Hyper-V다.
전체 장치 유형 분포도 `device-population`에서 함께 조회했다. unknown·cluster·컨테이너 등의 정보는 위 표본 집계에 포함하지 않았다.

## 2. 수집 항목·원천 필드

아래 필드는 주요 수집·보강 항목이다. D42 뷰 전체 컬럼 목록이나 확정된 ACTCISPEC 속성 목록이 아니다.
`device_pk` 등은 현재 원천 행의 조회·연결 키이며 재발견 이후의 동일성 보장은 별도 검증 대상이다.

| 수집 항목 | 주요 정보 | D42 원천 |
| --- | --- | --- |
| Computer 식별·기본 정보 | 원천 ID, 이름, 시리얼, UUID, 장비 유형, 모델·제조사, 서비스 상태, 최종 발견 시각 | `view_device_v2`: device_pk, name, serial_no, uuid, type, physicalsubtype, virtualsubtype, network_device, in_service, last_discovered; `view_hardware_v2.name`, `view_vendor_v1.name` |
| BIOS | 제조사명, 버전, 출시일 | `view_device_v2`: bios_vendor_fk, bios_version, bios_release_date; `view_vendor_v1.name`; BIOS 제품명으로 확인된 필드는 아님 |
| 메모리 총량 | 장비가 보고한 총용량·단위 | `view_device_v2`: ram, ram_size_type |
| CPU 요약 | CPU 수, CPU당 코어 수, 속도 | `view_device_v2`: total_cpus, core_per_cpu, cpu_speed |
| 개별 CPU 파트 | 슬롯, 모델·제조사, 코어 수, 속도·단위, 시리얼 | `view_part_v1`: part_pk, device_fk, slot, serial_no, pcount; `view_partmodel_v1`: name, vendor_fk, cores, speed, speed_unit; type_name='CPU' |
| 개별 RAM 파트 | 슬롯, 시리얼, 모델·제조사, 용량·단위, RAM 유형 | 같은 Part·Part Model 조인; `p.slot, p.serial_no, p.pcount, pm.name, pm.vendor_fk, pm.ramsize, pm.ramsize_unit, pm.ramtype`; type_name='RAM' |
| 디스크 | 모델·제조사, 시리얼, 용량·단위, 디스크·미디어 유형 | 같은 Part·Part Model 조인; `p.serial_no, p.pcount, pm.name, pm.vendor_fk, pm.hdsize, pm.hdsize_unit, pm.hddtype_name, pm.media_type_name`; type_name='Hard Disk' |
| 파일시스템·마운트 | 마운트 경로, 파일시스템 원천 문자열·종류, 전체·여유 용량, 라벨 | `view_mountpoint_v2`: mountpoint_pk, device_fks, mountpoint, filesystem, fstype_name, capacity, free_capacity, label |
| 운영체제 | 설치 OS 이름·버전·빌드, 제조사 | `view_deviceos_v1`: deviceos_pk, device_fk, os_fk, os_name, os_version, os_version_no; `view_os_v1.vendor_fk` → Vendor |
| 네트워크 인터페이스 | 포트명, MAC, 속도, 인터페이스 유형, MTU, 상태, 제조사 | `view_netport_v1`: netport_pk, device_fk, port, hwaddress, hwaddress2, port_speed, global_type, type_name, mtu, physical_state, vendor_fk |
| IP·서브넷 설정 | IP, 연결 장비·포트, 서브넷, 프리픽스 길이, 게이트웨이, 공유 표시 | `view_ipaddress_v2`: ipaddress_pk, device_fks, ip_address, netport_fk, subnet_fk, is_shared; `view_subnet_v1`: mask_bits, gateway |
| GPU | 모델·제조사, 시리얼, 메모리 용량·단위 | Part·Part Model 조인; `p.serial_no, p.pcount, pm.name, pm.vendor_fk, pm.ramsize, pm.ramsize_unit`; type_name='GPU' |
| 설치 소프트웨어 | 제품명·제조사, 설치 버전, 설치 경로·일자, 최초 탐지·최종 갱신 시각 | `view_softwareinuse_v1`: softwareinuse_pk, device_fk, software_fk, version, install_path, install_date, first_detected, last_updated; `view_software_v1`: name, vendor_fk |
| 가상 호스트·섀시·관리 장비 연결 | VM의 호스트, 섀시, VM 관리 장비 원천 FK | `view_device_v2`: virtual_host_device_fk, host_chassis_device_fk, vm_manager_device_fk |

Part의 `first_added, last_updated`, Mount의 `first_added, last_updated`,
Netport의 `first_added, last_edited`도 양쪽 표본 전건 보유한다.
이 시각들이 발견 시각과 같은 의미인지는 이 조회로 확인되지 않는다.

사업 문서의 서버 명시 항목은 OS·CPU·Disk·Filesystem·Memory다.
설치 소프트웨어에는 DB·WEB/WAS·기타 SW의 요구 필드가 있지만, 제품별 구분과 누락 보강은 아직 검증하지 않았다.
GPU·개별 RAM 모듈·IP·BIOS 등을 독립 CI로 관리하라는 요구는 제공된 발췌에 명시되어 있지 않다.

## 3. 연결 키·관측 건수

행 수는 고유 원천 PK 수, 장비 수는 연결된 표본 Computer 수다.
IP·Mount는 배열에 포함된 Computer 각각과 연결한 쌍도 별도로 센다.

| 수집 항목 | 원천 연결 식 | 원천 행 수 .68 / .35 | 연결 장비 수 .68 / .35 |
| --- | --- | ---: | ---: |
| CPU | `p.device_fk=d.device_pk`, type_name='CPU' | 70 / 106 | 21 / 23 |
| RAM 파트 | `p.device_fk=d.device_pk`, type_name='RAM' | 35 / 18 | 16 / 12 |
| 디스크 | `p.device_fk=d.device_pk`, type_name='Hard Disk' | 23 / 19 | 18 / 19 |
| GPU | `p.device_fk=d.device_pk`, type_name='GPU' | 6 / 3 | 3 / 2 |
| OS | `o.device_fk=d.device_pk` | 26 / 63 | 26 / 63 |
| 파일시스템 | `d.device_pk=ANY(m.device_fks)` | 117 / 141 | 22 / 23 |
| 인터페이스 | `n.device_fk=d.device_pk` | 88 / 241 | 33 / 69 |
| IP | `d.device_pk=ANY(i.device_fks)` | 50 / 97 | 33 / 64 |
| 설치 소프트웨어 | `u.device_fk=d.device_pk` | 4,900 / 5,683 | 19 / 19 |

보강 조인:

- Part → Model: `p.partmodel_fk=pm.partmodel_pk`; Model → Vendor: `pm.vendor_fk=v.vendor_pk`.
- Device → Hardware → Vendor: `d.hardware_fk=h.hardware_pk AND h.vendor_fk=v.vendor_pk`.
- BIOS → Vendor: `d.bios_vendor_fk=v.vendor_pk`.
- OS → OS 제품 → Vendor: `o.os_fk=m.os_pk AND m.vendor_fk=v.vendor_pk`.
- 설치 SW → SW 제품 → Vendor: `u.software_fk=s.software_pk AND s.vendor_fk=v.vendor_pk`.
- IP → Port / Subnet: `i.netport_fk=n.netport_pk` / `i.subnet_fk=b.subnet_pk`.

IP의 원천 행은 50 / 97개지만 Computer 연결 쌍은 **51 / 118쌍**이다.
Mount는 117 / 141쌍으로 이번 표본에서는 행 수와 같다.
IP→Port FK는 42 / 87개이고 모두 실제 Port에 연결된다. 그 Port의 device_fk도 IP의 device_fks 안에 있다.
나머지 IP에는 Port FK가 없다. Subnet 연결은 50 / 97개 전건 존재한다.

가상 호스트 FK는 3 / 55개이며 모두 실제 Device와 표본 Computer에 연결된다.
섀시 FK는 양쪽 모두 없고, VM 관리자 FK는 0 / 4개이며 존재하는 FK는 모두 실제 Device에 연결된다.
이 FK 방향은 원천 참조 방향이며 Maximo 관계 코드·저장 방향을 확정하지 않는다.

## 4. 값 보유율·불일치

분모는 각 원천의 행 수다. 없는 값을 0·UNKNOWN 등으로 대체하지 않은 관측이다.

| 항목 | .68 | .35 | 확인된 한계 |
| --- | --- | --- | --- |
| Computer 이름 / UUID / 시리얼 | 34 / 25 / 21건 (34대) | 70 / 65 / 15건 (70대) | 표본 내 비어 있지 않은 값의 중복은 없음; UUID·시리얼 누락, 이름의 장기 안정성 미검증 |
| 장비 모델·제조사 | 각각 3/34 | 각각 5/70 | 가상 장비 보강 가용성이 낮음 |
| 최종 발견 시각 | 33/34 | 70/70 | 1대 누락 |
| BIOS 제조사 / 버전 / 날짜 | 1 / 3 / 3건 | 0 / 5 / 5건 | BIOS 제조사명은 제품명이 아님 |
| 장비 RAM 용량·단위 | 각각 26/34 | 각각 65/70 | 존재하는 단위는 GB |
| CPU 수·CPU당 코어 수 | 각각 26/34 | 각각 65/70 | 개별 CPU 파트 존재 여부와 다름 |
| CPU 파트 시리얼 | 0/70 | 0/106 | 시리얼 기반 개별 식별 불가 |
| RAM 슬롯 / 시리얼 / 모델 | 19 / 21 / 32건 (35행) | 18 / 5 / 15건 (18행) | 식별에 사용할 값의 누락 존재 |
| RAM 용량·단위 / 유형 | 각각 35건 / 0건 | 각각 18건 / 0건 | 용량·GB 단위는 전건 존재, ramtype은 전건 비어 있음 |
| 디스크 시리얼 / 제조사 | 13 / 0건 (23행) | 3 / 0건 (19행) | 모델·용량·단위는 전건 존재 |
| OS 버전 / 빌드 / 제조사 | 17 / 23 / 23건 (26행) | 21 / 19 / 16건 (63행) | OS 이름은 전건 존재 |
| 인터페이스 포트명 / MAC | 77 / 80건 (88행) | 184 / 236건 (241행) | MAC2·속도·global_type·type_name·MTU·상태·제조사는 양쪽 모두 비어 있음 |
| IP / 프리픽스 / 게이트웨이 | 50 / 50 / 0건 | 97 / 97 / 0건 | 게이트웨이 필드는 있지만 값은 없음 |
| GPU 시리얼 / 메모리 용량 | 6 / 0건 | 3 / 0건 | 메모리 단위만 있고 용량은 없음 |
| SW 설치 경로 / 설치 일자 | 72 / 3,529건 (4,900행) | 22 / 4,216건 (5,683행) | 이름·버전 가용성과 경로 가용성은 다름 |
| SW 버전 / 제조사 | 4,894 / 1,226건 | 5,683 / 1,206건 | 최초 탐지·최종 갱신 시각은 전건 존재 |

RAM 파트의 pcount는 두 표본 모두 1이다. .68에는 16행의 RAM이 연결되었으나 슬롯명이 전부 빈 장비가 1대 있다.
양쪽에서 RAM이 연결된 장비 16 / 12대 모두 장비 총량과 파트 합계를 같은 GB 단위로 비교할 수 있다.
일치한 장비는 **15 / 12대**다. .68의 1대는 불일치하며 원인은 이 조회로 확정되지 않았다.
장착 RAM 행 수만으로 총 슬롯 수·빈 슬롯 수를 확인할 수는 없다.

파일시스템은 overlay·devtmpfs·efivarfs도 제외하지 않고 조사했다.
용량이 있는 행은 114/117 · 140/141, 여유 용량은 106/117 · 129/141이다.
`capacity/free_capacity`에 대한 CI 저장 단위·가상 파일시스템 포함 정책은 아직 확정되지 않았다.

Part 유형 분포에는 .68에서 PCI 319행도 관측됐다. 이번 CPU·RAM·디스크·GPU 상세 표에는 포함하지 않았다.
추가 파트 유형 조사·포함 여부는 ISSUE-8에서 관리한다.

## 5. 검증 범위

두 서버에서 원천 필드 조회·값 보유율·현재 PK 기반 장비 연결·식별 후보 중복·RAM 합계를 검증했다.
Maximo 분류·속성·관계 대응, 원천 값과 실제 장비의 일치, 재발견 후 식별 안정성, 적재·UI 표시는 이 조사에 포함하지 않았다.

## 6. Computer 매핑 재대조 — 2026-09-14

양쪽 서버의 데이터 사전(`/services/data/v1.0/dd/`)과
[필드·CPU·기본 포트 조회](../../exploration-queries/device42/computer-ci-mapping-audit.sql)를 재확인했다.
결과는 `local/db-access-kit/work/computer-ci-mapping-20260914-{68,35}/`에 있다.
실제 매핑 SELECT는 [Computer 매핑](../../data-mapping/ci/types/computer.md#5-조회-sql)이 정본이다.

| 원천 | 확인된 의미·형태 |
| --- | --- |
| d.total_cpus / core_per_cpu | 데이터 사전은 장비의 CPU 수 / CPU당 코어 수로 설명. threads_per_core는 별도 필드 |
| d.cpu_speed / hz | 속도 숫자 / 속도 단위. 양쪽의 보유 단위는 GHz |
| d.ram / ram_size_type | 장비 총 메모리 / 용량 단위. 양쪽의 보유 단위는 GB |
| d.bios_version / bios_release_date | BIOS 버전 / 출시일. 날짜는 문자열이며 yyyy-MM-dd, MM/dd/yyyy, yyyy/MM/dd HH:mm 형태 관측 |
| d.vm_manager_int_id / vm_manager_ref_id | VM 관리자 내부 ID / 참조 ID. 같은 장비에 UUID 형태 내부 ID와 vm-접두어 참조 ID가 별도로 존재 |
| d.os_architecture | 데이터 사전은 OS의 32/64-bit로 설명. CPU 아키텍처 문자열과 구분 |
| p.details→architecture | CPU 파트의 아키텍처 문자열. x86_64, x64 (64-bit) 관측 |
| n.is_default | 데이터 사전은 기본 포트 여부로 설명. 양쪽에 true인 포트가 존재 |

같은 장비에 서로 다른 CPU 모델명·아키텍처 문자열이 나타나는 사례가 있다.
JSON 문자열이 다르다는 사실만으로 실제 CPU 아키텍처가 서로 다르다고 단정하지 않는다.
기본 포트는 모든 장비에 존재하지 않는다. 한 포트만 존재한다는 사실도 기본 포트임을 보증하지 않는다.
문서의 원천 SQL은 양쪽에서 실행되었고 장비 키 중복이 없었다. 실제 장비 대조·업무 테이블 적재 시험은 수행하지 않았다.
