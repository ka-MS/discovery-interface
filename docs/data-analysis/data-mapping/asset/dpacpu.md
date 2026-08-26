# DPACPU

배치된 자산 컴퓨터 프로세서

> Target: MAXIMO.DPACPU · ASSETCLASS: COMPUTER · 구현: DpaCpuIntegrate.java

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : <1|N> DPACPU  <!-- MERGE 키로 확정한다 -->
- 선행: DEPLOYEDASSET

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
|  | MAXIMO.DPACPU |  |  |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
|  |  |  |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N |  |  |  |
| CPUID | CPU ID | BIGINT(19) | N |  |  |  |
| CPUNUM | 프로세서 ID | ALN(64) | Y |  |  |  |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N |  |  |  |
| CURRSPEED | 현재 속도 | DECIMAL(10,2) | Y |  |  |  |
| DESCRIPTION | 설명 | ALN(256) | Y |  |  |  |
| IS64BITEN | 64비트 사용 | YORN(1) | N |  |  | DEFAULTVALUE=0 |
| MAKEMODEL | 제조/모델 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| MANUFACTURER | 제조업체 | ALN(128) | N |  |  | DEFAULTVALUE=UNKNOWN |
| MAXSPEED | 최대 속도 | DECIMAL(10,2) | Y |  |  |  |
| NODEID | 노드 ID | BIGINT(19) | N |  |  |  |
| NUMACTIVECORE | 활성 코어 | INTEGER(12) | Y |  |  |  |
| NUMCORE | 코어 | INTEGER(12) | Y |  |  |  |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y |  |  |  |
| SPEEDUNIT | 속도 단위 | ALN(16) | Y |  |  |  |
| TLOAMCPUTYPE | 프로세서 유형 | ALN(128) | Y |  |  |  |
| TLOAMFAMILY | 프로세서 제품군 | ALN(128) | Y |  |  |  |
| VCURRSPEED | 현재 속도 | ALN(32) | Y |  |  |  |
| VMAXSPEED | 최대 속도 | ALN(32) | Y |  |  |  |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.

## 6. 미결

`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.

