# DPALOGICALDRIVE

배치된 자산 컴퓨터 논리 드라이브

> Target: MAXIMO.DPALOGICALDRIVE · ASSETCLASS: COMPUTER · 구현: DpaLogicalDriveIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> DPALOGICALDRIVE  <!-- MERGE 키로 확정한다 -->
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DPALOGICALDRIVE |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ATTACHEDNETNAME | 첨부된 네트워크 이름 | ALN(256) | Y |  |  |  |
| AVAILABLESIZE | 가용 크기 | DECIMAL(10,2) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| COMPRESSED | 압축됨 | YORN(1) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| DRIVETYPE | 드라이브 유형 | ALN(32) | Y |  |  |  |
| ENCRYPTED | 비밀번호화됨 | YORN(1) | N |  |  |  |
| FILESYSTEM | 파일 시스템 | ALN(32) | Y |  |  |  |
| LOGICALDRIVEID | 논리 드라이브 ID | BIGINT(19) | N |  |  |  |
| MOUNT | 드라이브 | ALN(256) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| SIZEUNIT | 크기 단위 | ALN(16) | Y |  |  |  |
| TOTALSIZE | 총 크기 | DECIMAL(10,2) | Y |  |  |  |
| VAVAILABLESIZE | 가용 크기 | ALN(32) | Y |  |  |  |
| VOLUMELABEL | 볼륨 레이블 | ALN(16) | Y |  |  |  |
| VTOTALSIZE | 총 크기 | ALN(32) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

