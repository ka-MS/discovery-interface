# JSON 컬럼

> 관측 2026-09-01 · Device42 192.168.2.68
> 재조회 `../../exploration-queries/device42/json-column-shapes.sql`

Device42 뷰 여럿이 JSON 컬럼을 노출한다. 정규 컬럼에 없는 값을 찾을 때 여기를
뒤지게 되므로 성격을 정리한다.

## 어느 뷰에 있나

| 뷰 | JSON 컬럼 |
| --- | --- |
| `view_device_v2` | `details`, `vendor_custom_fields` |
| `view_netport_v1` | `details`, `vendor_custom_fields` |
| `view_part_v1` | `details` |
| `view_subnet_v1` | `details` |
| `view_deviceos_v1` | `discovered_data`, `enrichai_details` |
| `view_os_v1` | `enrichai_details` |
| `view_software_v1` | `details`, `enrichai_details` |
| `view_softwareinuse_v1` | `details`, `json`, `discovered_data`, `enrichai_details` |
| `view_ipaddress_v1` | 없음 |

## 세 종류다

성격이 다르므로 구분해서 다뤄야 한다.

| 컬럼 | 무엇인가 | 장비에서 온 값인가 |
| --- | --- | --- |
| `details` | 수집기가 받은 **원시 응답**을 그대로 담은 버킷 | 예 |
| `discovered_data` | 정규 컬럼에 넣기 **전 원본** | 예 |
| `enrichai_details` | 외부 카탈로그와 대조해 붙인 **보강 정보** | **아니오** |

## `details` — 수집 방식이 키를 결정한다

수집 방식마다 키 집합이 완전히 다르다. **겹치는 키가 하나도 없다.**

| 수집 방식 | 키 수 | 키 |
| --- | --- | --- |
| AWS EC2 | 17 | `account_id` `ami_launch_index` `arch` `ebs_optimized` `hypervisor` `iam_instance_profile` `image_id` `item_id` `key_name` `launch_time` `monitoring_state` `platform_details` `private_dns_name` `public_dns_name` `security_groups` `subnet_id` `vpc_id` |
| SNMP (네트워크) | 13 | `advertised_dp_info` `contact` `description` `fw_device_type` `json_node_type` `layers` `ports` `slots` `snmp_class` `snmp_location` `switch_id` `uptime` `vtp_domain` |
| Windows | 13 | `currentversion` `displayversion` `domain` `domain_role` `domain_role_id` `editionid` `last_boot_time` `tpm_activated` `tpm_enabled` `tpm_manufacturer_version` `tpm_version` `ubr` `windows_local_admin_disabled` |

AWS API 응답, SNMP MIB 값, Windows 레지스트리·WMI 값을 그대로 옮긴 것이다.
`snmp_class` 값이 그 근거다.

```
"snmp_class": "SNMP::Info::Layer3::CatalystStacked"
```

Perl `SNMP::Info` 모듈의 클래스명이다. 수집기 구현이 그대로 드러나 있다.

**스캔되지 않은 장비는 통째로 비어 있다.**

| type | 장비 | `details` 보유 | 최대 키 |
| --- | --- | --- | --- |
| virtual | 51 | 25 | 17 |
| physical | 5 | 5 | 13 |
| cluster | 2 | 2 | 12 |
| unknown | 37 | **0** | 0 |

`unknown` 37대는 IP 만 등록됐거나 수기로 만든 장비다.

## `view_part_v1.details` — 파트 종류가 키를 결정한다

| `type_name` | 키 |
| --- | --- |
| CPU | `architecture` `byte_order` `cpu_family_id` `cpu_model_id` `cpu_op_mode` `cpu_status` `enabled_cores` `hyperthreading_enabled` `hypervisor_vendor` `number_of_logical_processors` `online_cpu_list` `part_number` `stepping` `total_threads` `virtualization_type` 등 |
| Hard Disk | `drive_type` `fs_type` `hddsize` `model` `mount_point` `removable` `rotational` `scsi_bus` `scsi_logical_unit` `scsi_port` `scsi_target_id` `state` |
| RAM | `configured_clock_speed` `data_width` `form_factor` `ramsize` `total_width` |
| printer_marker | `marker_name` `marker_type` `marker_capacity` `marker_current_level` |
| printer_input | `input_name` `input_type` `input_capacity` `input_current_level` |
| printer_output | `output_name` `output_type` `output_capacity` `output_current_level` |

CPU 키는 리눅스 `lscpu` 출력, 프린터 키는 프린터 MIB 항목이다.

## `discovered_data` vs `enrichai_details`

같은 행에서 대비하면 성격이 분명하다.

```json
// discovered_data — 수집 원본
{"os_name": "Microsoft Windows Server 2019 Standard", "os_type": 64,
 "os_version": "D42_NULL", "os_version_no": "10.0.17763"}

// enrichai_details — 외부 보강
{"meta": {"vendor": "Microsoft", "os_name": "Windows Server",
          "os_family": "Windows", "market_version": "2019",
          "golden_record_id": "5c9b3fd7ea6e..."},
 "enrichment": {"eol": "...", "eos": "...", "lifecycle_stage": "..."},
 "normalized": "Microsoft Windows Server 2019 Standard"}
```

`discovered_data` 에는 `"D42_NULL"` 같은 Device42 내부 센티널 문자열이 그대로
남는다. `enrichai_details` 의 `golden_record_id` 는 외부 카탈로그의 레코드 ID 이고
EOL·EOS 는 장비가 알 수 없는 정보다.

| `view_deviceos_v1` | 행 |
| --- | --- |
| 전체 | 79 |
| `discovered_data` 보유 | 79 |
| `enrichai_details` 보유 | **54** |

보강은 외부 카탈로그에 매칭된 것만 붙는다.

## 사용 지침

**규격이 없다.** 스키마도 문서도 없고 수집 방식이 키를 정한다. 계약이 아니라
구현 노출이므로 Device42 버전이나 수집기가 바뀌면 키가 바뀔 수 있다.

써도 되는 경우는 세 조건을 모두 만족할 때다.

- 정규 컬럼에 대응하는 값이 없다
- 대상 장비군이 한정된다
- 없을 때 쓸 폴백이 있다

`DEPLOYEDASSET.DOMAINNAME` 이 그 예다. `details->>'domain'` 은 Windows 장비에만
있고(`.68` 4/31, `.35` 1/69) 없으면 `UNKNOWN` 으로 떨어진다.

쓰면 안 되는 경우다.

- 전건 채워질 것으로 기대하는 값. `unknown` 타입은 통째로 비어 있다
- 키 존재를 전제로 하는 매핑. `COALESCE(details->>'키', 폴백)` 형태로만 쓴다
- `enrichai_details` 를 발견된 사실로 적재하는 것. 외부 매칭 결과라 재실행 시
  값이 달라질 수 있고 성격이 다르다

매핑에 쓸 때는 문서에 충전율 실측치를 함께 남긴다.

## 조회 방법

DOQL 에서 되는 것과 안 되는 것이 있다.

```sql
-- 동작한다
d.details->>'domain'
LATERAL jsonb_object_keys(d.details::jsonb) AS k
(SELECT COUNT(*) FROM jsonb_object_keys(d.details::jsonb))
SUBSTR(CAST(o.enrichai_details AS VARCHAR), 1, 300)
CAST(d.details AS VARCHAR) LIKE '%domain%'

-- 500 이다
CAST(d.details AS VARCHAR) <> '{}'
```

비어 있는지는 키 개수로 판정한다. 다른 DOQL 제약은 `doql-constraints.md` 참조.
