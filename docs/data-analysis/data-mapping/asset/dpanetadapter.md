# DPANETADAPTER

배치된 자산 컴퓨터 네트워크 어댑터

> Target: MAXIMO.DPANETADAPTER · ASSETCLASS: COMPUTER · 구현: DpaNetAdapterIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPANETADAPTER (관측 39노드/61행)
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_netport_v1` | MAXIMO.DPANETADAPTER |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ADAPTERID | 어댑터 | BIGINT(19) | N |  |  |  |
| ADAPTERTYPE | 어댑터 유형 | ALN(32) | Y |  |  |  |
| ASSETTAG | 자산 태그 | ALN(64) | Y |  |  |  |
| BANDWIDTH | 대역폭 | DECIMAL(10,2) | Y |  |  |  |
| BANDWIDTHUNIT | 대역폭 단위 | ALN(16) | Y |  |  |  |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CHIPSET | 칩셋 | ALN(64) | Y |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| FIRMWAREVERSION | 펌웨어 버전 | ALN(32) | Y |  |  |  |
| MAKEMODEL | 제조/모델 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| NETMACADDR1 | MAC 주소 1 | ALN(17) | Y |  |  |  |
| NETMACADDR2 | MAC 주소 2 | ALN(17) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| PORT | 포트 | ALN(16) | Y |  |  |  |
| PROTOCOL | 프로토콜 | ALN(64) | Y |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| VBANDWIDTH | 대역폭 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

