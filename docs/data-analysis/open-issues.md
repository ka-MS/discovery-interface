# 미결 사항

미결 항목의 상태·남은 결정과 관련 설계 링크를 관리한다.
상세 구성안·대조표·선택 이유는 `design/`, 관측 사실은 `knowledge/`, 확정 매핑은 `data-mapping/`에 둔다.
종료된 사항은 `close-issues.md` 에 둔다.
GitHub 이슈로 옮긴 항목은 제목과 링크만 남긴다.

## ISSUE-2 스위치가 두 레코드로 분리됨

**상태:** 해결.

네트워크 장비가 Device42 에서 두 레코드로 나뉜다.

| 레코드 | 보유 | 미보유 |
| --- | --- | --- |
| `type = 'cluster'` | netport, MAC, 관리 IP | serial_no, OS |
| `type = 'physical'` (` - Switch N` 접미) | serial_no, OS, hardware_fk | netport, IP |

부모 DEPLOYEDASSET 은 `physical` 만 적재하는데 네트워크 정보는 cluster 에 있다.
장비 레벨 FK(`host_chassis_device_fk`, `virtual_host_device_fk`,
`vm_manager_device_fk`, `chassisslot_fk`)는 네 레코드 모두 비어 있다.

연결은 포트 레벨에 있다. cluster 소속 포트의 `second_device_fk` 가 물리
레코드를 가리킨다.

```sql
SELECT DISTINCT device_fk AS cluster_pk, second_device_fk AS physical_pk
FROM view_netport_v1 WHERE second_device_fk IS NOT NULL
```

`second_device_fk` 가 채워진 포트는 cluster → physical 방향뿐이며 양쪽 서버에서
결과가 일치한다(`.68` 11→13, 12→108 · `.35` 282→284, 281→283).

### 결정

- **MAC** — 위 대응으로 묶은 포트들의 `hwaddress` 최솟값. 멤버 고유값이라 스택
  멤버가 늘어도 겹치지 않는다. 베이스 MAC 포트는 `second_device_fk` 가 비어
  자동 제외된다.
- **IP** — cluster 의 `device_pk` 로 `view_ipaddress_v1` 직접 조회. 관리 IP 가
  `Vlan1` 논리 인터페이스에 붙어 있고 그 포트는 `second_device_fk` 가 비어
  포트 경유가 안 된다. 스택은 IP 를 공유하므로 멤버가 여럿이면 여러 행이 같은
  값을 갖는다.

적용은 `data-mapping/asset/dpanetdevice.md` 참조.

### 잔여

관측 시점에 cluster 당 물리 멤버가 1개뿐이라 1:N 동작을 실측하지 못했다.
멤버가 둘 이상인 스택이 생기면 재확인한다.

## ISSUE-6 DPA 변환 데이터 등록

**상태:** 기본 적재 구현 완료. 정규화 정책 논의 필요.

Maximo UI 는 `DPA*` 자식 테이블을 직접 읽지 않는다. 자식 위에 얹힌 뷰를 읽고,
그 뷰가 변환 변형과 INNER 조인한다. 값이 변환 변형에 없으면 행이 오류 없이
사라진다.

### 결정

- 일반 변환 데이터는 Device42를 직접 조회하는 `conversion` 잡에서 적재한다.
- 전체 실행 순서는 `conversion → asset → ci → software`다.
- 제조사·OS·프로세서·어댑터의 대상과 변형을 이름 기준 MERGE하고 신규 ID는
  Maximo 시퀀스로 발번한다.
- TLOAMSOFTWARE는 `software` 잡에서 DPASOFTWARE보다 먼저 적재한다.
- TLOAMSOFTWARE는 검증된 `UNIQUEID`로 MERGE하고, 조회한 ID를
  `DPASOFTWARE.TLOAMSOFTWAREID`와 `TLOAMPRODUCTID`에 넣는다.
- `DPAMSOFTWARE`와 `DPAMSWVARIANT`는 DPACSOFTWARE 뷰가 조인하지 않으므로
  현재 적재하지 않는다.

상세 매핑은 `data-mapping/conversion/`과
`data-mapping/software/tloamsoftware.md`에 둔다.

### 남은 결정

- 원시 문자열을 그대로 등록할지 별칭을 정규명으로 통합할지 결정이 필요하다.
- `DPANETADAPTER.MAKEMODEL`의 상수 `UNKNOWN`을 변환 데이터에 등록할지 결정이
  필요하다.

## ISSUE-7 원천에서 사라진 행의 처리

**상태:** 논의 필요.

현재 MERGE는 조회된 원천만 INSERT 또는 UPDATE한다. Device42에서 사라진 부모나
자식은 Maximo에 그대로 남는다. 삭제할지 비활성화할지 별도로 결정한다.

## ISSUE-8 Actual CI 대상 범위

**상태:** 논의 필요.

Device42의 본체 후보 View가 모두 별도 Actual CI를 뜻하지 않는다. 조사 결과는
`data-mapping/ci/ci-targets.md`에 있다.

DB·DB Instance 매핑 문서에 더해 Computer 원천 수집 항목 조사를 진행했다.
각 유형의 조사 진행이 다른 후보의 독립 CI 포함 여부를 확정하는 것은 아니다.

### Computer 관리 단위·포함 범위

수집 추천안·필드별 대조표·관계 구성안은 [Computer CI 수집 설계](design/ci/computer.md)에 둔다.

남은 결정:

- Computer 물리·가상 분류, CI 기준 스펙에 BIOS·코어 수 추가를 합의했다. 수집 조건·필드 대응은 [Device 매핑](data-mapping/ci/types/device.md)에 작성·재검증했다. 새 서브타입은 대상 정의를 보완한다.
- 개별 RAM·GPU의 포함 여부와 필요한 상세 관리 범위.
- Subnet·설치 SW의 관리 단위와 관계 구성 선택. IP는 아래 절에서 다룬다.

### Device 기준 통합 수집 — 2026-09-15

**1차 구현 완료:** 현재 본체·스펙 구조를 유지하며 `DeviceCiIntegrate`에서 Computer·VM과
판정 가능한 물리 Switch를 통합 조회·매핑한다. 관계는 본체 이후 별도 단계에서 재조회한다.
필드 대조표와 구현 순서는 [Device 통합 설계](design/ci/device.md)에 있다.

Switch는 `network_device=true`, `type='physical'`, `second_device_fk`로 연결된 cluster 1개,
비어 있지 않은 `fw_device_type=Switch` 한 종류를 모두 만족할 때만 `SYS.GENERICSWITCH`로 적재한다.
양 서버에서 각 2대를 확인했고 Router·NULL·복수 cluster·종류 충돌·Printer는 제외하고 로그를 남긴다.

2차 추천안은 [Device CI 기준정보 설계](design/ci/device-reference-data.md)에 작성했다.
Switch Authorized CI는 `CI.GENERICSWITCH`, 물리 Printer는 `SYS.PHYSICALPRINTER`·
`CI.PHYSICALPRINTER`로 제안하며 아직 MAS UI에는 적용하지 않았다.

남은 결정은 Router 실제 표본과 판정 규칙, 추천 분류 ID의 운영 승인, MAS UI 설정·승격 검증이다.
cluster·컨테이너·unknown·PDU는 이번 후보 범위에 포함하지 않는다.

### OS·Disk·Filesystem·IP 관리 단위 — 2026-09-15

**네 유형을 독립 ACTCI로 관리하기로 합의했다.** Computer 스펙으로 흡수하지 않는다.
근거는 장비당 개수(파일시스템 5.3 / 6.1, IP 1.5, 디스크 1.3 / 1.0개)와 ACTCISPEC 키가
`(ACTCINUM, ASSETATTRID, SECTION)`이라 다중 개체를 담을 수 없다는 점이다.
OS는 장비당 정확히 1개지만 EOL·EOS를 가진 독립 관리 대상이라 CI로 분리한다.

수집 구성안은 [os](design/ci/os.md)·[disk](design/ci/disk.md)·[filesystem](design/ci/filesystem.md)·[ip](design/ci/ip.md),
확정 매핑은 `data-mapping/ci/types/` 의 같은 이름 문서다.

남은 결정:

- **IP의 관계 경로.** Computer와 `NET.IPADDRESS` 사이에 `RELATIONRULES`가 양방향 0건이다. CDM 경로는 `Computer → NET.IPINTERFACE → NET.IPADDRESS`다. Network Interface를 CI로 함께 도입할지, IP를 관계 없이 적재할지, IP를 다음 단계로 미룰지 정해야 한다. 범위 확대 여부라 사용자 결정이 필요하다.
- **Disk·IP의 독립 CI 유지 여부.** 실제로 채울 수 있는 속성이 Disk 3개(모델·시리얼·용량), IP 1개(주소)뿐이다. 제조사·펌웨어·미디어 유형은 원천이 전건 비어 있다.
- **컨테이너·가상 파일시스템 선별.** `overlay` 38 / 62건, `devtmpfs` 9 / 10건, `squashfs` 0 / 8건이다. 경로에 컨테이너 ID가 들어가 재기동 시 원천 PK가 바뀌면 매 실행마다 새 CI가 생긴다. 제외를 추천하나 목록이 확정되지 않았다.
- **Subnet CI 도입 여부.** IP의 `mask_bits`가 전건 보유인데 `NET.IPADDRESS`에 담을 자리가 없다. `NET.IPNETWORK`가 별도 분류다.
- **수집 대상 범위.** OS는 Computer 연결분이 30% / 80%로 두 서버 차이가 크다. Filesystem·Disk는 전건 Computer 연결이라 판단이 필요 없다. **IP는 2026-09-15 장비 연결 전체로 넓혔다**(105 / 111건). 장비 종속 개체가 아니므로 부모 유형으로 좁히지 않는다.
- **IP 분류를 v4/v6로 나눌지.** `NET.IPV4ADDRESS`·`NET.IPV6ADDRESS`가 `NET.IPADDRESS`와 속성 22개가 동일하고 승격 범위에도 등록돼 있다. 원천 IPv6는 0 / 2건이다. 나누면 값을 정할 수 없는 `IPADDRESS_ADDRESSTYPE` 없이도 구분이 분류로 표현된다. 이미 97건이 `NET.IPADDRESS`로 적재돼 있어 분류 변경 처리도 함께 정해야 한다.
- **Disk 원천 커버리지.** 표본 Computer 95 / 85대 중 디스크를 가진 장비가 18 / 19대뿐이다. 사업 범위 「서버 — Disk」 요구를 이 원천으로 충족할 수 있는지 운영 D42에서 확인해야 한다.

### 사업 범위 기준 검토 — 2026-09-11

사용자가 제공한 [사업 추진 범위](../requirements/business-scope.md)를 수집 요구사항의 기준으로 삼는다. 기존 DB·DB Instance 문서는 조사 결과이며 사업 요구사항 전체의 충족을 뜻하지 않는다.

- 서버·네트워크·DB·WEB/WAS·기타 S/W 구분별로 원천과 적재 결과를 대조한다. 사업 구분과 구현의 수집 카테고리·CI 분류는 반드시 1:1일 필요는 없다.
- DB는 설치된 DBMS의 제품명·버전·설치 경로 수집을 우선 검토한다. 기존 Instance 매핑이 이 요구를 충족하는지, 설치 소프트웨어·서비스 등 보강 원천이 필요한지 확인한다.
- 개별 Database·Schema 및 Kubernetes 상세 개체는 사업 범위와 관리 필요성을 확인한 뒤 독립 CI 포함 여부를 정한다. 명시되지 않았다는 이유만으로 제외를 확정하지 않는다.
- CPU·IP 등 상세 정보를 별도 CI로 만들지 속성으로 둘지, 설치·실행 위치 등 어떤 관계를 수집할지는 카테고리별 조사에서 결정한다.
- 카테고리 하나의 본체·속성과 필요한 관계를 검증한 뒤 확장한다. 이 검토만으로 기존 분류 매핑을 변경하거나 새로운 관계 코드를 확정하지 않는다.

확인된 중복 표현은 다음과 같다.

- Database 전용 View는 `view_resource_v2`와 PK와 이름이 전건 일치한다.
- Kubernetes Cluster·Deployment·Node·Service 전용 View는
  `view_resource_v2`와 PK, identifier, 이름이 전건 일치한다.
- Cloud Instance는 Device와 전건 1:1로 연결되지만 PK와 이름은 전건 동일하지 않다.
- Database Instance는 Application Component와 연결되지만 PK와 이름은 다르다.
- Database Instance도 동일 PK·이름의 Resource가 있다. Instance 본체와 중복 적재하지 않는다.

### 남은 결정

- Actual CI로 관리할 개체 유형
- `view_resource_v2`의 유형별 포함 목록
- Cloud Instance의 별도 CI 여부와 DB Instance에 연결된 Application Component의 중복 적재 처리
- Application Group의 상태별 포함 기준
- Service Instance의 적재 단위와 상태별 포함 기준
- Subnet, VLAN, VRF를 Actual CI로 관리할지 여부

## ISSUE-11 Actual CI 분류·속성·관계와 식별자 매핑

**상태:** DB Instance는 엔진별 분류와 CI 합집합 기준 속성으로 2026-09-16 재확정. Database 본체는 일반 분류 유지. 기준정보 보완과 공통 적재 정책 논의 필요.

근거: [원천](data-mapping/ci/ci-targets.md), [분류·속성](knowledge/maximo/ci-classification.md),
[관계 규칙](knowledge/maximo/ci-model.md). 대상 포함 여부는 ISSUE-8에서 결정한다.

### 적재 범위 검토안

정기 ETL은 ACTCI·ACTCISPEC·ACTCIRELATION으로 제한하고, 필요한 기준정보는
별도 사전 구성으로 관리하는 안이다. 기존 분류·속성·관계 정의를 재사용할지,
누락 설정을 보완하거나 새 정의를 만들지 먼저 결정한다.
신규 분류는 CLASSSTRUCTURE·CLASSSPEC뿐 아니라 CLASSUSEWITH·CLASSSPECUSEWITH·
ASSETATTRIBUTE 등 연결 정보와 Maximo 애플리케이션 동작까지 검증해야 한다.

### 결정

- DB Instance는 `database_type`에 따라 엔진별 분류로 라우팅하고, 전용 분류가 없는 엔진은
  `APP.DB.GENERICDATABASESERVER`로 보낸다. 2026-09-16에 기존 단일 분류 결정을 대체했다.
- 수집 속성의 대조 기준은 승격 대상 CI 분류의 합집합이다. 1단계는 네 분류 공통 8개 중
  원천이 있는 것만 적재하고 엔진 전용 11개는 원천·표본이 갖춰진 뒤 정한다.
- Database 본체는 엔진별로 분기하지 않고 `APP.DB.DATABASE`를 유지한다.
- 확정된 원천·분류·속성 대응은 [DB](data-mapping/ci/types/database.md)와
  [DB Instance](data-mapping/ci/types/database-instance.md)를 정본으로 삼는다.
  선택 이유는 [Database CI 수집 설계](design/ci/databaseinstance.md)에 있다.
- 전용 분류가 없는 엔진은 `APP.DB.DATABASESERVER`로 보낸다. `APP.DB.GENERICDATABASESERVER`와
  관계 규칙이 완전히 같고 속성 차이가 하나뿐인데 그 값이 제품명과 중복되어 쓰지 않는다.
- 남은 결정: `APPSERVER_VENDORNAME` 고정표 여부, 엔진 전용 속성 세 후보의 원천 확정,
  Instance→Database 분류쌍 규칙 등록.

### 나머지 분류 선택 검토안

Device 확장 조사(2026-09-15)의 [분류·필드 대조표](design/ci/device.md)와 [설정 관측](knowledge/maximo/device-ci-classifications.md)을 작성했다.
Switch 판정·ACTCI 분류와 2차 기준정보 추천안은 확정했다. 남은 결정:

- Router 실제 표본과 판정 규칙. 표본이 없어 `SYS.GENERICROUTER` 분기는 구현하지 않는다.
- Switch의 `CI.GENERICSWITCH` 신규 분류·18개 속성·본체 1:1 승격 범위를 MAS UI에서 적용하고 검증한다.
- Printer의 `SYS.PHYSICALPRINTER`·`CI.PHYSICALPRINTER`, 13개 속성, 본체 1:1 승격 범위를 MAS UI에서 적용하고 검증한다.
- 추가 자산·위치·OS·EOL/EOS 등 필드의 대상 속성·단위와 Printer 대표 MAC·SysName 원천 대응.

이 표는 확정 매핑이 아니다. 이름이 유사하다는 이유만으로 의미가 같다고 보지 않는다.

| 원천 대상 | 검토할 분류 | 남은 판단 |
| --- | --- | --- |
| Device | SYS.COMPUTERSYSTEM / SYS.VIRTUALCOMPUTERSYSTEM / SYS.GENERICSWITCH / SYS.PHYSICALPRINTER 제안 | Router·컨테이너·unknown의 후속 범위와 신규 기준정보 UI 적용 |
| Application Component / Group | APP.APPLICATION 등 | 실행 구성요소와 논리 묶음의 구분 |
| Business Service | PROCESS.BUSINESSSERVICE | 원천 개체와 분류 의미 대응 |
| Service Instance | SERVICE.SERVICEINSTANCE | 발견된 서비스 프로세스와 분류 의미 대응; 누락 설정 보완 여부 |
| Subnet / VLAN | NET.IPNETWORK / NET.VLAN | 관리 범위와 주소·번호의 식별 범위 |
| Cloud / Kubernetes / VRF / Storage Array | 미선정 | 다른 명칭의 분류 재사용, 신규 분류 또는 제외 여부 |

### OS·Disk·Filesystem·IP 조사에서 드러난 항목 — 2026-09-15

근거는 [분류 조사](knowledge/maximo/ci-component-classifications.md)와
[원천 조사](knowledge/device42/ci-component-inventory.md)다.

- **관계 정의의 `USEWITH`가 전부 `CI`다.** 조회한 7개 코드에 `ACTCI`는 없다. OS → 물리 Computer의 `RELATION.INSTALLEDON`은 실제 ACTCI 관계 INSERT·승격·CI 화면 표시를 확인했다. 다른 관계는 별도 검증한다. [샘플 결과](knowledge/maximo/computer-ci-relations.md#oscomputer-승격-샘플-검증).
- **OS의 EOL·EOS를 담을 속성이 없다.** 원천이 48 / 22, 47 / 22건 보유하고 사업 범위 「나. EOS 관리」에 직결되는데 `SYS.OPERATINGSYSTEM`에 수명주기 날짜 속성이 없다. 전역 속성 신규 등록이 필요하다. 이번 범위에서는 제외를 추천했다.
- **Filesystem 용량 단위.** 원천에 단위 컬럼이 없다. 표본상 MB로 해석되며 `MEASUREUNITID='MBYTE'` 지정을 추천한다.
- **Disk 용량 단위.** `hdsize_unit`이 GB·TB 혼재다. `TBYTE` 코드 존재가 미확인이며 GB 정규화가 대안이다.
- **마운트 경로 길이.** 컨테이너 경로가 약 130자다. `ACTCINAME` 192자, `ALNVALUE` 254자 한계에 근접한다. 절단·생략 규칙이 필요하다.
- **식별자 접두어.** `view_part_v1`이 CPU·RAM·GPU와 공용이라 `D42:PART:` 대 `D42:DISK:` 선택이 남는다.
- **`IPADDRESS_ADDRESSTYPE` 코드 규약** 미확인.
- **중복 속성 선택.** OS의 `VERSIONSTRING`·`NAME`, IP의 `STRINGNOTATION`, Disk의 `MEDIAACCESSDEVICE_NAME`이 각각 다른 속성과 중복이다. 한쪽만 채택해야 한다.
- **`MODELOBJECT_CDMSOURCE`·`SOURCETOKEN` 채택 여부.** 네 분류 모두 공통으로 갖는다. 연계 출처와 원천 키를 남기는 용도로 쓸지 정해야 한다.
- **Computer의 CI 기준 대조를 다시 해야 한다.** BIOSRELEASEDATE 등록으로 `CI.COMPUTERSYSTEM`이 19개가 됐다. 현재 수집 18개와 대조하면 기준 밖이 BIOSMANUFACTURER·ROMVERSION·CPUCORESINSTALLED 3개, 기준에 있는데 미수집이 FQDN·MANAGEDSYSTEMNAME·SIGNATURE·SYSTEMBOARDUUID 4개다. 2026-09-15 관측.
- **CI 계열 대조 기준.** 적재 대상은 ACTCI 분류지만 수집 속성 선택의 기준선은 `CIROOT` 아래 CI 분류다. OS는 `CI.OS`(7개), Filesystem은 `CI.FILESYSTEM`(16개), IP는 `CI.IPADDRESS`(6개)이며 **Disk는 대응 분류가 없다.** 2026-09-15 조사에서 확인해 반영했다.
- **승격 범위 정본은 `CITEMPLATE`이다.** 2026-09-15 확인했다. 어떤 ACTCI 분류가 어떤 CI 분류로 승격되는지가 여기 설정돼 있으며 범위 14개·매핑 158행이다. 관측은 [CI 승격 범위](knowledge/maximo/ci-promotion-scope.md). 이전 문서들이 "승격 미검증"으로만 적었던 부분을 대체한다.
- **Filesystem 승격 매핑을 추가했다.** 기본 구성은 `CI.FILESYSTEM`을 `SYS.LOCALFILESYSTEM`에만 매핑하는데 이 ETL은 `SYS.FILESYSTEM`으로 적재한다. 2026-09-15 매핑 행을 추가했다(`CITEMPLATE` 164번). `CI.FILESYSTEM`에 ACTCI 분류 둘이 걸린 것은 158행 중 유일한 경우이며 승격 동작은 미검증이다. 화면의 「유효성 검증」으로 확인해야 한다.
- **Disk는 승격할 수 없다.** `DEV.DISKDRIVE`가 `CITEMPLATE`에 한 행도 없다. CI 계열에 디스크 분류가 없다는 관측과 같은 결론이다. ISSUE-8의 관리 단위 재검토 근거다.
- **가상 Computer 승격 범위에 자식이 없다.** `CI.VIRTUALCOMPUTERSYSTEM` 범위는 자기 자신 1행뿐인데 `CI.COMPUTERSYSTEM` 범위는 OS·Filesystem·IP를 포함한 9행이다. 2026-09-15 적재 기준 Computer 70대 중 65대가 가상이므로, 현재 설정으로는 가상 서버를 승격해도 본체만 올라간다. 기준정보 변경은 이 ETL 범위가 아니다.
- **`MODELOBJECT_CDMSOURCE`·`SOURCETOKEN` 미채택 확정.** CI 계열 분류에 `MODELOBJECT_` 속성이 0개라 승격에서 전달되지 않고, `ACTCINUM`·`CHANGEBY`와 정보가 중복된다. 시스템 전체 기존 값도 0건이다. 2026-09-15 결정.
- **`MODELOBJECT_LABEL`은 IP와 Filesystem에서 채택했다.** 원천에 실제 값이 있어 넣었다(IP 55 / 59건, Filesystem 24 / 8건). CI 계열에 없으므로 승격 전달은 기대하지 않는다.
- **CI 기준 밖 속성의 승격 전달.** `OPERATINGSYSTEM_KERNELARCHITECTURE`는 `CI.OS`에 없는데 채택했다. Computer의 BIOS 출시일·CPU 코어 수와 같은 의도적 추가다. ACTCI→CI 승격에서 이런 속성이 누락되는지는 미검증이다. 승격 자체가 미구현이다.
- **Disk의 승격 대상 분류 부재.** CI 계열에 디스크 분류가 없어 승격할 곳이 없다. 관리 단위 재검토 근거로 ISSUE-8에도 걸었다.
- **LASTSCANDT 원천.** IP만 `last_discovered`를 전건 갖는다. OS·Disk·Filesystem은 부모 Computer의 값을 쓰는 안을 추천했다.
  DB Instance는 Resource의 `last_discovered`가 전건 NULL이어서 2026-09-16에 `last_changed`로 확정했다.

### 속성 대응 검토안

타입·단위 근거는 분류 문서, 값 보유율·길이는 원천 문서에 둔다.
Device 본체·스펙의 현재 대응은 [Device 매핑](data-mapping/ci/types/device.md), 연관 CI 후보는 [수집 설계 대조표](design/ci/computer.md#원천-필드별-스펙-대조보완)를 참조한다.

| 원천 필드 | 검토할 ASSETATTRID | 남은 판단 |
| --- | --- | --- |
| database.database_id / creation_date / collate / recovery_model / allocated_size | 일반 DB 분류에서 미선정 | 기존 속성 대응 또는 신규 속성 정의; DB ID 범위·날짜 형식·크기 단위 |
| databaseinstance.database_count / connection_count / is_default_instance | DB Instance 분류에서 미선정 | 기존 속성 대응 또는 신규 속성 정의; 연결 수의 수집 시점 |
| subnet.mask_bits / vlan.number | IPNETWORK_PREFIXLENGTH / VLAN_VLANID | 대상 포함 및 분류 선택 |

원천 명칭은 원천 문서와 유형별 문서에 기재한 View다.
빈 속성 행 생성 여부, 섹션, 기본값·필수 여부·표시 순서의 적용 방식도 결정한다.
compatibility_level의 일반 분류 속성 대응은 미정이며, DB 제품 버전으로 간주하지 않는다.

### 네트워크 인터페이스와 스위치 CI 단위 — 2026-09-17

관측은 [네트워크 CI 모델](knowledge/maximo/network-ci-model.md)에 있다.

- **스위치 CI 단위.** 현재 ACTCI로 올리는 스위치는 물리 멤버인데 netport가 0개다.
  포트 33·28개는 전부 스택(`cluster`) 객체에 달려 있고 그건 수집하지 않는다.
  **IP도 같다.** `view_ipaddress_device_v2` 기준 IP를 가진 네트워크 장비는 cluster 2대(각 1건)뿐이고
  물리 멤버는 0건이다. 그래서 IP 관계 필터를 `CiSourceFilter.DEVICE`로 넓혀도 추가되는 쌍이 0건이며
  `cluster`는 그 필터에도 포함되지 않는다. 필터 확장은 이 항목이 정해진 뒤에 의미가 생긴다.
  사업 범위의 네트워크 행에 "Network Interface 정보"가 명시돼 있으므로 인터페이스를 채우려면
  스위치 CI를 cluster로 옮길지, 물리 멤버에 스택의 포트를 붙일지 먼저 정해야 한다.
- **인터페이스 CI 도입 시점.** `NET.L2INTERFACE`는 netport와 속성이 그대로 대응한다.
  서버 NIC는 사업 범위 서버 행에 없으므로 네트워크 장비 작업에서 도입하고, 그때 서버 쪽 적용을
  다시 판단한다.
- **Computer-IP 표준 경로.** 지금은 접두어 없는 `USES`로 직접 연결했다. 표준은
  `NET.IPINTERFACE` 경유이며 원천에 그 계층이 생기거나 IBM 디스커버리를 병행하면 이관을 검토한다.
- **서버-스위치 물리 연결.** 원천이 `.68` 5건·`.35` 2건이고 상대 포트가 전부 `Vlan1`이라
  MAC 학습 기반 연관으로 보인다. `NET.NETWORKCONNECTION` 중간 CI 도입 여부는 미결이다.

### Device 적재 전 확인 — 2026-09-15

본체·스펙 대응 및 SQL은 [Device 매핑](data-mapping/ci/types/device.md), 관계 구성안은 [수집 설계](design/ci/computer.md)에 둔다.
등록된 분류·스펙·관계는 [분류 조사 결과](knowledge/maximo/computer-classification-specs.md)에서 확인했다.

- BIOS 출시일 원문용 COMPUTERSYSTEM_BIOSRELEASEDATE(ALN). **2026-09-15 사용자가 전역 ASSETATTRIBUTE와 CI.COMPUTERSYSTEM 템플릿을 수동 등록했다.** ACTCI 쪽 SYS.COMPUTERSYSTEM 템플릿은 아직 없어 적재는 계속 명시적 추가 속성 경로(CLASSSPECID=NULL, DISPLAYSEQUENCE=180)를 쓴다. ACTCI 템플릿 등록 시 기존 경로가 자동 우선한다. UI·승격 검증은 남아 있다.
- 사용 분류 enum 기반 공통 캐시와 명시적 추가 속성 처리: [캐시 설계](design/ci/definition-cache.md). 실제 Maximo에서 새 캐시 SQL·추가 속성 경로의 동작 확인은 후속 검증.
- sourceId=`D42:<원천 개체 종류>:<원천 PK>` 기반 본체·스펙 저장을 구현했다. 신규 숫자 ID는 각 Maximo 시퀀스 NEXT VALUE를 사용하며 기존 ID를 유지한다. 실제 Maximo 동시 채번·적재 검증은 남아 있다.
- FQDN·SIGNATURE 대응과 MANAGEDSYSTEMNAME·SYSTEMBOARDUUID 원천 보강. 매핑 규칙에 따른 단위 표시·승격 후 전달 및 조건부 CPU·MAC 보강의 UI 확인.
- CI 전용 설정을 제거하고 기존 asset의 getData → mapData → putData 형태로 통일했다. 현재 상수·시간대·건별 오류 처리와 롤백 보류는 [실행 준비](data-mapping/ci/types/device-run.md)에 기록했다. 재시도·삭제·분류 변경 등 운영 정책 확장은 후속 결정이며 설치 SW의 경로 보강도 후속 유형에서 진행.
- 관계의 방향·카디널리티·SWAPPED 적용과 IP 직접 연결·SW의 OS 연결 조건 검증.

### DB·Instance 원천별 남은 판단

- `.68`은 databaseinstance_fk 기준 9쌍, instance_id·Resource.root_resource_fk 기준 10쌍이다(`.35`는 37쌍).
  현재 관계 SQL은 FK 기준을 유지한다. 차이의 원인과 보강 우선순위를 확인한 후 1건의 관계를 추가할지 결정한다.
- 일반 DB의 34개 속성에는 DB 내부 ID·생성 시각·정렬 규칙·호환성 수준·복구 모드·크기에
  바로 대응시킬 전용 속성이 확인되지 않았다. 이름이 비슷한 DATABASE_ASSETID 등을 대신 쓰지 않고
  기존 속성 재해석 또는 신규 정의 여부를 결정한다.
- DB Instance의 DB 수·연결 수·기본 Instance 여부와 JSON의 주소·CPU·메모리·시작 시각·메모리 상태는
  Target 속성과 의미·단위·범위가 미확정이다. 호스트 자원 값을 Instance 속성으로 임의 대입하지 않는다.
- db_type_id의 다른 엔진 코드 대응은 미확인이다. 제품명에는 database_type을 사용하므로
  코드표 미확인이 이름 매핑을 막지는 않는다.
- Resource.details.version은 APPSERVER_PRODUCTVERSION에 원문으로 대응한다. CI 쪽에 없는
  APPSERVER_VERSIONSTRING은 쓰지 않는다. 제품 버전 번호·빌드·OS를 분해하는 규칙은 만들지 않는다.
  표본 밖 엔진의 키·형식은 추가 검증 대상이다.
- Resource.notes는 두 유형 모두 빈 문자열이지만 DESCRIPTION 원천으로 대응한다.
  원문 유지 조건과 별개로 비어 있지 않은 메모의 실제 적재·UI 표시는 미검증이다.

### DB 매핑의 적재 전 보완

- 일반 DB 분류의 선택 속성에 ASSETATTRIBUTEID 연결과 ACTCI용 CLASSSPECUSEWITH를
  보완해야 한다. 적용 범위, 표시 순서·필수 여부·기본값을 정하고 별도 변경 승인을 받는다.
- Instance→DB는 RELATION.CONTAINS를 사용하는 안이다. 엔진별 네 분류와 기존 범용 분류 모두
  APP.DB.DATABASE 대상 분류쌍 규칙이 0건이므로 카디널리티 1:N·포함 관계·부모 방향 설정을
  확정하고 등록해야 한다. 등록 전에는 MERGE 가드가 전건 거부한다.
- 조회 SQL은 databaseinstance_fk가 없는 DB도 반환한다. 관계 부재를 이유로 본체를 제외하지 않는다.
  다만 발견 시각·식별자 정책이 정해지기 전에는 적재 가능으로 간주하지 않는다.

### 관계·필수값

2026-09-15 Computer 관계 조사: [관계 설계](design/ci/relations.md),
[공통 저장 초안](data-mapping/ci/actcirelation.md).
Disk·Filesystem 포함 및 OS 설치 관계의 원천·분류쌍 매핑을 작성했다.
OS → 물리 Computer 한 쌍은 SWAPPED=0으로 INSERT한 뒤 CI 승격·관계 표시·부모 보존을 확인했다.
[검증 기록](knowledge/maximo/computer-ci-relations.md#oscomputer-승격-샘플-검증).
공통 MERGE(ActCiRelationWriter)와 OS→Computer·Computer→Disk·Computer→Filesystem 세 관계는 구현했고
2026-09-15 `./run.sh ci-relation` 운영 적재로 검증했다(ACTCIRELATION 143행, 고아·규칙 위반 0건, 재실행 멱등성 확인).
신규 적재분의 CI 승격, 관계의 이동·삭제, Host→VM의 CI 승격,
Interface→IP, 배열 펼침 경로는 아직 미검증이다. Host→VM 원천 조회·1:N 기준정보·코드는 완료했다.
검증 수준과 남은 항목은 [공통 매핑 4절](data-mapping/ci/actcirelation.md#4-검증-수준과-후속)을 참조한다.
실행 위치는 CI 본체 적재 이후의 별도 관계 단계로 정리했다.
GUID 두 컬럼은 샘플 승격 결과에 따라 신규 NULL로 결정해 미결에서 내렸다.

남은 결정은 Interface–IP의 1:1 설정 대조, Interface CI 도입과 포트 미연결·공유 IP 경로,
다른 분류쌍의 ACTCIRELATION.SWAPPED·UI·승격 검증이다. 규칙 SWAPPED를 행에 그대로 복사하지 않는다.
관계의 이동·삭제는 ISSUE-7과 함께 검토하며 이번 조사에서 정책을 확정하지 않는다.
호스트 변경 시 MERGE는 새 관계를 추가할 뿐 이전 관계를 지우지 않으므로,
ETL 관리 범위와 원천 조회의 완전한 성공 여부를 전제로 한 정리 정책이 필요하다.
부분 조회나 조회 실패를 근거로 관계를 삭제하지 않는다.

- Instance→장치는 `appcomp_fk → device_fk`가 실제 행과 일치하는 `.35` 3쌍에
  `RELATION.RUNSON`을 사용한다. `.68`의 `device_fk` 없는 Instance 1건은 본체만 유지하고 관계 미해결로 기록한다.
- 신규 VIRTUALIZES의 Host→VM 1:N 방향·분류쌍과 실제 관계 연결은 검증했다.
  CI 승격과 호스트 이동 시 이전 관계 정리 정책은 남아 있다. 기존 CI의 관계 행은 정합성 기준 없이 복사하지 않는다.
- ACTCINUM·GUID·CCIDISGUID·숫자 PK·MERGE 키를 구분한다. 유형 간 숫자 PK 충돌을
  피할 식별 범위와 재수집 시 동일성 정책, 시퀀스 예약 공존 방식을 결정한다.
- LASTSCANDT 누락을 제외·보강·대체 중 어떻게 처리할지 결정한다.
  last_changed·적재 시각을 발견 시각으로 대체하는 규칙은 아직 없다.
- LANGCODE·CHANGEBY·CHANGEDATE·기본값의 JDBC 적재 규칙 및 UI 표시를 검증한다.

## ISSUE-10 전력·설비 서브타입의 적재 제외 범위

**상태:** [이슈 #1](https://github.com/ka-MS/discovery-interface/issues/1) 로 이관.
