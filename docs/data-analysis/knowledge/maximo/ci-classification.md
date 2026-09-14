# CI 분류 모델

> 관측 2026-09-04 · Maximo BLUDB
> 재조회 [컬럼·관계](../../exploration-queries/maximo/ci-target-structure.sql),
> [적용 범위·정합성](../../exploration-queries/maximo/ci-classification-audit.sql),
> [계층·대표 속성](../../exploration-queries/maximo/ci-classification-templates.sql),
> [분류별 정의 충족 여부](../../exploration-queries/maximo/ci-definition-coverage.sql)

## 분류와 속성의 역할

Computer 수집 항목에 대한 최신 CI/ACTCI 분류·스펙·관계 대조는
[Computer 관련 분류 조사](computer-classification-specs.md)를 참조한다.

| 테이블 | 역할 | 연결 키 |
| --- | --- | --- |
| CLASSSTRUCTURE | 분류 계층 | CLASSSTRUCTUREID; PARENT가 상위 CLASSSTRUCTUREID 참조 |
| CLASSUSEWITH | 분류의 적용 오브젝트 | CLASSSTRUCTUREID + OBJECTNAME |
| CLASSSPEC | 분류별 속성 템플릿 | CLASSSTRUCTUREID; CLASSSPECID |
| CLASSSPECUSEWITH | 오브젝트별 필수 여부·순서·기본값 | CLASSSPECID + OBJECTNAME |
| ASSETATTRIBUTE | 속성 정의와 DATATYPE | ASSETATTRIBUTEID; ASSETATTRID |
| ACTCISPEC | Actual CI 한 건의 속성 값 | REFOBJECTID; CLASSSPECID |

`CLASSSPEC`은 속성 값 테이블이 아니다. `MANDATORY`, `SEQUENCE`,
`USEINSPEC`, `USEINDESC`, `DEFAULTALNVALUE/DEFAULTNUMVALUE/DEFAULTTABLEVALUE`는
`CLASSSPECUSEWITH`에 있다. `CLASSSPEC.DATATYPE`은 비영속 속성이고, 저장된
데이터형은 `ASSETATTRIBUTE.DATATYPE`에서 조회한다.

## 적용 범위

| 항목 | ACTCI | CI |
| --- | ---: | ---: |
| CLASSUSEWITH 분류 수 | 1,030 | 177 |
| CLASSSPECUSEWITH 템플릿 수 | 32,992 | 1,665 |
| ALN 속성 | 23,275 | 1,222 |
| NUMERIC 속성 | 9,717 | 443 |
| MANDATORY=1 | 0 | 0 |
| 기본값 보유 | 0 | 0 |
| USEINSPEC=1 | 32,992 | 1,665 |

두 오브젝트에 공통 적용되는 CLASSSTRUCTUREID는 **0개**다.
분류 이름 접두어가 아니라 `CLASSUSEWITH.OBJECTNAME`으로 적용 범위를 확인한다.

위 집계에 포함된 양쪽 템플릿은 CLASSSPEC의 분류·속성·섹션, CLASSUSEWITH, ASSETATTRIBUTE와
전건 일치한다. 이 범위에서 INHERITEDFROMID·DOMAINID는 모두 NULL이고
APPLYDOWNHIER는 0이다. ASSETATTRIBUTE.DOMAINID도 모두 NULL이다.

이 집계는 등록된 CLASSSPECUSEWITH 기준이다. ACTCI 분류에서 역으로 조회하면
40개 분류의 CLASSSPEC 1,905건에 ACTCI용 적용 설정이 없다. 이 1,905건은
ASSETATTRIBUTEID로 연결되지 않지만 ASSETATTRID로는 속성 정의가 존재한다.

ACTCI 분류는 루트 1개와 부모가 존재하는 1,029개다. CI는 루트 2개,
부모가 존재하는 174개, 부모가 없는 1개다. 마지막 행은
`CI.GENERIC_COMPUTERSYSTEM`이며 PARENT가 SQL NULL이 아닌 문자열 `'null'`이다.
양쪽 분류의 ORGID·SITEID는 전건 NULL이다.

## 확인된 ACTCI 분류와 속성 설정

관측 ID는 조회 결과이며 적재용 상수가 아니다. 아래 모든 분류에 CLASSUSEWITH의
ACTCI 설정은 있다. 속성 적용 수는 CLASSSPECUSEWITH, 속성 ID 연결 수는
CLASSSPEC.ASSETATTRIBUTEID와 ASSETATTRID가 같은 ASSETATTRIBUTE 행 기준이다.

| CLASSIFICATIONID | CLASSSTRUCTUREID | 속성 수 | ACTCI 속성 적용 | 속성 ID 연결 |
| --- | --- | ---: | ---: | ---: |
| APP.DB.DATABASE | CCI20000 | 34 | 0 | 0 |
| APP.DB.DATABASESERVER | CCI10162 | 42 | 42 | 42 |
| APP.DB.MSSQL.SQLSERVER | CCI10114 | 48 | 48 | 48 |
| APP.DB.MSSQL.SQLSERVERDATABASE | CCI10391 | 32 | 32 | 32 |
| APP.DB.DB2.DB2INSTANCE | CCI10954 | 52 | 52 | 52 |
| APP.DB.ORACLE.ORACLEINSTANCE | CCI10153 | 46 | 46 | 46 |
| SYS.COMPUTERSYSTEM | CCI10470 | 98 | 98 | 98 |
| SYS.VIRTUALCOMPUTERSYSTEM | CCI10793 | 98 | 98 | 98 |
| SERVICE.SERVICEINSTANCE | CCI20015 | 39 | 0 | 0 |
| PROCESS.BUSINESSSERVICE | CCI10387 | 31 | 31 | 31 |
| APP.APPLICATION | CCI10676 | 34 | 34 | 34 |
| APP.WEB.WEBSERVER | CCI10828 | 41 | 41 | 41 |
| NET.IPNETWORK | CCI10390 | 25 | 25 | 25 |
| NET.VLAN | CCI10682 | 32 | 32 | 32 |

APP.DB.DATABASE 34건과 SERVICE.SERVICEINSTANCE 39건의 ASSETATTRIBUTEID는
모두 0이다. 두 분류는 ASSETATTRID로 조회하면 각 속성 정의가 정확히 1건씩 존재한다.

ACTCI 적용 분류의 CLASSIFICATIONID와 DESCRIPTION에서 `KUBERNET`, `K8S`,
`DOCKER`, `CLOUD`, `EC2`, `VRF`, `STORAGEARRAY` 검색 결과는 각각 0건이다.
다른 명칭의 대응 분류 여부는 이 검색으로 확인되지 않는다.

## 대표 속성의 타입·단위

| 분류 | ASSETATTRID | 한글 의미 | DATATYPE |
| --- | --- | --- | --- |
| SYS.COMPUTERSYSTEM | COMPUTERSYSTEM_SERIALNUMBER / COMPUTERSYSTEM_UUID | 일련번호 / UUID | ALN |
| SYS.COMPUTERSYSTEM | COMPUTERSYSTEM_NUMCPUS | CPU 수 | NUMERIC |
| SYS.COMPUTERSYSTEM | COMPUTERSYSTEM_MEMORYSIZE / COMPUTERSYSTEM_CPUSPEED | 메모리 / CPU 속도 | NUMERIC |
| APP.DB.MSSQL.SQLSERVERDATABASE | SQLSERVERDATABASE_NAME | DB 이름 | ALN |
| APP.DB.MSSQL.SQLSERVERDATABASE | SQLSERVERDATABASE_DBID | DB ID | NUMERIC |
| APP.DB.MSSQL.SQLSERVERDATABASE | SQLSERVERDATABASE_CREATIONDATE | DB 생성 시각 | ALN |
| NET.IPNETWORK | IPNETWORK_PREFIXLENGTH | Prefix 길이 | NUMERIC |
| NET.VLAN | VLAN_VLANID | VLAN 번호 | NUMERIC |

이 속성들의 CLASSSPEC·ASSETATTRIBUTE 단위는 모두 NULL이다. ACTCI용 적용 설정은
MANDATORY=0, USEINSPEC=1이고 기본값은 없다. 타입이 같아도 단위·코드 의미가
같다는 뜻은 아니다. 위 타입은 속성 정의이며 ACTCISPEC의 저장 컬럼과 구분한다.

[ISSUE-11](../../open-issues.md#issue-11-actual-ci-분류속성관계와-식별자-매핑) — 원천별 분류·속성 선택과 누락된 적용 설정의 보완 여부 미정.

## CLASSSTRUCTURE 컬럼

자산 분류의 구조화된 계층. 전체 1,820행, 영속 31개 컬럼이다.
물리 PK는 숫자 `CLASSSTRUCTUREUID`, 고유 인덱스는 문자열 `CLASSSTRUCTUREID`다.
다른 CI 테이블과 PARENT는 UID가 아닌 ID를 참조한다.

아래 두 표는 MAXATTRIBUTE와 한국어 TITLE 기준이다. Null은 REQUIRED의 반전이며,
기본값·SameAs는 Maximo 설정이지 물리 DEFAULT·FK를 뜻하지 않는다.
비영속 컬럼과 ROWSTAMP는 제외한다.

| 컬럼 | 한글명 | 타입 | Null | 기본값 / 참조 |
| --- | --- | --- | --- | --- |
| CLASSIFICATIONGROUPID | 분류 그룹 ID | ALN(200) | Y | – |
| CLASSIFICATIONID | 분류 | UPPER(192) | N | SameAs: `CLASSIFICATION.CLASSIFICATIONID` |
| CLASSSTRUCTUREID | 클래스 구조 | UPPER(25) | N | 기본값 `&AUTOKEY&` |
| CLASSSTRUCTUREUID | CLASSSTRUCTUREUID | BIGINT(19) | N | – |
| COMMODITY | 상품 | UPPER(8) | Y | SameAs: `COMMODITIES.COMMODITY` |
| COMMODITYGROUP | 상품 그룹 | UPPER(8) | Y | SameAs: `COMMODITIES.COMMODITY` |
| DESCRIPTION | 설명 | ALN(254) | Y | – |
| DISCLASSIFICATION | DIS 분류 | ALN(192) | Y | – |
| GENASSETDESC | 설명 생성 | YORN(1) | N | 기본값 `1` |
| HASCHILDREN | 하위 있음 | YORN(1) | N | – |
| HASLD | 상세 설명 있음 | YORN(1) | N | 기본값 `0` |
| INDICATEDPRIORITY | 표시된 우선순위 | INTEGER(12) | Y | 도메인 `TICKETPRIORITY` |
| LANGCODE | 언어 코드 | UPPER(4) | N | SameAs: `LANGUAGE.MAXLANGCODE` |
| ORGID | 조직 | UPPER(8) | Y | SameAs: `ORGANIZATION.ORGID` |
| OWNER | 소유자 | UPPER(30) | Y | – |
| PARENT | 상위 클래스 구조 | UPPER(25) | Y | SameAs: `CLASSSTRUCTURE.CLASSSTRUCTUREID` |
| PERSON | 사용자 | UPPER(100) | Y | SameAs: `PERSON.PERSONID` |
| PERSONGROUP | 개인 그룹 | UPPER(8) | Y | SameAs: `PERSONGROUP.PERSONGROUP` |
| PLUSPINSERTCUSTOMER | 삽입된 고객 | UPPER(12) | Y | SameAs: `PLUSPCUSTOMER.CUSTOMER` |
| PLUSPISGLOBAL | 글로벌함 | YORN(1) | N | 기본값 `1` |
| PLUSPROLLDOWN | 고객을 하위와 연관 | YORN(1) | N | 기본값 `1` |
| PLUSPROLLDOWNATTR | 고객을 속성과 연관 | YORN(1) | N | 기본값 `1` |
| PMCOMTOPOIMG | 토폴로지에서 사용된 이미지 | ALN(100) | Y | SameAs: `PMCOMTOPOIMG.NAME` |
| SHOW | 서비스 요청 작업 센터에 표시 | YORN(1) | N | 기본값 `1` |
| SHOWINASSETTOPO | 토폴로지에 자산 표시 | YORN(1) | Y | 기본값 `0` |
| SHOWINIA | 영향 분석 결과에 표시 | YORN(1) | Y | 기본값 `0` |
| SHOWINTOPO | CI 토폴로지 비즈니스 보기에 표시 | YORN(1) | Y | 기본값 `0` |
| SITEID | 사이트 | UPPER(8) | Y | SameAs: `SITE.SITEID` |
| SORTORDER | 주문 | INTEGER(12) | Y | – |
| TYPE | 유형 | UPPER(10) | Y | – |
| USECLASSINDESC | 분류 사용 | YORN(1) | N | – |

## CLASSSPEC 컬럼

자산 사양에 대한 분류 템플리트. 전체 39,237행, 영속 26개 컬럼이다.
물리 PK는 `CLASSSPECID`, 고유 인덱스는
`(CLASSSTRUCTUREID, ASSETATTRID, SECTION, ORGID, SITEID)`다.

| 컬럼 | 한글명 | 타입 | Null | 기본값 / 참조 |
| --- | --- | --- | --- | --- |
| APPLYDOWNHIER | 하위 계층 적용 | YORN(1) | N | 기본값 `0` |
| ASSETATTRIBUTEID | 자산 속성 ID | BIGINT(19) | Y | SameAs: `ASSETATTRIBUTE.ASSETATTRIBUTEID` |
| ASSETATTRID | 속성 | UPPER(300) | N | SameAs: `ASSETATTRIBUTE.ASSETATTRID` |
| ATTRDESCPREFIX | 설명 접두어 | ALN(8) | Y | SameAs: `ASSETATTRIBUTE.ATTRDESCPREFIX` |
| CLASSSPECID | 분류 | BIGINT(19) | N | – |
| CLASSSTRUCTUREID | 클래스 구조 | UPPER(25) | N | SameAs: `CLASSSTRUCTURE.CLASSSTRUCTUREID` |
| CONTINUOUS | 연속 | YORN(1) | N | 기본값 `0`; SameAs: `FEATURES.CONTINUOUS` |
| CS01 | Cs01 | ALN(10) | Y | – |
| CS02 | Cs02 | ALN(10) | Y | – |
| CS03 | Cs03 | ALN(10) | Y | – |
| CS04 | Cs04 | DATETIME(10) | Y | – |
| CS05 | Cs05 | DECIMAL(15,2) | Y | – |
| DOMAINID | 도메인 | UPPER(18) | Y | SameAs: `MAXDOMAIN.DOMAINID` |
| INHERITEDFROM | 상속 소스 | ALN(254) | Y | SameAs: `CLASSSTRUCTURE.HIERARCHYPATH` |
| INHERITEDFROMID | ClassSpec에서 상속 | BIGINT(19) | Y | SameAs: `CLASSSPEC.CLASSSPECID` |
| LINEARTYPE | 선형 유형 | UPPER(12) | Y | SameAs: `FEATURES.FEATURETYPE`; 도메인 `FEATURETYPE` |
| LINKEDTOATTRIBUTE | 속성에 연결됨 | UPPER(300) | Y | SameAs: `ASSETATTRIBUTE.ASSETATTRID` |
| LINKEDTOSECTION | 섹션에 연결됨 | UPPER(10) | Y | SameAs: `CLASSSPEC.SECTION` |
| LOOKUPNAME | 검색 이름 | ALN(30) | Y | – |
| MEASUREUNITID | 측정 단위 | UPPER(16) | Y | SameAs: `MEASUREUNIT.MEASUREUNITID` |
| ORGID | 조직 | UPPER(8) | Y | SameAs: `ORGANIZATION.ORGID` |
| PLUSPINSERTCUSTOMER | 삽입된 고객 | UPPER(12) | Y | SameAs: `PLUSPCUSTOMER.CUSTOMER` |
| PLUSPISGLOBAL | 글로벌함 | YORN(1) | N | 기본값 `1` |
| SECTION | 섹션 | UPPER(10) | Y | – |
| SITEID | 사이트 | UPPER(8) | Y | SameAs: `SITE.SITEID` |
| TABLEATTRIBUTE | 테이블 속성 | UPPER(50) | Y | SameAs: `MAXATTRIBUTE.ATTRIBUTENAME` |
