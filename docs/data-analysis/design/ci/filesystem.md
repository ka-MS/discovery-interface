# Filesystem CI 수집 설계

> 상태: 추천안 · 작성 2026-09-15. 분류·스펙·관계는 조사 근거에 기반한 추천이며 사용자 확정 전이다.
> 원천 관측: [OS·Disk·Filesystem·IP 원천 조사](../../knowledge/device42/ci-component-inventory.md)
> 타겟 관측: [OS·Disk·Filesystem·IP 분류 조사](../../knowledge/maximo/ci-component-classifications.md)
> 확정 매핑: [Filesystem 매핑](../../data-mapping/ci/types/filesystem.md)

수치는 `.68 / .35` 순이다.

## 1. 관리 단위 — 독립 CI

2026-09-15 사용자 합의다. 장비당 5.3 / 6.1개로 네 유형 중 다중성이 가장 높다.
ACTCISPEC 키가 `(ACTCINUM, ASSETATTRID, SECTION)`이라 Computer 스펙으로 담을 수 없다.

## 2. 분류 선택 — SYS.FILESYSTEM

| 후보 | CLASSSTRUCTUREID | 스펙 | 판정 |
| --- | --- | ---: | --- |
| **SYS.FILESYSTEM** | CCI10497 | 33 | **추천** |
| SYS.LOCALFILESYSTEM | CCI10588 | 33 | 부적합. 원격 마운트가 섞여 있다 |
| SYS.UNIX.UNIXFILESYSTEM | CCI10480 | 36 | 부적합. OS 계열 혼재 |
| SYS.WINDOWS.WINDOWSFILESYSTEM | CCI10614 | 33 | 부적합. 위와 같음 |
| SYS.NFSFILESYSTEM | CCI10481 | 38 | 부적합. NFS만 해당 |
| SYS.REMOTEFILESYSTEM | CCI10496 | 35 | 부적합. 원격만 해당 |

`SYS.FILESYSTEM`과 `SYS.LOCALFILESYSTEM`은 **속성 집합이 완전히 동일하다**.
이름만으로는 고를 수 없어 원천 값으로 판단했다.

관측된 `fstype_name` 분포다.

| 종류 | .68 | .35 | 성격 |
| --- | ---: | ---: | --- |
| overlay | 38 | 62 | 컨테이너 런타임 |
| xfs | 18 | 23 | 로컬 |
| NTFS | 14 | 4 | 로컬 |
| devtmpfs | 9 | 10 | 가상 |
| ext4 | 5 | 9 | 로컬 |
| VMFS | 4 | 9 | 클러스터 |
| **nfs / nfs4** | **2 / 3** | **5 / 1** | **원격** |
| squashfs | 0 | 8 | 읽기 전용 이미지 |
| iso9660, UDF, FAT32, vfat, ext3, efivarfs | 나머지 | 나머지 | 혼합 |

nfs·nfs4가 두 서버 모두 존재한다. 원격 파일시스템을 `SYS.LOCALFILESYSTEM`으로 적재하면
분류가 사실과 어긋난다. overlay·VMFS·squashfs도 로컬 디스크 파일시스템이 아니다.
범용 `SYS.FILESYSTEM`이 전체를 담을 수 있는 유일한 후보다.

종류별로 분류를 나누는 안도 있으나, `overlay`·`squashfs`·`VMFS`·`devtmpfs`에 대응하는
분류가 없어 일부를 적재할 수 없게 된다. Computer가 범용 분류를 쓴 선례와도 맞지 않는다.

### SYS.LOCALFILESYSTEM과의 차이 — 2026-09-15 재검토

두 분류의 **속성 33개가 완전히 동일하다.** 한쪽에만 있는 속성이 없다.
계층상으로도 부모-자식이 아니라 둘 다 `SYS.COMPUTERSYSTEM` 아래 형제다.
`SYS.NFSFILESYSTEM`·`SYS.REMOTEFILESYSTEM`·`SYS.UNIX.UNIXFILESYSTEM` 등도 같은 층에 있다.

관계 규칙은 `SYS.LOCALFILESYSTEM`이 완전한 상위집합이다. Computer→FS `CONTAINS`는 동일하고,
`SYS.LOCALFILESYSTEM`에만 `RELATION.STOREDON` 18개가 더 있다. 반대는 0개다.
대상은 `DEV.DISKPARTITION`·`DEV.STORAGEVOLUME`·`SYS.VMWARE.VMWAREDATASTORE` 등이다.

다만 **`DEV.DISKDRIVE`로 가는 규칙은 양쪽 다 없다.** 이 ETL이 적재하는 Disk CI와는
어느 쪽을 골라도 연결할 수 없다. STOREDON을 쓰려면 파티션·볼륨 CI를 새로 도입해야 한다.

제외 필터 적용 후 남은 원천의 약 30%가 로컬이 아니다.

| 성격 | .68 (69건) | .35 (60건) |
| --- | ---: | ---: |
| 로컬 (xfs·ext4·NTFS·vfat·FAT32·ext3) | 50 | 42 |
| 원격 (nfs·nfs4) | 5 | 6 |
| 이동식 (iso9660·UDF) | 8 | 3 |
| 클러스터 (VMFS) | 4 | 9 |

nfs 마운트를 `LOCALFILESYSTEM`으로 적재하면 분류가 사실과 어긋난다. `SYS.FILESYSTEM`을 유지한다.

**승격 범위는 별도 설정이 필요했다.** Maximo 기본 구성은 `CI.FILESYSTEM`을
`SYS.LOCALFILESYSTEM`에만 매핑한다. 2026-09-15 `SYS.FILESYSTEM` 매핑 행을 추가해
승격 경로를 열었다. 상세는 [CI 승격 범위](../../knowledge/maximo/ci-promotion-scope.md).

## 3. 스펙 대조표

**대조 기준은 `CI.FILESYSTEM`(CCI00026) 16개다.** 적재 대상은 `SYS.FILESYSTEM`이며
CI 기준 16개는 `SYS.FILESYSTEM` 고유 19개에서 ISPLACEHOLDER·LOCATIONTAG·SERVICEPACK을 뺀 것과 같다.
관측은 [분류 조사](../../knowledge/maximo/ci-component-classifications.md) 6절.

| ASSETATTRID | CI.FILESYSTEM | 자료형 | 원천 | 채택 | 비고 |
| --- | :---: | --- | --- | --- | --- |
| FILESYSTEM_MOUNTPOINT | ○ | ALN | `m.mountpoint` | 채택 | 전건. 7절 길이 주의 |
| FILESYSTEM_TYPE | ○ | ALN | `m.fstype_name` | 채택 | 115 / 141건 |
| FILESYSTEM_CAPACITY | ○ | NUMERIC | `m.capacity` | 채택 | 114 / 140건. 7절 단위 |
| FILESYSTEM_AVAILABLESPACE | ○ | NUMERIC | `m.free_capacity` | 채택 | 106 / 129건 |
| 나머지 CI 기준 12개 | ○ | – | – | 미채택 | inode·블록 크기·버전 등 원천 대응 없음 |
| MODELOBJECT_LABEL | ✗ | ALN | `m.label` | 검토 | 24 / 8건. 낮다 |
| MODELOBJECT_CDMSOURCE·SOURCETOKEN | ✗ | ALN | 연계 출처·원천 키 | 검토 | |

**채택한 네 개가 모두 CI 기준 안에 있다.** 기준선을 벗어난 추가가 없다.

원천 `filesystem`(파일시스템 원천 문자열, 93 / 133건)에 대응할 속성이 없다.
`fstype_name`이 종류를 담으므로 원문까지 넣을 자리가 필요하면 추가 속성 등록이 필요하다.
이번 범위에서는 제외를 추천한다.

`TOTALINODES`·`AVAILABLEINODES`·`FILESYSTEMBLOCKSIZE` 등 NUMERIC 9개는 원천에 없다.

## 4. 본체 필드

| ACTCI 컬럼 | 원천 | 비고 |
| --- | --- | --- |
| ACTCINUM | `D42:MOUNTPOINT:<mountpoint_pk>` | 5절 |
| ACTCINAME | `m.mountpoint` | 전건. 7절 길이 주의 |
| CLASSSTRUCTUREID | SYS.FILESYSTEM 조회값 | |
| DESCRIPTION | 대응 없음 | |
| LASTSCANDT | 부모 Computer의 `last_discovered` 추천 | `view_mountpoint_v2`에 발견 시각이 없다 |
| CHANGEBY / LANGCODE / CHANGEDATE / HASLD | Computer와 동일 규약 | |

## 5. 식별자

`ACTCINUM = D42:MOUNTPOINT:<mountpoint_pk>`. 두 서버 모두 전건 유일하다.

`device_fks`는 배열이지만 관측상 **항상 원소가 정확히 1개**다. 0개인 행도 2개 이상인 행도 없다.
따라서 조인에 의한 원천 PK 중복(ISSUE-9)이 현재 데이터에서는 발생하지 않는다.
타입이 배열이므로 조회는 `= ANY(...)`로 쓰고, 방어적으로 `DISTINCT ON (mountpoint_pk)`를
유지할지는 매핑에서 정한다. 유지 비용이 낮으므로 유지를 추천한다.

## 6. 관계 추천안

**확정이 아니다.** 규칙 존재는 관측 사실이고, 채택 여부는 결정 대상이다.

Computer가 출발점이다. 양쪽 Computer 분류 모두 같은 규칙을 갖는다.

| 출발 | 도착 | RELATIONNUM | CONTAINMENT | CARDINALITY |
| --- | --- | --- | ---: | --- |
| SYS.COMPUTERSYSTEM | SYS.FILESYSTEM | RELATION.CONTAINS | 1 | 1:N |
| SYS.VIRTUALCOMPUTERSYSTEM | SYS.FILESYSTEM | RELATION.CONTAINS | 1 | 1:N |

추천은 **RELATION.CONTAINS**다. 대안 규칙이 없고, 기수 1:N이 원천(장비당 5.3 / 6.1개)과 맞는다.

`device_fks`가 항상 원소 1개이므로 파일시스템 하나당 관계도 1건이다.

**주의:** `RELATION.CONTAINS`의 `USEWITH`는 `CI`이고 `ACTCI`가 아니다. `RELATIONRULES`의
분류쌍 조건은 만족하지만 실제 적재·UI 표시는 미검증이다. ISSUE-11에서 다룬다.

## 7. 길이와 단위

**마운트 경로가 길다.** 컨테이너 런타임 마운트는 다음 형태다.

```
/run/containerd/io.containerd.runtime.v2.task/k8s.io/1f895b758cd5bbda45d7143ae17e9e0a365b8e63050ea3f51db67a1a0ad88b19/rootfs
```

약 130자다. `ACTCI.ACTCINAME`은 192자, `ACTCISPEC.ALNVALUE`는 254자이므로 이 표본은 들어가지만
여유가 크지 않다. Computer 매핑은 길이 초과 시 해당 건이 통째로 실패한다. 절단·생략 규칙이 필요하다.

**용량 단위 컬럼이 원천에 없다.** 표본에서 NTFS `C:\`가 1,952,708이고 같은 장비의 물리 디스크가
2TB급이므로 **MB로 해석**된다. `FILESYSTEM_CAPACITY`·`AVAILABLESPACE` 템플릿에도 단위 지정이 없다.
원천 값을 그대로 넣고 단위 해석을 문서에 남기는 안과, MB 기준 `MEASUREUNITID`를 지정하는 안이 있다.
`MBYTE` 코드는 Computer 메모리 매핑에서 이미 쓰고 있어 존재가 확인됐다. 후자를 추천한다.

## 8. 수집 대상 범위

| 범위 | 건수 |
| --- | ---: |
| 전체 파일시스템 | 117 / 141 |
| Computer 연결 | 117 / 141 |

**전건이 Computer에 연결된다.** 범위를 나눌 실익이 없다.

다만 종류별 선별은 검토 대상이다. `overlay`가 38 / 62건으로 3분의 1에서 절반을 차지하는데,
컨테이너 런타임이 만드는 일시적 마운트이고 경로가 컨테이너 ID를 포함해 재기동 시 바뀐다.
원천 PK도 함께 바뀌면 매 실행마다 새 CI가 생기고 이전 CI는 갱신되지 않은 채 남는다.

선별 후보는 다음과 같다.

1. 전부 적재한다. 원천을 그대로 반영한다.
2. `overlay`·`devtmpfs`·`squashfs` 등 가상·일시 파일시스템을 제외한다.
3. 용량이 있는 것만 적재한다.

추천은 2다. 다만 제외 목록을 코드에 고정하기 전에 운영 D42의 종류 분포를 확인해야 한다.
현재 데이터로는 `.68` 117건 중 47건, `.35` 141건 중 80건이 제외 대상이 된다.

## 9. 미결

| 항목 | 상태 | 추적 |
| --- | --- | --- |
| 컨테이너·가상 파일시스템 선별 | 제외 추천. 목록 확정 필요 | ISSUE-8 |
| 마운트 경로 길이 초과 처리 | 절단·생략 규칙 필요 | ISSUE-11 |
| 용량 단위 | MB 해석. `MBYTE` 코드 지정 추천 | ISSUE-11 |
| `filesystem` 원문 속성 미등록 | 이번 범위 제외 추천 | ISSUE-11 |
| LASTSCANDT 원천 | 부모 Computer 값 사용 추천 | ISSUE-11 |
| 배열 `DISTINCT ON` 유지 여부 | 유지 추천 | ISSUE-9 |
| 승격 1:N 매핑 검증 | `CI.FILESYSTEM`에 ACTCI 분류 둘이 걸린 유일한 경우. 동작 미검증 | ISSUE-11 |
