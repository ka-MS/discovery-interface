# OS·Disk·Filesystem·IP 분류 조사

> 관측 2026-09-15 · Maximo BLUDB · 읽기 전용 조회
> 재조회 [분류 후보·스펙·관계 규칙](../../exploration-queries/maximo/ci-component-classifications.sql)

Computer 분류 조사는 [Computer 관련 분류 조사](computer-classification-specs.md)에 있다.
분류 모델 구조와 컬럼은 [CI 분류 모델](ci-classification.md)을 따른다.

ACTCI 적용 분류는 1,030개다. 이름으로 후보를 좁힌 뒤 각 후보의 스펙을 대조했다.

## 1. 유형별 후보

키워드 검색으로 94개 분류가 걸렸다. 유형별로 의미가 맞는 것만 남긴 결과다.

| 유형 | 후보 | CLASSSTRUCTUREID | 스펙 수 | ACTCI 적용 스펙 |
| --- | --- | --- | ---: | ---: |
| OS | **SYS.OPERATINGSYSTEM** | CCI10538 | 50 | 50 |
| OS | SYS.WINDOWS.WINDOWSOPERATINGSYSTEM | CCI10519 | 50 | 50 |
| OS | SYS.I5OS.I5OPERATINGSYSTEM | CCI10500 | 79 | 79 |
| Filesystem | **SYS.FILESYSTEM** | CCI10497 | 33 | 33 |
| Filesystem | SYS.LOCALFILESYSTEM | CCI10588 | 33 | 33 |
| Filesystem | SYS.UNIX.UNIXFILESYSTEM | CCI10480 | 36 | 36 |
| Filesystem | SYS.WINDOWS.WINDOWSFILESYSTEM | CCI10614 | 33 | 33 |
| Filesystem | SYS.NFSFILESYSTEM | CCI10481 | 38 | 38 |
| Filesystem | SYS.REMOTEFILESYSTEM | CCI10496 | 35 | 35 |
| IP | **NET.IPADDRESS** | CCI10810 | 22 | 22 |
| IP | NET.IPINTERFACE | CCI10490 | 19 | 19 |
| Disk | **DEV.DISKDRIVE** | CCI10631 | 38 | 38 |
| Disk | DEV.DISKPARTITION | CCI10479 | 32 | 32 |
| Disk | DEV.STORAGEVOLUME | CCI10626 | 41 | 41 |

굵은 항목이 추천이다. 선택 이유는 각 유형의 수집 구성안에 있다.

후보가 0개인 유형은 없다. 네 유형 모두 ACTCI 적용 설정이 있는 범용 분류가 존재한다.

## 2. 후보 스펙의 공통 상태

조사한 11개 후보 분류의 스펙 365건 전체가 다음을 만족한다.

- `ORGID`·`SITEID`가 모두 NULL이다. 전역 템플릿이므로 현재 적재 코드가 그대로 읽는다.
- `CLASSSPECUSEWITH`의 `OBJECTNAME='ACTCI'` 행이 있고 `USEINSPEC=1`이다.
- `MANDATORY`가 전건 0이다. 필수 속성이 없다.

모든 후보에 `MODELOBJECT_*` 14개가 공통으로 붙는다. CDM 공통 속성이며
`MODELOBJECT_DESCRIPTION`, `MODELOBJECT_DISPLAYNAME`, `MODELOBJECT_LABEL`,
`MODELOBJECT_CDMSOURCE`, `MODELOBJECT_SOURCETOKEN`이 적재 후보다.
Computer 분류도 같은 14개를 갖는다.

## 3. 추천 분류의 속성

`MODELOBJECT_*` 공통 14개는 표에서 뺐다.

### SYS.OPERATINGSYSTEM — 고유 36개

OPERATINGSYSTEM_ 접두어다.

| 속성 | 자료형 | 원천 대응 |
| --- | --- | --- |
| OSNAME | ALN | `view_deviceos_v1.os_name` |
| OSVERSION | ALN | `os_version` |
| VERSIONSTRING | ALN | `os_version` 또는 `os_name` |
| KERNELVERSION | ALN | `os_version_no` |
| KERNELARCHITECTURE | ALN | `os_arch_name` |
| NAME | ALN | `os_name` |
| BUILDLEVEL, SERVICEPACK, CHARSET, LOCALE, FQDN, OSMODE, MANAGEDSYSTEMNAME, ASSETID, ASSETTAG, SYSTEMGUID, CICATEGORY, GENERALCIROLE, ISPLACEHOLDER, LOCATIONTAG, CONFIGLASTUPDATE | ALN | 대응 없음 |
| MAJORVERSION, RELEASE, LEVEL, MODIFIER, WORDSIZE, BOOTTIME, CURRENTTIME, OSID, OSCONFIDENCE, CIROLE, LIFECYCLESTATE, LASTLIFECYCLESTATETIME, LASTAUDITSTATE, LASTAUDITTIME, VIRTUALMEMORYSIZE | NUMERIC | 대응 없음 |

**EOL·EOS에 대응하는 속성이 없다.** 원천은 `eol`·`eos`를 48/22, 47/22건 갖는데
`SYS.OPERATINGSYSTEM`에 수명주기 날짜 속성이 없다. `LIFECYCLESTATE`는 NUMERIC 코드다.
사업 범위 「나. EOS 관리」와 직접 연결되는 항목이므로 추가 속성 후보로 남긴다.

### SYS.FILESYSTEM — 고유 19개

FILESYSTEM_ 접두어다.

| 속성 | 자료형 | 원천 대응 |
| --- | --- | --- |
| MOUNTPOINT | ALN | `view_mountpoint_v2.mountpoint` |
| TYPE | ALN | `fstype_name` |
| CAPACITY | NUMERIC | `capacity` |
| AVAILABLESPACE | NUMERIC | `free_capacity` |
| VERSIONSTRING, BUILDLEVEL, SERVICEPACK, MANAGEDSYSTEMNAME, ISPLACEHOLDER, LOCATIONTAG | ALN | 대응 없음 |
| TOTALINODES, AVAILABLEINODES, FILESYSTEMBLOCKSIZE, MAXBLOCKS, MAXFILESIZE, MAJORVERSION, RELEASE, LEVEL, MODIFIER | NUMERIC | 대응 없음 |

`SYS.LOCALFILESYSTEM`의 속성 집합은 `SYS.FILESYSTEM`과 **완전히 동일하다**.
이름만으로는 고를 수 없어 원천 값 분포로 판단했다. 3절 결론은 유형별 설계 문서에 있다.

원천 `filesystem`(파일시스템 원천 문자열)과 `label`에 대응할 속성이 없다.
`capacity`·`free_capacity`의 단위 컬럼이 원천에 없고 Target 속성에도 단위 지정이 없다.

### NET.IPADDRESS — 고유 8개

IPADDRESS_ 접두어다. 후보 중 가장 작다.

| 속성 | 자료형 | 원천 대응 |
| --- | --- | --- |
| STRINGNOTATION | ALN | `view_ipaddress_v2.ip_address` |
| DOTNOTATION | ALN | `HOST(ip_address)` |
| ADDRESSSPACE | ALN | 대응 없음 |
| ADDRESSTYPE | NUMERIC | IPv4/IPv6 코드. 코드 규약 미확인 |
| BYTENOTATION | ALN | 대응 없음 |
| ISPLACEHOLDER, LOCATIONTAG, MANAGEDSYSTEMNAME | ALN | 대응 없음 |

**서브넷 마스크·게이트웨이에 대응하는 속성이 없다.** 원천 `mask_bits`는 전건 보유인데
`NET.IPADDRESS`에 담을 자리가 없다. 서브넷은 `NET.IPNETWORK`(CCI10390, 25개)가 별도 분류다.

### DEV.DISKDRIVE — 고유 24개

DISKDRIVE_ 15개와 MEDIAACCESSDEVICE_ 9개다.

| 속성 | 자료형 | 원천 대응 |
| --- | --- | --- |
| MEDIAACCESSDEVICE_SERIALNUMBER | ALN | `view_part_v1.serial_no`. 공백 제외 13 / 3건 |
| MEDIAACCESSDEVICE_MODEL | ALN | `view_partmodel_v1.name` |
| MEDIAACCESSDEVICE_NAME | ALN | `view_partmodel_v1.name` |
| MEDIAACCESSDEVICE_MANUFACTURER | ALN | `partmodel.vendor_fk` → 전건 비어 있음 |
| DISKDRIVE_VENDOR | ALN | 위와 같음. 채울 값 없음 |
| DISKDRIVE_REVISION | ALN | `view_part_v1.firmware` → 전건 빈 문자열 |
| DISKDRIVE_DISKSIZE | NUMERIC | `partmodel.hdsize` + `hdsize_unit` |
| DISKDRIVE_ISSOLIDSTATE | ALN | `partmodel.hddtype_name='SSD'` 판정. 보유 10/2건 |
| MEDIAACCESSDEVICE_TYPE, STATUS, ISPLACEHOLDER, LOCATIONTAG, MANAGEDSYSTEMNAME | ALN | 대응 없음 |
| DISKDRIVE_ANSIT10ID, SUPPORTSVARIABLESPEED, SOLIDSTATEARCHITECTURE | ALN·NUMERIC | 대응 없음 |
| DISKDRIVE_DISKSPEED, SPINSPEED, DATATRANSFERRATE, TOTALIOPS, FORMFACTORSIZE, DISKINTERFACETYPE, PRIMARYDISKINTERFACETYPE, SECONDARYDISKINTERFACETYPE | NUMERIC | 대응 없음 |

`DISKDRIVE_DISKSIZE`는 NUMERIC이고 템플릿에 측정 단위가 지정돼 있지 않다.
원천 `hdsize_unit`은 GB/TB가 섞여 있다. 단위 정규화 규칙이 필요하다.

## 4. 미등록 속성

속성명 검색 185건 중 네 유형에 쓸 만한 전역 속성은 위 표가 전부다.
원천에 값이 있는데 대응 속성이 없는 항목은 다음과 같다.

| 유형 | 원천 필드 | 상태 |
| --- | --- | --- |
| OS | `eol`, `eos` | 대응 속성 없음. 사업 범위 EOS 관리 대상 |
| Filesystem | `filesystem`, `label` | 대응 속성 없음 |
| IP | 서브넷 `mask_bits` | `NET.IPADDRESS`에 자리 없음. `NET.IPNETWORK` 별도 분류 |
| Disk | `hddtype_name` 원문 | `ISSOLIDSTATE` 불리언으로만 담긴다 |

`DISKSIZE`라는 NUMERIC 속성이 `ORGID='EAGLENA'`로 따로 있다. 조직 전용 정의이므로
적재 코드의 전역 템플릿 조회 대상이 아니다. 근거는 [CI 분류 모델](ci-classification.md)의 조직·사이트 범위 절.

## 5. Computer와의 관계 규칙

`ACTCIRELATION`의 유효성은 `RELATIONNUM` + 출발 `CLASSSTRUCTUREID` + 도착 `CLASSSTRUCTUREID`가
`RELATIONRULES`에 있어야 성립한다. 근거는 [CI 모델](ci-model.md).
추천 분류 네 개와 Computer 두 분류의 조합을 양방향으로 조회했다.

### 규칙이 있는 조합

| 출발 | 도착 | RELATIONNUM | CONTAINMENT | CARDINALITY | REVREL | SWAPPED |
| --- | --- | --- | ---: | --- | ---: | ---: |
| SYS.COMPUTERSYSTEM | DEV.DISKDRIVE | RELATION.CONTAINS | 1 | 1:N | 0 | 0 |
| SYS.COMPUTERSYSTEM | SYS.FILESYSTEM | RELATION.CONTAINS | 1 | 1:N | 0 | 0 |
| SYS.VIRTUALCOMPUTERSYSTEM | DEV.DISKDRIVE | RELATION.CONTAINS | 1 | 1:N | 0 | 0 |
| SYS.VIRTUALCOMPUTERSYSTEM | SYS.FILESYSTEM | RELATION.CONTAINS | 1 | 1:N | 0 | 0 |
| SYS.OPERATINGSYSTEM | SYS.COMPUTERSYSTEM | RELATION.INSTALLEDON | 1 | N:1 | 1 | 1 |
| SYS.OPERATINGSYSTEM | SYS.COMPUTERSYSTEM | RELATION.RUNSON | 0 | 1:1 | 1 | 1 |
| SYS.OPERATINGSYSTEM | SYS.VIRTUALCOMPUTERSYSTEM | RELATION.INSTALLEDON | 1 | N:1 | 1 | 1 |
| SYS.OPERATINGSYSTEM | SYS.VIRTUALCOMPUTERSYSTEM | RELATION.RUNSON | 0 | 1:1 | 1 | 1 |

Disk·Filesystem은 Computer가 출발점이고, OS는 OS가 출발점이다. 방향이 서로 반대다.
OS는 두 코드가 있다. `INSTALLEDON`은 포함 관계에 N:1, `RUNSON`은 비포함에 1:1이다.

### IP는 직접 규칙이 없다

`SYS.*COMPUTERSYSTEM`과 `NET.IPADDRESS` 사이에는 **양방향 모두 규칙이 0건**이다.
`NET.IPADDRESS`가 걸린 규칙은 135건 있으나 Computer와의 직접 조합은 없다.

CDM이 정의한 경로는 인터페이스를 거친다.

| 출발 | 도착 | RELATIONNUM | CONTAINMENT | CARDINALITY |
| --- | --- | --- | ---: | --- |
| SYS.COMPUTERSYSTEM | NET.IPINTERFACE | RELATION.CONTAINS | 1 | 1:N |
| SYS.VIRTUALCOMPUTERSYSTEM | NET.IPINTERFACE | RELATION.CONTAINS | 1 | 1:N |
| NET.IPINTERFACE | NET.IPADDRESS | RELATION.BINDSTO | 0 | 1:1 |
| NET.IPADDRESS | NET.IPNETWORK | RELATION.MEMBEROF | 0 | 1:1 |

즉 `Computer → IPINTERFACE → IPADDRESS`다. 원천도 같은 모양이다.
`view_ipaddress_v2.netport_fk` → `view_netport_v1.device_fk`가 이 경로에 대응하며,
`subnet_fk`는 `NET.IPNETWORK`(`RELATION.MEMBEROF`)에 대응한다.

다만 원천에서 `netport_fk`를 가진 IP는 78 / 103건뿐이다. 나머지는 인터페이스를 거치지 않는다.
IP를 어떻게 연결할지는 수집 구성안에서 정한다.

### RELATION 정의의 USEWITH

조합에 쓰인 관계 코드 7개의 `RELATION.USEWITH`는 **전부 `CI`**다. `ACTCI`는 없다.

| RELATIONNUM | TYPE | USEWITH |
| --- | --- | --- |
| RELATION.BOOTSFROM, CONTAINS, DEFINEDUSING, INSTALLEDON, REALIZES, RUNSON, VIRTUALIZES | UNIDIRECTIONAL | CI |

`RELATION`에 `USEWITH='ACTCI'` 행이 0건이라는 기존 관측([CI 모델](ci-model.md))과 일치한다.
`RELATIONRULES`의 분류쌍 규칙은 존재하므로 `ACTCIRELATION`의 분류쌍 조건은 만족하지만,
관계 정의 자체가 ACTCI용으로 표시돼 있지 않다. 실제 적재·UI 표시 가능 여부는 미검증이다.
이 차이는 ISSUE-11에서 다룬다.

## 6. CI 계열 대조 기준

> 관측 2026-09-15 · 재조회는 같은 쿼리 파일의 `ci-root-children`·`ci-side-specs`·`ci-side-storage-search` 블록

분류 계열이 둘이다. 적재 대상은 `ACTUALCIROOTCLASS` 아래의 ACTCI 분류지만,
**수집할 속성을 고르는 대조 기준은 `CIROOT` 아래의 CI 분류**다.
Computer가 `CI.COMPUTERSYSTEM` 18개를 기준선으로 삼은 것과 같다.
근거는 [Computer 관련 분류 조사](computer-classification-specs.md) 1절.

`CIROOT`(CCI00001) 아래에 24개 분류가 있다. 네 유형과 관련된 것은 셋이다.

| 유형 | CI 계열 대조 분류 | CLASSSTRUCTUREID | 스펙 수 |
| --- | --- | --- | ---: |
| OS | CI.OS | CCI00013 | 7 |
| Filesystem | CI.FILESYSTEM | CCI00026 | 16 |
| IP | CI.IPADDRESS | CCI00011 | 6 |
| Disk | **없음** | – | – |

`CI.OS` 7개는 FQDN, KERNELVERSION, NAME, OSCONFIDENCE, OSMODE, OSNAME, OSVERSION이다.
`ACTCI` 쪽 `SYS.OPERATINGSYSTEM`이 갖는 `OPERATINGSYSTEM_KERNELARCHITECTURE`는 **여기에 없다**.

`CI.IPADDRESS` 6개는 ADDRESSSPACE, ADDRESSTYPE, BYTENOTATION, DOTNOTATION,
MANAGEDSYSTEMNAME, STRINGNOTATION이다.

`CI.FILESYSTEM` 16개는 AVAILABLEINODES, AVAILABLESPACE, BUILDLEVEL, CAPACITY,
FILESYSTEMBLOCKSIZE, LEVEL, MAJORVERSION, MANAGEDSYSTEMNAME, MAXBLOCKS, MAXFILESIZE,
MODIFIER, MOUNTPOINT, RELEASE, TOTALINODES, TYPE, VERSIONSTRING이다.
`SYS.FILESYSTEM`의 고유 19개에서 ISPLACEHOLDER·LOCATIONTAG·SERVICEPACK을 뺀 것과 같다.

### Disk는 CI 계열 대응 분류가 없다

`CIROOT` 자식 24개에 디스크·저장 장치 분류가 없다. `CI.%DISK%`·`CI.%MEDIA%`·
`CI.%STORAGE%`·`CI.%DRIVE%`로 CI 적용 분류 전체를 검색하면 `CI.IPSTORAGESWITCHFUNCTION`
한 건만 나오는데, 부모가 `CI.FUNCTION`(CCI00003)이고 스위치 기능이라 디스크와 무관하다.

따라서 Disk는 대조 기준 없이 ACTCI 쪽 `DEV.DISKDRIVE`만 보고 속성을 골라야 한다.
승격 대상 CI 분류가 없다는 뜻이기도 하다. 관리 단위 재검토 근거로 ISSUE-8에 남긴다.

### 승격 범위는 CITEMPLATE이 정한다

어떤 ACTCI 분류가 어떤 CI 분류로 승격되는지는 `MAXIMO.CITEMPLATE`에 설정돼 있다.
상세는 [CI 승격 범위](ci-promotion-scope.md)에 별도로 둔다.

적재 분류 중 `DEV.DISKDRIVE`만 이 테이블에 한 행도 없어 승격할 수 없다.
CI 계열에 디스크 분류가 없다는 위 관측과 같은 결론이다.

CI 기준 밖 속성이 승격에서 실제로 누락되는지는 이 환경에서 확인한 적이 없다.
이 ETL은 승격을 구현하지 않는다. 구조상 위험으로만 기록한다. ISSUE-11.
