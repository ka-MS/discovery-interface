# Disk CI 수집 설계

> 상태: 추천안 · 작성 2026-09-15. 분류·스펙·관계는 조사 근거에 기반한 추천이며 사용자 확정 전이다.
> 원천 관측: [OS·Disk·Filesystem·IP 원천 조사](../../knowledge/device42/ci-component-inventory.md)
> 타겟 관측: [OS·Disk·Filesystem·IP 분류 조사](../../knowledge/maximo/ci-component-classifications.md)
> 확정 매핑: [Disk 매핑](../../data-mapping/ci/types/disk.md)

수치는 `.68 / .35` 순이다.

## 1. 관리 단위 — 독립 CI

2026-09-15 사용자 합의다. 장비당 1.3 / 1.0개이고 `view_part_v1` 한 행이 디스크 한 개다.
`pcount`는 두 서버 모두 값이 전부 1이므로 수량이 아니다. 곱하지 않는다.

## 2. 분류 선택 — DEV.DISKDRIVE

| 후보 | CLASSSTRUCTUREID | 스펙 | 판정 |
| --- | --- | ---: | --- |
| **DEV.DISKDRIVE** | CCI10631 | 38 | **추천** |
| DEV.DISKPARTITION | CCI10479 | 32 | 부적합. 파티션은 디스크 하위 개념이며 원천이 파티션 단위가 아니다 |
| DEV.STORAGEVOLUME | CCI10626 | 41 | 부적합. 논리 볼륨이며 물리 장치가 아니다 |

원천은 `view_partmodel_v1.type_name='Hard Disk'`인 물리 파트다. 모델명·시리얼·용량·인터페이스 종류를
갖는 물리 저장 장치이므로 `DEV.DISKDRIVE`가 맞는다. 이 분류는 `MEDIAACCESSDEVICE_` 9개와
`DISKDRIVE_` 15개를 갖는다.

## 3. 스펙 대조표

**CI 계열에 대조 분류가 없다.** `CIROOT` 자식 24개에 디스크·저장 장치 분류가 없고,
`CI.%DISK%`·`CI.%MEDIA%`·`CI.%STORAGE%`·`CI.%DRIVE%`로 CI 적용 분류를 전수 검색해도
`CI.IPSTORAGESWITCHFUNCTION` 한 건뿐인데 스위치 기능이라 무관하다.
관측은 [분류 조사](../../knowledge/maximo/ci-component-classifications.md) 6절.

다른 세 유형과 달리 기준선 없이 ACTCI 쪽 `DEV.DISKDRIVE`만 보고 골랐다.
승격 대상 CI 분류가 없다는 뜻이기도 하다. 9절 미결에 남긴다.

| ASSETATTRID | 자료형 | 원천 | 채택 | 비고 |
| --- | --- | --- | --- | --- |
| MEDIAACCESSDEVICE_MODEL | ALN | `pm.name` | 채택 | 23 / 19건 |
| MEDIAACCESSDEVICE_SERIALNUMBER | ALN | `p.serial_no` | 채택 | 13 / 3건. 공백 제외 |
| MEDIAACCESSDEVICE_NAME | ALN | `pm.name` | 보류 | MODEL과 중복. 본체명과도 중복 |
| DISKDRIVE_DISKSIZE | NUMERIC | `pm.hdsize` + `pm.hdsize_unit` | 채택 | 7절 단위 |
| DISKDRIVE_ISSOLIDSTATE | ALN | `pm.hddtype_name='SSD'` | 보류 | 판정 근거가 1 / 0건뿐 |
| MEDIAACCESSDEVICE_MANUFACTURER | ALN | `pm.vendor_fk` | 미채택 | **전건 없음** |
| DISKDRIVE_VENDOR | ALN | 위와 같음 | 미채택 | **전건 없음** |
| DISKDRIVE_REVISION | ALN | `p.firmware` | 미채택 | **전건 빈 문자열** |
| MEDIAACCESSDEVICE_TYPE | ALN | `pm.media_type_name` | 미채택 | **전건 없음** |
| MODELOBJECT_CDMSOURCE | ALN | 상수 `Device42` | 검토 | |
| MODELOBJECT_SOURCETOKEN | ALN | `D42:PART:<pk>` | 검토 | |
| 나머지 20개 | – | – | 미채택 | 원천 대응 없음 |

**실제로 채울 수 있는 속성이 3개뿐이다.** 모델명·시리얼·용량이다. 디스크 CI 하나에
속성 3개는 독립 CI로 관리할 값어치를 다시 볼 만하다. 8절에서 다룬다.

## 4. 본체 필드

| ACTCI 컬럼 | 원천 | 비고 |
| --- | --- | --- |
| ACTCINUM | `D42:PART:<part_pk>` | 5절 |
| ACTCINAME | `pm.name` | 모델명. `sda 500 GB`처럼 장치명인 경우도 있다 |
| CLASSSTRUCTUREID | DEV.DISKDRIVE 조회값 | |
| DESCRIPTION | `p.description` | 값 분포 미확인 |
| LASTSCANDT | 부모 Computer의 `last_discovered` 추천 | `view_part_v1`에 발견 시각이 없다 |
| CHANGEBY / LANGCODE / CHANGEDATE / HASLD | Computer와 동일 규약 | |

`ACTCINAME`이 모델명이면 같은 모델의 디스크 여러 개가 같은 이름을 갖는다.
`ACTCINUM`이 원천 PK 기반이라 CI는 구분되지만 화면에서는 구별이 어렵다.
장비명이나 슬롯(`p.slot`)을 붙일지 검토 대상이다.

## 5. 식별자

`ACTCINUM = D42:PART:<part_pk>`. `part_pk`는 두 서버 모두 전건 유일하다.

`view_part_v1`은 CPU·RAM·GPU도 담는 공용 뷰다. 개체 종류를 `PART`로 두면 장래 CPU CI를
추가할 때 같은 접두어를 쓰게 된다. 원천 PK가 뷰 전체에서 유일하므로 충돌은 없지만,
종류를 구분하려면 `D42:DISK:<part_pk>`를 쓸 수도 있다. 결정 대상이다.

## 6. 관계 추천안

**확정이 아니다.** 규칙 존재는 관측 사실이고, 채택 여부는 결정 대상이다.

Computer가 출발점이다. 양쪽 Computer 분류 모두 같은 규칙을 갖는다.

| 출발 | 도착 | RELATIONNUM | CONTAINMENT | CARDINALITY |
| --- | --- | --- | ---: | --- |
| SYS.COMPUTERSYSTEM | DEV.DISKDRIVE | RELATION.CONTAINS | 1 | 1:N |
| SYS.VIRTUALCOMPUTERSYSTEM | DEV.DISKDRIVE | RELATION.CONTAINS | 1 | 1:N |

추천은 **RELATION.CONTAINS**다. 대안 규칙이 없고, 기수 1:N이 원천(장비당 디스크 1.3 / 1.0개)과
맞으며 `CONTAINMENT=1`이라 Computer 하위 부품으로 표현된다.

출발이 Computer, 도착이 Disk다. OS는 방향이 반대(OS가 출발)이므로 적재 시 혼동하지 않는다.

**주의:** `RELATION.CONTAINS`의 `USEWITH`는 `CI`이고 `ACTCI`가 아니다. `RELATIONRULES`의
분류쌍 조건은 만족하지만 실제 적재·UI 표시는 미검증이다. ISSUE-11에서 다룬다.

## 7. 단위

`DISKDRIVE_DISKSIZE`는 NUMERIC이고 템플릿에 측정 단위가 지정돼 있지 않다.
원천은 `hdsize` + `hdsize_unit` 쌍이며 `.68`은 GB 21건·TB 2건, `.35`는 GB 19건이다.

Computer의 메모리·CPU 속도는 값과 원천 단위를 함께 매핑하고 `MEASUREUNITID`에 코드를 넣었다.
같은 방식을 쓰려면 `GBYTE`·`TBYTE` 코드가 `MEASUREUNIT`에 있어야 한다. `TBYTE` 존재는 미확인이다.
없으면 GB로 정규화해 저장하고 단위 코드는 `GBYTE`로 고정하는 안이 대안이다.

## 8. 수집 대상 범위

| 범위 | 건수 |
| --- | ---: |
| 전체 디스크 | 23 / 19 |
| Computer 연결 디스크 | 23 / 19 |

**전건이 Computer에 연결된다.** 범위를 나눌 실익이 없다.

다만 건수 자체가 적다. 표본 Computer가 95 / 85대인데 디스크를 가진 장비는 18 / 19대뿐이다.
나머지 장비는 디스크 파트가 수집되지 않았다. 사업 범위 「서버 — Disk」 요구를 이 원천으로
충족할 수 있는지는 운영 D42에서 다시 확인해야 한다. 파일시스템은 22 / 23대에서 117 / 141건이
수집되므로 용량 정보는 파일시스템 쪽이 넓다.

## 9. 미결

| 항목 | 상태 | 추적 |
| --- | --- | --- |
| 채울 속성이 3개뿐 | 독립 CI 유지 여부 재확인 | ISSUE-8 |
| CI 계열 대조 분류 없음 | 승격 대상 CI 분류가 없다. `CITEMPLATE`에도 0행이라 **승격 불가**가 확인됐다. 관리 단위 재검토 근거 | ISSUE-8 |
| 원천 커버리지 부족 | 18 / 19대만 디스크 보유 | ISSUE-8 |
| 용량 단위 코드 | `TBYTE` 존재 미확인. GB 정규화 대안 | ISSUE-11 |
| 식별자 접두어 | `PART` 대 `DISK` | ISSUE-11 |
| LASTSCANDT 원천 | 부모 Computer 값 사용 추천 | ISSUE-11 |
| ACTCINAME 중복 | 모델명만으로는 화면 구별 어려움 | ISSUE-11 |
