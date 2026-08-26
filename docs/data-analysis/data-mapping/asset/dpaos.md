# DPAOS

배치된 자산 컴퓨터 운영 체제

> Target: MAXIMO.DPAOS · ASSETCLASS: COMPUTER · 구현: DpaOsIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPAOS (관측 61노드/63행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_deviceos_v1`, `view_os_v1` | MAXIMO.DPAOS |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| BUILD | 빌드 | ALN(64) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CHARACTERSET1 | 문자 세트 | ALN(32) | Y |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| LANGUAGE | 언어 | ALN(32) | Y |  |  |  |
| LICENSEDORG | 라이센스가 부여된 조직 | ALN(64) | Y |  |  |  |
| LICENSEDUSER | 라이센스가 부여된 사용자 | ALN(64) | Y |  |  |  |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| NAME | 운영 체제 | ALN(256) | N |  |  | DEFAULTVALUE=UNKNOWN |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| OSID | 운영 체제 ID | BIGINT(19) | N |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| SERVICEPACK | 서비스 팩 | ALN(64) | Y |  |  |  |
| VERSION | 버전 | ALN(128) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

