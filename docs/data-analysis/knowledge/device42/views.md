# Device42 DOQL 뷰

> 관측 2026-08-27 · Device42 192.168.1.35
> 재조회 docs/data-analysis/exploration-queries/device42/view-counts.sql
> 벤더 절만 2026-08-28 · 양쪽 서버
> 재조회 docs/data-analysis/exploration-queries/device42/vendor-master-source.sql

실재가 확인된 뷰와 device 연결 키다. 카탈로그 조회가 막혀 있어
개별 확인으로 얻은 목록이다.

## device 를 직접 참조하는 뷰

매핑의 주 원천이다.

| 뷰 | 연결 컬럼 | 대응 Maximo 테이블 |
| --- | --- | --- |
| `view_deviceos_v1` | `device_fk` | DPAOS |
| `view_softwareinuse_v1` | `device_fk` | DPASOFTWARE |
| `view_netport_v1` | `device_fk` | DPANETADAPTER |
| `view_ipaddress_v1` | `device_fk` | DPATCPIP |
| `view_mountpoint_v1` | `device_fk` | DPALOGICALDRIVE |
| `view_part_v1` | `device_fk` | DPACPU, DPADISK, DPAMEDIAADAPTER, DPACOMPUTER, DPANETPRINTER |
| `view_serviceinstance_v2` | `device_fk` | 대응 없음 |
| `view_appcomp_v1` | `device_fk` | 대응 없음 |
| `view_deviceurl_v1` | `device_fk` | 대응 없음 |
| `view_pdu_v1` | `device_fk` | 대응 없음 |

`view_device_v2` 는 `virtual_host_device_fk`, `host_chassis_device_fk`,
`vm_manager_device_fk` 로 자기 자신을 참조한다.

## 포트와 MAC

MAC 전용 뷰는 없다. MAC 은 `view_netport_v1.hwaddress` 에만 있으며 포트와 1:1 이다.
`hwaddress2` 는 전건 비어 있다.

관측 290포트 중 249개가 MAC 을 가지며 그중 248개가 고유하다. MAC 이 없는 것은
논리 인터페이스(`Vlan1`, `Loopback Interface`), 미사용 물리 포트
(`GigabitEthernet0/0`, `Bluetooth0/4`), AWS 가상 인터페이스(`eni-…`)다.

스위치의 물리 포트 MAC 은 빈틈없는 연속 블록이다. `ITMSG_L2_SW1` 은 26포트가
`0019aa435281`~`0019aa43529a`, `ITMSG_L3_SW1` 은 28포트가
`549fc6badb81`~`549fc6badb9c` 다.

`second_device_fk` 가 채워진 포트는 cluster → physical 방향뿐이다. 서버·VM
포트에는 없다.

## 보강 조인용 뷰

| 뷰 | 조인 키 | 용도 |
| --- | --- | --- |
| `view_os_v1` | `view_deviceos_v1.os_fk = os_pk` | OS 이름·제조사 |
| `view_software_v1` | `view_softwareinuse_v1.software_fk = software_pk` | 소프트웨어명·분류 |
| `view_partmodel_v1` | `view_part_v1.partmodel_fk = partmodel_pk` | 파트 모델명·타입 |
| `view_vendor_v1` | 각 뷰의 `vendor_fk = vendor_pk` | 제조사명 |
| `view_hardware_v1` | `view_device_v2.hardware_fk = hardware_pk` | 하드웨어 모델명 |
| `view_subnet_v1` | `view_ipaddress_v1.subnet_fk = subnet_pk` | 넷마스크·게이트웨이 |
| `view_vlan_v1` | `view_subnet_v1.parent_vlan_fk = vlan_pk` | VLAN |
| `view_service_v2` | `view_serviceinstance_v2.service_fk = service_pk` | 서비스명 |

## 벤더

`view_vendor_v1` 이 벤더 마스터다. 장비와 소프트웨어가 각각 `vendor_fk` 로
참조한다. 장비는 직접 참조하지 않고 `view_device_v2.hardware_fk` →
`view_hardware_v1.vendor_fk` 로 두 단계를 거친다. Maximo 제조사 마스터에 넣을
값의 원천이다.

| 관측 | 192.168.2.68 | 192.168.1.35 |
| --- | --- | --- |
| 벤더 | 71 | 41 |
| 하드웨어가 쓰는 벤더 | 6 | 6 |
| 소프트웨어가 쓰는 벤더 | 45 | 11 |
| `vendor_fk` 없는 소프트웨어 | 1613 / 2273 | 586 / 808 |

가상 장비는 `hardware_fk` 가 비어 제조사를 얻을 수 없다. 물리 장비만 벤더에
닿는다.

이름은 정규화되어 있지 않다. `IBM` 과 `IBM Corporation`, `Microsoft` 와
`Microsoft Corp.` 와 `Microsoft Corporation`, `HCL` 과 `HCL Technologies Ltd.`
와 `HCL Technologies Limited` 가 각각 별개 행이다. `Google\Chrome` 처럼 벤더명이
아닌 값도 있다.

`enrichai_details` 에 `normalized` 와 `alias` 가 들어 있으나 충전율이 낮다.
`normalized` 는 18/71 · 14/41, `alias` 는 10/71 · 6/41 이다. 정규화 사전으로
쓸 수 없다.

## 존재하지 않는 뷰

확인 결과 없는 것들이다. 재조사를 막기 위해 기록한다.

`view_macaddress_*`, `view_display_v1`, `view_monitor_v1`,
`view_customfieldvalue_v1`, `view_devicecustomfields_v1`,
`view_softwaresuite_v1`, `view_service_v1`, `view_serviceinstance_v1`,
`view_operatingsystem_v1`, `view_hardwaremodel_v1`

MAC 주소는 `view_netport_v1.hwaddress` 에 있다. 커스텀필드 값은
`view_device_v2.vendor_custom_fields` 컬럼에 있다.

`view_display_v1`, `view_monitor_v1` 부재는 192.168.1.35와 192.168.2.68
양쪽에서 확인했다.

## 건수

| 뷰 | 건수 |
| --- | --- |
| `view_serviceinstance_v2` | 2025 |
| `view_software_v1` | 808 |
| `view_service_v2` | 759 |
| `view_softwareinuse_v1` | 585 |
| `view_ipaddress_v1` | 528 |
| `view_netport_v1` | 236 |
| `view_part_v1` | 86 |
| `view_device_v2` | 85 |
| `view_mountpoint_v1` | 84 |
| `view_deviceos_v1` | 75 |
| `view_os_v1` | 45 |
| `view_vendor_v1` | 41 |
| `view_enduser_v1` | 34 |
| `view_partmodel_v1` | 27 |
| `view_subnet_v1` | 25 |
| `view_vlan_v1` | 11 |
| `view_hardware_v1` | 7 |
| `view_appcomp_v1` | 6 |
| `view_remotecollector_v1` | 1 |
| `view_pdu_v1` | 1 |
| `view_room_v1` | 0 |
| `view_building_v1` | 0 |
| `view_asset_v1` | 0 |
| `view_customer_v1` | 0 |
| `view_rack_v1` | 0 |
| `view_deviceurl_v1` | 0 |
