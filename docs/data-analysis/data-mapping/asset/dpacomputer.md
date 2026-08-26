# DPACOMPUTER

배치된 자산 컴퓨터

> Target: MAXIMO.DPACOMPUTER · ASSETCLASS: COMPUTER · 구현: DpaComputerIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> DPACOMPUTER  <!-- MERGE 키로 확정한다 -->
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DPACOMPUTER |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| BIOSDATE | BIOS 날짜 | DATETIME(10) | Y |  |  |  |
| BIOSNAME | BIOS | ALN(64) | Y |  |  |  |
| BIOSPNP | PNP | YORN(1) | N |  |  |  |
| BIOSVERSION | BIOS 버전 | ALN(32) | Y |  |  |  |
| CAPACITYMODEL1 | 용량 모델 | ALN(128) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| LOGONNAME | 로그온 | ALN(64) | Y |  |  |  |
| MIPS1 | MIPS 용량 | INTEGER(12) | Y |  |  |  |
| MOBOASSETTAG | 자산 태그 | ALN(64) | Y |  |  |  |
| MOBOCHIPSET | 칩셋 | ALN(128) | Y |  |  |  |
| MOBODESCRIPTION | 마더보드 설명 | ALN(256) | Y |  |  |  |
| MOBOMAKEMODEL | 제조/모델 | ALN(128) | Y |  |  |  |
| MOBOMANUFACTURER | 제조업체 | ALN(128) | Y |  |  |  |
| MOBOSERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| MSUS1 | MSU 용량 | INTEGER(12) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| NUMCORETOTAL | 총 코어 | INTEGER(12) | Y |  |  |  |
| NUMCPUCONFIG1 | 구성된 프로세서 수 | INTEGER(12) | Y |  |  |  |
| NUMCPUTOTAL1 | 총 프로세서 수 | INTEGER(12) | Y |  |  |  |
| PLANTCODE1 | 제조 공장 | ALN(32) | Y |  |  |  |
| RAMDESCRIPTION | RAM 설명 | ALN(256) | Y |  |  |  |
| RAMSIZE | RAM 크기 | DECIMAL(10,2) | Y |  |  |  |
| RAMTOTALSLOTS | RAM 총 슬롯 | INTEGER(12) | Y |  |  |  |
| RAMTYPE | RAM 유형 | ALN(32) | Y |  |  |  |
| RAMUNIT | RAM 단위 | ALN(16) | Y |  |  |  |
| RAMUNUSEDSLOTS | RAM 미사용 슬롯 | INTEGER(12) | Y |  |  |  |
| SMBIOS | SMBIOS | YORN(1) | N |  |  |  |
| SUPPORTSWMI | WMI 지원 | YORN(1) | N |  |  |  |
| SWDETECTIONTOOL | 소프트웨어 검색 도구 | ALN(256) | Y |  |  |  |
| SWLASTSCANDATE | 소프트웨어 최종 스캔 날짜 | DATETIME(10) | Y |  |  |  |
| TLOAMAUTHUSERID | 인증된 사용자 ID | ALN(256) | Y |  |  |  |
| TLOAMCARRIER | 모바일 네트워크 사업자 | ALN(64) | Y |  |  |  |
| TLOAMDEVOWNERSHIP | 모바일 디바이스 소유권 | UPPER(50) | Y |  |  |  |
| TLOAMDEVPHONENUM | 전화번호 | ALN(20) | Y |  |  |  |
| TLOAMDEVPWDCOMPL | 비밀번호 준수 | YORN(1) | Y |  |  |  |
| TLOAMDEVPWDENBLD | 비밀번호 사용 | YORN(1) | Y |  |  |  |
| TLOAMIMEI | IMEI | ALN(64) | Y |  |  |  |
| TLOAMPARENTID | 상위 노드 Id | BIGINT(19) | Y |  |  |  |
| TLOAMPARENTNAME | 상위 | ALN(128) | Y |  |  |  |
| TLOAMPLATFORMBASE | 플랫폼 | UPPER(20) | Y |  |  |  |
| VRAMSIZE | RAM 크기 | ALN(32) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

