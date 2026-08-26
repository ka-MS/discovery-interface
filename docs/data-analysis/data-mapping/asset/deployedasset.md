# DEPLOYEDASSET

배치된 자산

> Target: MAXIMO.DEPLOYEDASSET · ASSETCLASS: COMPUTER, NETDEVICE, NETPRINTER · 구현: DeployedAssetIntegrate.java

## 1. 관계

- 계층의 루트. 부모 없음.
- `NODEID` 는 `MAXIMO.DEPLOYEDASSETSEQ` 로 발번한다.
- 적재 대상 필터와 키 전략은 3번에 기술한다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DEPLOYEDASSET |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ASSETCLASS | 자산 클래스 | ALN(32) | N |  |  |  |
| ASSETTAG | 자산 태그 | ALN(64) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| DOMAINNAME | 도메인 | ALN(128) | N |  |  |  |
| GUID | 발견 ID | ALN(192) | Y |  |  |  |
| HWDETECTIONTOOL | 하드웨어 검색 도구 | ALN(256) | Y |  |  |  |
| HWLASTSCANDATE | 하드웨어 최종 스캔 날짜 | DATETIME(10) | Y |  |  |  |
| IMPORTSOURCE | 가져오기 소스 | ALN(128) | Y |  |  |  |
| MAKEMODEL | 제조/모델 | ALN(128) | Y |  |  |  |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| NODEID2 | 노드 ID 2 | BIGINT(19) | Y |  |  |  |
| NODENAME | 노드 | ALN(128) | N |  |  |  |
| ORGID | 조직 | UPPER(8) | Y |  |  |  |
| PLUSPCUSTOMER | 고객 | UPPER(12) | Y |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| SITEID | 사이트 | UPPER(8) | Y |  |  |  |
| SOURCEID | 소스 | ALN(128) | Y |  |  |  |
| SOURCEID2 | Source2 | ALN(128) | Y |  |  |  |
| SUPPORTSSNMP | SNMP 지원 | YORN(1) | N |  |  |  |
| SYSTEMROLE | 역할 | ALN(32) | Y |  |  |  |
| TLOAMHASH | 파티션 ID | UPPER(192) | Y |  |  |  |
| TLOAMHWTYPE | 하드웨어 유형 | ALN(32) | Y |  |  |  |
| TLOAMISPROMOTED | 승격 여부 | UPPER(8) | Y |  |  |  |
| TLOAMNRSGUID | 통합 ID | ALN(192) | Y |  |  |  |
| TLOAMNRSHOSTSYSTEM | NRS 호스트 시스템 | ALN(128) | Y |  |  |  |
| TLOAMNRSMANAGEDSYSTEMNAME | NRS 관리 대상 시스템 이름 | ALN(128) | Y |  |  |  |
| TLOAMNRSMANUFACTURER | NRS 제조업체 | ALN(128) | Y |  |  |  |
| TLOAMNRSMODEL | NRS 제조사/모델 | ALN(128) | Y |  |  |  |
| TLOAMNRSNAME | NRS 이름 | ALN(128) | Y |  |  |  |
| TLOAMNRSPRIMARYMACADDRESS | NRS MAC 주소 | ALN(17) | Y |  |  |  |
| TLOAMNRSSERIALNUMBER | NRS 일련 번호 | ALN(128) | Y |  |  |  |
| TLOAMNRSSIGNATURE | NRS 특성 | ALN(128) | Y |  |  |  |
| TLOAMNRSSYSTEMBOARDUUID | NRS 시스템 보드 UUID | ALN(64) | Y |  |  |  |
| TLOAMNRSUUID | NRS 가상 머신 UUID | ALN(64) | Y |  |  |  |
| TLOAMNRSVMID | NRS VMID | ALN(128) | Y |  |  |  |
| TLOAMSTATUS | 상태 | UPPER(20) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

