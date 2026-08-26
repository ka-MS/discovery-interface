# DPASOFTWARE

배치된 자산 컴퓨터 애플리케이션

> Target: MAXIMO.DPASOFTWARE · ASSETCLASS: COMPUTER · 구현: DpaSoftwareIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPASOFTWARE (관측 53노드/13031행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_softwareinuse_v1`, `view_software_v1` | MAXIMO.DPASOFTWARE |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| FIRSTENCOUNTERED1 | 첫 번째 발견 날짜 | DATETIME(10) | Y |  |  |  |
| INSTALLDATE | 설치 날짜 | DATETIME(10) | Y |  |  |  |
| INSTALLPATH | 설치 경로 | ALN(4000) | Y |  |  |  |
| LANGUAGE | 언어 | ALN(32) | Y |  |  |  |
| LASTENCOUNTERED1 | 마지막 발견 날짜 | DATETIME(10) | Y |  |  |  |
| LASTUSAGEDATE | 최종 사용일 | DATETIME(10) | Y |  |  |  |
| LICENSEDORG | 라이센스가 부여된 조직 | ALN(64) | Y |  |  |  |
| LICENSEDUSER | 라이센스가 부여된 사용자 | ALN(64) | Y |  |  |  |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| METRICID | 메트릭 ID | BIGINT(19) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| PRODUCTID | 제품 ID | ALN(128) | Y |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| SOFTWAREID | 소프트웨어 | BIGINT(19) | N |  |  |  |
| SOFTWARENAME | 애플리케이션 | ALN(256) | N |  |  | DEFAULTVALUE=UNKNOWN |
| SUITEID | 스위트 ID | BIGINT(19) | Y |  |  |  |
| SUITENAME | 스위트 | ALN(254) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| TLOAMPRODUCTID | 소프트웨어 | BIGINT(19) | Y |  |  |  |
| TLOAMSOFTWAREID | 소프트웨어 | BIGINT(19) | Y |  |  |  |
| TLOAMUNINSTDATE | 설치 제거 날짜 | DATE(4) | Y |  |  |  |
| TLOAMUSEEXCP | 사용 예외 | UPPER(30) | Y |  |  |  |
| TLOAMUSEEXCPJUST | 조정 비고 | ALN(50) | Y |  |  |  |
| TYPE | 애플리케이션 유형 | ALN(64) | Y |  |  |  |
| USAGECOUNT | 사용 회수 | INTEGER(12) | Y |  |  |  |
| VERSION | 버전 | ALN(128) | Y |  |  |  |
| VUSAGEDISPLAYTEXT | 사용 | ALN(64) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

