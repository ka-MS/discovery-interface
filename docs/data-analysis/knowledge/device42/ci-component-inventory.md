# OS·Disk·Filesystem·IP 원천 조사

> 관측 2026-09-15 · D42 .68 / .35 · 읽기 전용 조회
> 재조회 [원천 형태·값 분포](../../exploration-queries/device42/ci-component-source.sql)

Computer 연관 범위의 연결 키·표본 건수는 [Computer 연관 수집 원천](computer-inventory.md)에 있다.
이 문서는 네 유형을 독립 CI로 다루는 데 필요한 사실만 다룬다. 원천 PK 유일성,
Computer 외 장비 연결분, 전체 컬럼, 값 보유율, 유형별 특이사항이다.

수치는 모두 `.68 / .35` 순이다. 한쪽 값만 옮겨 적지 않는다.

## 1. 뷰와 버전

2026-09-15 `.68`에서 개별 확인했다. 존재하지 않는 뷰는 500이며, 실행기는 첫 500에서
배치 전체를 중단하므로 미확인 뷰를 등록 쿼리에 함께 두지 않는다.

| 유형 | 사용 뷰 | 보강 뷰 | 상위 버전 |
| --- | --- | --- | --- |
| OS | `view_deviceos_v1` | `view_os_v1` → `view_vendor_v1` | `_v2` 없음 |
| Disk | `view_part_v1` | `view_partmodel_v1` → `view_vendor_v1` | `_v2` 없음 |
| Filesystem | `view_mountpoint_v2` | – | `_v3` 없음. `_v1`도 존재하나 `_v2`를 쓴다 |
| IP | `view_ipaddress_v2` | `view_subnet_v1`, `view_netport_v1` | `_v3` 없음. `_v1`도 존재하나 `_v2`를 쓴다 |

`view_part_v1`에는 `type_name`이 없다. 파트 종류는 `view_partmodel_v1.type_name`이므로
디스크 필터는 `view_partmodel_v1` 조인이 필요하다. 기존 문서의 `type_name='Hard Disk'`
표기는 조인한 모델 쪽 컬럼을 뜻한다.

### 컬럼

| 뷰 | 컬럼 |
| --- | --- |
| `view_deviceos_v1` | deviceos_pk, os_fk, os_version, os_arch, os_arch_name, os_version_no, os_license_key_id, os_license_key, discovered_license_key, os_license_key_count, the_key, os_license_key_notes, count_in_licensing, license_use_count, device_fk, first_added, last_edited, enrichai_details, os_name, eol, eos, base_release_date, release_start_date, release_end_date, extended_end_of_support_date, end_of_maintenance_date, extended_end_of_maintenance_date, discovered_data |
| `view_os_v1` | os_pk, name, vendor_fk, notes, category_id, category_name, licensed_count, oslicensingmodel_fk, licensing_model_name, track_licensed_count_by_keys, enrichaidata_fk, enrichai_details, last_changed, discovered_licensed_count |
| `view_part_v1` | part_pk, partmodel_fk, pcount, firmware, serial_no, description, checked_out_to, assignment, device_fk, room_fk, rack_fk, netport_fk, asset_fk, raid_type_id, raid_type_name, raid_group, asset_no, first_added, last_updated, date_changed, slot, partslot_fk, tags, details, last_changed |
| `view_partmodel_v1` | partmodel_pk, name, type_id, type_name, support_ports, description, cores, threads, speed, speed_unit, ramsize, ramsize_unit, ramtype, ramspeed, hdsize, hdsize_unit, hddtype_id, hddtype_name, hdrpm_id, hdrpm_name, connectivity_id, connectivity_name, media_type_id, media_type_name, connector_type_id, connector_type_name, length, modelno, partno, vendor_fk, location, width, height, port_prefix, port_template, notes, tags, last_changed, usage_type, verbose_name |
| `view_mountpoint_v2` | mountpoint_pk, mountpoint, identifier, filesystem, fstype_name, capacity, free_capacity, label, first_added, last_updated, device_fks |
| `view_ipaddress_v2` | ipaddress_pk, ip_address, ip_hybrid, label, subnet_fk, type_id, type, available, is_public, resource_fk, notes, first_added, last_edited, tags, netport_fk, details, last_changed, last_discovered, is_shared, device_fks, cloudinfrastructure_fk |
| `view_subnet_v1` | subnet_pk, name, verbose_name, description, number, network, mask_bits, gateway, range_begin, range_end, parent_vlan_fk, customer_fk, parent_subnet_fk, vrfgroup_fk, subnetcategory_fk, catgeory_name, category_name, catgeory_description, category_description, service_level_id, service_level, type_id, type, notes, allow_network_address, allow_broadcast_address, assigned, allocated, tags, last_changed, auto_add_ips, details |

`view_netport_v1`은 49개 컬럼이며 IP 보강에 쓰는 것은 netport_pk, port, name, normalized_port,
hwaddress, device_fk, type_name이다.

## 2. 원천 PK 유일성

네 유형 모두 원천 PK가 전건 유일하다. 행 수와 고유 PK 수가 같다.

| 유형 | PK | 행 수 | 고유 PK |
| --- | --- | ---: | ---: |
| OS | `deviceos_pk` | 88 / 79 | 88 / 79 |
| Filesystem | `mountpoint_pk` | 117 / 141 | 117 / 141 |
| IP | `ipaddress_pk` | 297 / 543 | 297 / 543 |
| Disk | `part_pk` | 23 / 19 | 23 / 19 |

`D42:<개체종류>:<원천PK>` 식별자를 그대로 쓸 수 있다.

## 3. Computer 연결분과 전체

Computer 판정은 `CiSourceFilter.COMPUTER`의 수집 필터와 같다. Device 본체의 Switch 확장 조건은 포함하지 않는다.

| 유형 | 전체 | Computer 연결 | 비율 |
| --- | ---: | ---: | ---: |
| OS | 88 / 79 | 26 / 63 | 30% / 80% |
| Filesystem | 117 / 141 | 117 / 141 | **전건** |
| IP | 297 / 543 | 50 / 97 | **17% / 18%** |
| Disk | 23 / 19 | 23 / 19 | **전건** |

Filesystem과 Disk는 Computer에만 붙는다. 수집 대상 범위를 나눌 실익이 없다.

IP는 전체의 5분의 1 미만만 Computer에 붙는다. 장비 연결 자체가 없는 행이 대부분이다.

| 연결 장비 수 | IP 건수 | Filesystem 건수 |
| ---: | ---: | ---: |
| 0 | 192 / 432 | 0 / 0 |
| 1 | 99 / 95 | 117 / 141 |
| 2 | 5 / 15 | 0 / 0 |
| 3 | 1 / 0 | 0 / 0 |
| 7 | 0 / 1 | 0 / 0 |

OS는 두 서버 차이가 크다. `.68`은 30%, `.35`는 80%다. 한쪽으로 결론내지 않는다.

## 4. 값 보유율

원천에는 NULL 대신 **빈 문자열**을 쓰는 컬럼이 있다. 문자열 컬럼은
`COUNT(NULLIF(TRIM(x), ''))`로 세어 공백을 제외했다. 단순 `COUNT()`로 세면
빈 문자열이 보유로 잡혀 실제보다 크게 나온다.

### OS — 88 / 79행

| 필드 | 보유 | 비고 |
| --- | ---: | --- |
| os_name | 88 / 79 | 전건. 제조사·제품·버전을 합친 정규화 문자열 |
| os_version | 41 / 37 | 시장 버전. 절반 이하 |
| os_version_no | 23 / 19 | 커널·빌드 문자열. 4분의 1 수준 |
| os_arch_name | 21 / 19 | `64-bit` 형태 |
| os_fk | 88 / 79 | 전건. OS 제품 참조 |
| eol | 48 / 22 | 지원 종료일 |
| eos | 47 / 22 | 서비스 종료일 |

`os_name`은 `IBM Red Hat Enterprise Linux 8.10`처럼 제조사·제품·버전이 합쳐진 값이다.
제조사만 필요하면 `os_fk` → `view_os_v1.vendor_fk` → `view_vendor_v1`을 쓴다.

`eol`·`eos`는 사업 범위 「나. EOS 관리」에 직접 대응하는 원천이다.
`enrichai_details` JSON에 `lifecycle_stage`, `support_policy`, 확장 지원 종료일이 더 있다.

### Filesystem — 117 / 141행

| 필드 | 보유 | 비고 |
| --- | ---: | --- |
| mountpoint | 117 / 141 | 전건. 마운트 경로 |
| identifier | 117 / 141 | 전건 |
| filesystem | 93 / 133 | 파일시스템 원천 문자열 |
| fstype_name | 115 / 141 | 종류명 |
| capacity | 114 / 140 | 전체 용량 |
| free_capacity | 106 / 129 | 여유 용량 |
| label | 24 / 8 | 낮다 |

용량 단위 컬럼이 없다. 단위 해석은 매핑에서 정해야 한다.

### IP — 297 / 543행

| 필드 | 보유 | 비고 |
| --- | ---: | --- |
| ip_address | 297 / 543 | 전건. `inet` 타입 |
| subnet_fk | 297 / 543 | 전건 |
| last_discovered | 297 / 543 | 전건 |
| label | 55 / 59 | 공백 제외. 대부분 빈 문자열 |
| netport_fk | 78 / 103 | 포트 연결은 4분의 1 수준 |
| is_shared | 6 / 16 | 공유 표시 |
| subnet.mask_bits | 297 / 543 | 전건 |
| subnet.gateway | 0 / 0 | 컬럼은 있으나 값 없음 |
| type | 1 / 1 | 사실상 비어 있음 |

`ip_address`는 `inet`이다. 문자열 함수 앞에 `CAST(... AS VARCHAR)`가 필요하고 그 결과에는
`/32`가 붙는다. 주소 값만 필요하면 `HOST()`를 쓴다.

### Disk — 23 / 19행

| 필드 | 보유 | 비고 |
| --- | ---: | --- |
| serial_no | 13 / 3 | 공백 제외. `.35`는 3건뿐 |
| pcount | 23 / 19 | 전건. **값이 전부 1** |
| firmware | **0 / 0** | 전건 빈 문자열. 채울 값 없음 |
| partmodel.name | 23 / 19 | 전건. 모델명 |
| partmodel.hdsize | 23 / 19 | 전건 |
| partmodel.hdsize_unit | 23 / 19 | 전건 |
| partmodel.hddtype_name | 10 / 2 | 낮다. `SSD`는 1 / 0건 |
| partmodel.media_type_name | 0 / 0 | 전건 없음 |
| partmodel.vendor_fk | 0 / 0 | 전건 없음. 제조사 보강 불가 |

## 5. 유형별 특이사항

**OS는 장비당 정확히 1개다.** 양쪽 서버 모두 `MAX(장비당 OS 수)=1`, 2개 이상인 장비 0대다.
1:1이지만 `eol`·`eos`를 가진 독립 관리 대상이므로 CI로 분리한다.

**`pcount`는 수량이 아니라 상수 1이다.** 두 서버 모두 값이 전부 1이다.
`view_part_v1` 한 행이 디스크 한 개다. 수량으로 해석해 곱하지 않는다.

**Filesystem의 `device_fks`는 배열이지만 항상 원소 1개다.** 두 서버 모두 최대 1이고 0인 행도 없다.
배열 조인의 중복 위험(ISSUE-9)이 현재 데이터에서는 실현되지 않는다. 다만 타입이 배열이므로
조회는 `= ANY(...)`로 쓰고, 장래 다중 연결에 대비해 `DISTINCT ON`을 유지할지는 설계에서 정한다.

**IP의 `device_fks`는 실제로 다중이다.** 최대 3 / 7개다. 장비 연결이 없는 행이 대부분이라
Computer 기준 수집은 전체의 5분의 1 미만을 가져온다.

**발견 시각 원천이 유형마다 다르다.** IP만 `last_discovered`를 전건 갖는다.
OS는 `first_added`·`last_edited`, Filesystem·Disk는 `first_added`·`last_updated`가 전건이지만
이들은 발견 시각이 아니라 레코드 변경 시각이다. ACTCI.LASTSCANDT 대응은 설계에서 정한다.

**제조사 보강 가능 여부가 갈린다.** OS는 `view_os_v1.vendor_fk`로 연결되지만,
Disk는 `view_partmodel_v1.vendor_fk`가 두 서버 모두 전건 비어 있어 제조사를 채울 수 없다.


## 6. 단위와 표본 값

`view_mountpoint_v2.capacity`에 단위 컬럼이 없다. 표본에서 NTFS `C:\`가 1,952,708이고
같은 장비의 물리 디스크가 2TB급이므로 **MB 단위**로 해석된다. 다른 단위 근거는 없다.

`view_partmodel_v1.hdsize`는 `hdsize_unit`과 짝이며 GB·TB가 섞인다.
`.68`은 GB 21건·TB 2건, `.35`는 GB 19건이다.

`view_ipaddress_v2.ip_address`는 `inet`이다. `CAST(... AS VARCHAR)`는 `192.168.2.127/32`처럼
실제 프리픽스가 아닌 `/32`를 붙인다. 주소만 필요하면 `HOST()`를 쓴다.
프리픽스 길이는 `view_subnet_v1.mask_bits`에서 가져온다. 표본에서 22·16·32가 관측된다.
