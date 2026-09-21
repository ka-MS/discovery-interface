# CI 분류·스펙 매핑 명세

> 기준: 현재 운영 소스 코드 · 2026-09-21 문서 대조. 이번 작업은 운영 DB에 접속하지 않았다.
> 이 문서는 **코드가 선택하는 ACTCI 분류와 수집 속성의 전체 목록**이다.
> 환경별 CLASSSTRUCTUREID·CLASSSPECID와 등록된 전체 스펙은 실행 시 조회한다. 수집 목록과 DB 등록 목록은 다르다.

## 1. 책임과 식별자

- CLASSSTRUCTURE는 분류 기준정보, CLASSSPEC은 분류별 속성 템플릿, ASSETATTRIBUTE는 속성의 자료형 정의다.
- CLASSUSEWITH는 분류의 ACTCI 적용 여부, CLASSSPECUSEWITH는 스펙의 ACTCI 적용·순서·필수 여부를 제공한다.
- 이 ETL은 위 기준정보를 **조회만** 한다. 기준정보를 만들거나 고치지 않는다.
- Mapper는 CLASSIFICATIONID로 분류를 선택하고 조회된 CLASSSTRUCTUREID를 ACTCI에 저장한다.
- ACTCISPEC에는 해당 ACTCI 분류의 CLASSSPECID를 쓴다. 승격 대상인 CI.* 분류의 템플릿 ID를 복사하지 않는다.
- 분류의 PARENT 계층은 분류 트리이지 CI 인스턴스 간 관계가 아니다. 연결은 [관계 명세](relations.md)에서 별도로 정의한다.
- `ci` 시작 시 정의를 한 번 로딩한다. 분류가 없으면 해당 CI를 건너뛰고, 중복 등 정의 로딩 예외는 본체·관계 실행 전에 전파한다.
- `ci-relation` 단독 실행은 정의 캐시를 로딩하지 않고 저장 시 실제 ACTCI·RELATIONRULES를 대조한다.

구현: [CiClassification](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/CiClassification.java),
[CiDefinitionLoader](../../../../src/main/java/com/itmsg/device42/target/maximo/ci/definition/CiDefinitionLoader.java),
[CiDefinitionCache](../../../../src/main/java/com/itmsg/device42/target/maximo/ci/definition/CiDefinitionCache.java),
[CiSpecMapper](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/CiSpecMapper.java).

## 2. CI → 분류 전체 대응

아래 조건은 Source 조회를 통과한 행에 대한 Mapper 선택이다. 전체 수집 SQL은 유형 문서를 따른다.
문자열은 대소문자를 구분하며, DB 엔진은 trim 후 비교한다. 분류 ID를 찾지 못했을 때 다른 분류로 재시도하지 않는다.

| CI 유형 | 분류 선택 조건 | CLASSIFICATIONID | 호출하는 스펙 묶음 | ACTCINUM |
| --- | --- | --- | --- | --- |
| 물리 Computer | physical, network_device가 true 아님 | SYS.COMPUTERSYSTEM | C 17개 | D42:DEVICE:&lt;device_pk&gt; |
| VM | virtual, network_device가 true 아님 | SYS.VIRTUALCOMPUTERSYSTEM | C 17개 + V 1개 | D42:DEVICE:&lt;device_pk&gt; |
| 물리 Switch | physical + network_device=true + 연결 cluster 1개 + 종류 1종 Switch | SYS.GENERICSWITCH | C 17개 + S 1개 | D42:DEVICE:&lt;device_pk&gt; |
| Network Cluster | cluster + network_device=true + 자기 종류 1종 Switch | SYS.COMPUTERSYSTEMCLUSTER | N 2개 | D42:DEVICE:&lt;device_pk&gt; |
| OS | 조회 대상 OS 전체 | SYS.OPERATINGSYSTEM | O 5개 | D42:DEVICEOS:&lt;deviceos_pk&gt; |
| Disk | Hard Disk 파트 | DEV.DISKDRIVE | D 3개 | D42:PART:&lt;part_pk&gt; |
| Filesystem | Computer 연결 + 파일시스템 필터 | SYS.FILESYSTEM | F 5개 | D42:MOUNTPOINT:&lt;mountpoint_pk&gt; |
| IP | 장비에 연결된 IP, 본체는 PK당 하나 | NET.IPADDRESS | I 4개 | D42:IPADDRESS:&lt;ipaddress_pk&gt; |
| Microsoft SQL Instance | database_type=Microsoft SQL | APP.DB.MSSQL.SQLSERVER | B 5개 | D42:DATABASEINSTANCE:&lt;databaseinstance_pk&gt; |
| DB2 Instance | database_type=DB2 | APP.DB.DB2.DB2INSTANCE | B 5개 | 위와 같음 |
| Oracle Instance | database_type=Oracle Database | APP.DB.ORACLE.ORACLEINSTANCE | B 5개 | 위와 같음 |
| 기타 DB Instance | 그 밖의 엔진·NULL | APP.DB.DATABASESERVER | B 5개 | 위와 같음 |

Network Printer는 Device Mapper의 첫 조건에서 제외한다. Router·불명확한 네트워크 종류·복수 cluster도 적재하지 않는다.
독립 Database, Interface, 독립 CPU·Memory CI는 현재 실행 목록에 없다. DPA 자산 수집과 혼동하지 않는다.

## 3. 분류별 수집 스펙 전체 목록

아래 42개 고유 ASSETATTRID가 코드의 수집 목록이다. 묶음 C를 세 분류가 공유하고 B를 네 분류가 공유한다.
모든 Mapper는 SECTION=NULL로 조회한다. 개수는 **매핑 시도 수**이며 실제 저장 수를 보장하지 않는다.
정상 템플릿·자료형·단위 조건은 4절을 따른다. 개별 D42 컬럼·보강 조인은
[Device](types/device.md), [OS](types/os.md), [Disk](types/disk.md), [Filesystem](types/filesystem.md),
[IP](types/ip.md), [DB Instance](types/database-instance.md)에 있다.

| 묶음 | ASSETATTRID | 값 컬럼 | 현재 값·변환 |
| --- | --- | --- | --- |
| C | COMPUTERSYSTEM_NAME | ALNVALUE | 장비 이름 |
| C | COMPUTERSYSTEM_SERIALNUMBER | ALNVALUE | 장비 시리얼 |
| C | COMPUTERSYSTEM_UUID | ALNVALUE | 장비 UUID; 본체 키로 사용하지 않음 |
| C | COMPUTERSYSTEM_MODEL | ALNVALUE | 하드웨어 모델 |
| C | COMPUTERSYSTEM_MANUFACTURER | ALNVALUE | 하드웨어 제조사 |
| C | COMPUTERSYSTEM_MEMORYSIZE | NUMVALUE | ram, GB→GBYTE·MB→MBYTE; 수치 보존 |
| C | COMPUTERSYSTEM_NUMCPUS | NUMVALUE | total_cpus |
| C | COMPUTERSYSTEM_CPUSPEED | NUMVALUE | cpu_speed, GHz→GHZ·MHz→MHZ; 수치 보존 |
| C | COMPUTERSYSTEM_CPUTYPE | ALNVALUE | CPU 파트 모델이 정확히 1종일 때 |
| C | COMPUTERSYSTEM_ARCHITECTURE | ALNVALUE | CPU 파트 architecture가 정확히 1종일 때 |
| C | COMPUTERSYSTEM_PRIMARYMACADDRESS | ALNVALUE | Computer 기본 포트 1개 또는 Switch cluster 포트 MIN |
| C | COMPUTERSYSTEM_TYPE | ALNVALUE | 상수 ComputerSystem |
| C | COMPUTERSYSTEM_VIRTUAL | ALNVALUE | virtual이면 true, 그 외 false 문자열 |
| C | COMPUTERSYSTEM_BIOSMANUFACTURER | ALNVALUE | BIOS 제조사 |
| C | COMPUTERSYSTEM_ROMVERSION | ALNVALUE | BIOS 버전 |
| C | COMPUTERSYSTEM_BIOSRELEASEDATE | ALNVALUE | BIOS 출시일 원문; 유일한 명시적 추가 속성 예외 |
| C | COMPUTERSYSTEM_CPUCORESINSTALLED | NUMVALUE | total_cpus × core_per_cpu; 어느 하나 NULL이면 NULL |
| V | COMPUTERSYSTEM_VMID | ALNVALUE | VM만 vm_manager_int_id |
| S | GENERICCOMPUTERSYSTEM_GENERICTYPE | ALNVALUE | 물리 Switch만 Switch |
| N | COMPUTERSYSTEMCLUSTER_MANAGEDSYSTEMNAME | ALNVALUE | cluster 장비 이름 |
| N | COMPUTERSYSTEMCLUSTER_LOCATIONTAG | ALNVALUE | details.snmp_location trim, 공백은 NULL |
| O | OPERATINGSYSTEM_OSNAME | ALNVALUE | os_name |
| O | OPERATINGSYSTEM_NAME | ALNVALUE | os_name |
| O | OPERATINGSYSTEM_OSVERSION | ALNVALUE | os_version |
| O | OPERATINGSYSTEM_KERNELVERSION | ALNVALUE | os_version_no |
| O | OPERATINGSYSTEM_KERNELARCHITECTURE | ALNVALUE | os_arch_name |
| D | MEDIAACCESSDEVICE_MODEL | ALNVALUE | 파트 모델 |
| D | MEDIAACCESSDEVICE_SERIALNUMBER | ALNVALUE | 파트 시리얼 |
| D | DISKDRIVE_DISKSIZE | NUMVALUE | GB는 유지, TB는 ×1024; GBYTE |
| F | FILESYSTEM_MOUNTPOINT | ALNVALUE | mountpoint |
| F | FILESYSTEM_TYPE | ALNVALUE | fstype_name |
| F | FILESYSTEM_CAPACITY | NUMVALUE | capacity; MBYTE 상수 |
| F | FILESYSTEM_AVAILABLESPACE | NUMVALUE | free_capacity; MBYTE 상수 |
| F | MODELOBJECT_LABEL | ALNVALUE | 마운트 label |
| I | IPADDRESS_DOTNOTATION | ALNVALUE | HOST(ip_address) |
| I | IPADDRESS_STRINGNOTATION | ALNVALUE | HOST(ip_address), 위와 동일 |
| I | IPADDRESS_MANAGEDSYSTEMNAME | ALNVALUE | 연결 장비 중 device_pk 최소인 장비 이름 |
| I | MODELOBJECT_LABEL | ALNVALUE | IP label |
| B | APPSERVER_NAME | ALNVALUE | Instance 이름 |
| B | APPSERVER_PRODUCTNAME | ALNVALUE | 엔진명 |
| B | APPSERVER_PRODUCTVERSION | ALNVALUE | Resource details.version |
| B | APPSERVER_KEYNAME | ALNVALUE | Resource identifier |
| B | DATABASESERVER_HOME | ALNVALUE | Component products[0].install_path |

MODELOBJECT_LABEL은 두 묶음에 나타나므로 위 표는 43행, 고유 속성은 42개다.
DB Instance의 다섯 문자열은 Source SQL의 NULLIF(TRIM(value),'')를 거친다.
FQDN·SIGNATURE 등 과거 대조 후보는 이 수집 목록에 포함되지 않는다.
속성 상수 근거:
[ComputerSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/ComputerSpec.java),
[ComputerSystemClusterSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/ComputerSystemClusterSpec.java),
[GenericComputerSystemSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/GenericComputerSystemSpec.java),
[OsSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/OsSpec.java),
[DiskSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/DiskSpec.java),
[FilesystemSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/FilesystemSpec.java),
[IpSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/IpSpec.java),
[DatabaseInstanceSpec](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/ci/mapping/DatabaseInstanceSpec.java).

## 4. 스펙 유효성·누락값 처리

| 조건 | 실제 코드 동작 |
| --- | --- |
| 분류 CLASSIFICATIONID 중복 | 정의 로딩 예외, 본체·관계 미실행 |
| 분류 미등록·ACTCI 적용 없음 | 그 분류의 본체를 건너뜀 |
| 템플릿 (CLASSSTRUCTUREID,ASSETATTRID,SECTION) 중복 | 적용 유효성과 무관하게 정의 로딩 예외 |
| 스펙의 ASSETATTRIBUTEID 미연결·속성명 불일치 | 해당 템플릿 제외 |
| ACTCI용 USEINSPEC=1 설정 없음 | 해당 템플릿 제외 |
| SEQUENCE가 NULL·SMALLINT 범위 밖, MANDATORY가 NULL·0/1 아님 | 해당 템플릿 제외 |
| 정상 템플릿, 원천 NULL·blank | 값 컬럼 NULL인 스펙 행 생성; 기존 값도 NULL로 동기화 |
| 값 있는 ALN + Java String / NUMERIC + BigDecimal | 각각 ALNVALUE / NUMVALUE 생성 |
| 값 있는데 자료형 불일치 | 해당 스펙 생략; TABLEVALUE로 대체하지 않음 |
| 메모리·CPU 속도·Disk 크기 값 있는데 단위 미지원 | 해당 스펙 생략 |
| MANDATORY=1인데 값 없음 | 필수 플래그를 저장할 뿐 자체 필수값 검증은 하지 않음 |

CLASSSPEC·CLASSSPECUSEWITH는 ORGID·SITEID가 NULL인 전역 설정만 사용한다.
스펙을 생략하는 것은 삭제나 기존 값 초기화가 아니다. 생성된 스펙 DTO만 MERGE한다.
코드가 지원하지 않는 타입이라도 값이 없으면 정상 템플릿의 NULL 행을 만들 수 있다.

**BIOSRELEASEDATE 예외:** 정상 템플릿이 없고 원천 값이 있을 때만 추가 경로를 시도한다.
해당 분류에 그 키의 템플릿 자체가 없어야 하며, 전역 ASSETATTRIBUTE의 같은 이름이 정확히 한 건이어야 한다.
CLASSSPECID=NULL, DISPLAYSEQUENCE=180, MANDATORY=0으로 생성하고 공통 자료형 검사를 적용한다.
잘못된 기존 템플릿을 이 경로로 우회하지 않는다. 정상 템플릿이 있으면 그 설정이 우선한다.

## 5. 실제 정의 조회 SQL

다음은 CiDefinitionLoader 원문이다. `:classifications`는 2절의 12개 분류명 전체,
`:classIds`는 첫 조회에서 반환된 CLASSSTRUCTUREID 목록으로 바인딩한다.
분류 조회 결과가 0건이면 스펙 조회를 생략한다. 속성 정의는 전체를 읽는다.

### 분류

```sql
SELECT s.CLASSIFICATIONID,s.CLASSSTRUCTUREID
FROM MAXIMO.CLASSSTRUCTURE s
WHERE s.CLASSIFICATIONID IN (:classifications)
  AND EXISTS (
      SELECT 1 FROM MAXIMO.CLASSUSEWITH u
      WHERE u.CLASSSTRUCTUREID=s.CLASSSTRUCTUREID AND u.OBJECTNAME='ACTCI'
  )
```

### 속성 정의

```sql
SELECT ASSETATTRIBUTEID,ASSETATTRID,DATATYPE,MEASUREUNITID,ORGID,SITEID
FROM MAXIMO.ASSETATTRIBUTE
```

### 분류별 템플릿과 ACTCI 적용 설정

```sql
SELECT c.CLASSSTRUCTUREID,c.CLASSSPECID,c.ASSETATTRID,c.ASSETATTRIBUTEID,
    c.SECTION,c.MEASUREUNITID,c.LINKEDTOATTRIBUTE,c.LINKEDTOSECTION,
    u.SEQUENCE,u.MANDATORY
FROM MAXIMO.CLASSSPEC c
LEFT JOIN MAXIMO.CLASSSPECUSEWITH u
  ON u.CLASSSPECID=c.CLASSSPECID AND u.OBJECTNAME='ACTCI' AND u.USEINSPEC=1
  AND u.CLASSSTRUCTUREID=c.CLASSSTRUCTUREID AND u.ASSETATTRID=c.ASSETATTRID
  AND (u.SECTION=c.SECTION OR (u.SECTION IS NULL AND c.SECTION IS NULL))
  AND u.ORGID IS NULL AND u.SITEID IS NULL
WHERE c.CLASSSTRUCTUREID IN (:classIds)
  AND c.ORGID IS NULL AND c.SITEID IS NULL
```

## 6. DB에 등록된 전체 스펙과 수집 스펙의 구분

코드만으로 현재 환경의 전체 CLASSSPEC 목록을 확정할 수 없다. 3절은 수집 목록이며
DB에 이보다 많은 속성이 있어도 자동으로 수집하지 않는다.
등록 스펙 전체를 재조회하려면 다음 읽기 전용 SQL을 사용한다.
`:classifications`에 2절 목록을 바인딩한다. LEFT JOIN 결과의 적용 설정 누락·중복을 숨기지 않는다.

```sql
SELECT s.CLASSIFICATIONID, s.CLASSSTRUCTUREID, c.CLASSSPECID,
       c.ASSETATTRID, c.SECTION, a.DATATYPE, c.MEASUREUNITID,
       c.ORGID, c.SITEID, u.OBJECTNAME, u.USEINSPEC,
       u.SEQUENCE, u.MANDATORY
FROM MAXIMO.CLASSSTRUCTURE s
LEFT JOIN MAXIMO.CLASSSPEC c ON c.CLASSSTRUCTUREID = s.CLASSSTRUCTUREID
LEFT JOIN MAXIMO.ASSETATTRIBUTE a
  ON a.ASSETATTRIBUTEID = c.ASSETATTRIBUTEID AND a.ASSETATTRID = c.ASSETATTRID
LEFT JOIN MAXIMO.CLASSSPECUSEWITH u
  ON u.CLASSSPECID = c.CLASSSPECID AND u.OBJECTNAME = 'ACTCI'
  AND u.CLASSSTRUCTUREID = c.CLASSSTRUCTUREID AND u.ASSETATTRID = c.ASSETATTRID
  AND (u.SECTION = c.SECTION OR (u.SECTION IS NULL AND c.SECTION IS NULL))
WHERE s.CLASSIFICATIONID IN (:classifications)
ORDER BY s.CLASSIFICATIONID, c.ASSETATTRID, c.SECTION;
```

이 SQL은 등록 현황 확인용이며 실행 캐시의 필터·유효성 검사를 대체하지 않는다.
과거 등록 관측은 [Computer](../../knowledge/maximo/computer-classification-specs.md),
[OS·Disk·Filesystem·IP](../../knowledge/maximo/ci-component-classifications.md),
[DB Instance](../../knowledge/maximo/db-classification-specs.md)에 날짜와 함께 보존한다.
관측된 ID·속성 수를 환경 독립 상수로 사용하지 않는다.
이 ETL은 ACTCI·ACTCISPEC을 적재하며 Authorized CI 승격은 수행하지 않는다.
