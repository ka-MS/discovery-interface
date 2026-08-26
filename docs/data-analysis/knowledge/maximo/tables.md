# DPA* 테이블

> 관측 2026-08-27 · Maximo BLUDB
> 재조회 docs/data-analysis/exploration-queries/maximo/column-skeleton.sql

매핑 대상 14개 테이블의 한글명과 컬럼 수다. 컬럼 목록 기준은
`MAXIMO.MAXATTRIBUTE` 다. `SYSCAT.COLUMNS` 에 있는 `ROWSTAMP` 등 시스템
컬럼은 제외된다.

| 테이블 | 한글명 | 컬럼 수 |
| --- | --- | --- |
| DEPLOYEDASSET | 배치된 자산 | 39 |
| DPACOMPUTER | 배치된 자산 컴퓨터 | 42 |
| DPACPU | 배치된 자산 컴퓨터 프로세서 | 19 |
| DPADISK | 배치된 자산 컴퓨터 디스크 | 19 |
| DPADISPLAY | 배치된 자산 컴퓨터 디스플레이 | 14 |
| DPALOGICALDRIVE | 배치된 자산 컴퓨터 논리 드라이브 | 17 |
| DPAMEDIAADAPTER | 배치된 자산 컴퓨터 미디어 어댑터 | 16 |
| DPANETADAPTER | 배치된 자산 컴퓨터 네트워크 어댑터 | 19 |
| DPANETDEVICE | 배치된 자산 네트워크 디바이스 | 12 |
| DPANETPRINTER | 배치된 자산 네트워크 프린터 | 19 |
| DPAOS | 배치된 자산 컴퓨터 운영 체제 | 15 |
| DPASOFTWARE | 배치된 자산 컴퓨터 애플리케이션 | 29 |
| DPASWSUITE | 배치된 자산 컴퓨터 스위트 | 18 |
| DPATCPIP | 배치된 자산 컴퓨터 TCP/IP | 15 |

합계 293개 컬럼. 14개 테이블 모두 한글명이 전건 제공된다.

## 메타데이터 출처

| 항목 | 출처 |
| --- | --- |
| 테이블 한글 설명 | `MAXOBJECT` / `L_MAXOBJECT` (LANGCODE='KO') |
| 컬럼 한글명 | `MAXATTRIBUTE` / `L_MAXATTRIBUTE` (LANGCODE='KO') |
| 타입·길이·소수자리 | `MAXATTRIBUTE.MAXTYPE`, `LENGTH`, `SCALE` |
| 필수 여부 | `MAXATTRIBUTE.REQUIRED` |
| 기본값 | `MAXATTRIBUTE.DEFAULTVALUE` |
