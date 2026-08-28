# 백업 테이블

> 관측 2026-08-28 · Maximo BLUDB

DPA 계열 원본 테이블의 사본이다. 매핑 구현이 실제로 쓰기를 시작하기 전 상태를
남겨둔 것이다.

## 현황

| 백업 테이블 | 행 | 생성 | 내용 |
| --- | --- | --- | --- |
| `DEPLOYEDASSET_BAK` | 106 | 2026-08-25 | `IMPORTSOURCE` 전건 빈값. Device42 적재 전 기존 수집분 |
| `DEPLOYEDASSET_TMP` | 58 | 2026-08-25 | 전건 `IMPORTSOURCE = 'Device42'` |
| `DPACOMPUTER_BAK` | 68 | 2026-08-26 | 원본 전량 |
| `DPAOS_BAK` | 63 | 2026-08-28 | 원본 전량 |
| `DPASOFTWARE_BAK` | 13031 | 2026-08-28 | 원본 전량 |
| `DPACPU_BAK` | 57 | 2026-08-28 | 원본 전량 |
| `DPADISK_BAK` | 144 | 2026-08-28 | 원본 전량 |
| `DPALOGICALDRIVE_BAK` | 80 | 2026-08-28 | 원본 전량 |
| `DPANETADAPTER_BAK` | 61 | 2026-08-28 | 원본 전량 |
| `DPATCPIP_BAK` | 53 | 2026-08-28 | 원본 전량 |
| `DPAMEDIAADAPTER_BAK` | 39 | 2026-08-28 | 원본 전량 |
| `DPADISPLAY_BAK` | 52 | 2026-08-28 | 원본 전량 |
| `DPASWSUITE_BAK` | 14 | 2026-08-28 | 원본 전량 |
| `DPANETDEVICE_BAK` | 34 | 2026-08-28 | 원본 전량 |
| `DPANETPRINTER_BAK` | 4 | 2026-08-28 | 원본 전량 |

변환 계열은 별도로 떴다. `conversion` 잡이 이 테이블들에 MERGE 하기 전 상태다.

| 백업 테이블 | 행 | 생성 | 내용 |
| --- | --- | --- | --- |
| `TLOAMSOFTWARE_BAK` | 2175 | 2026-08-28 | 소프트웨어 제품 테이블 |
| `DPAMMANUFACTURER_BAK` | 315 | 2026-08-28 | 제조업체 변환 대상 |
| `DPAMMANUVARIANT_BAK` | 315 | 2026-08-28 | 제조업체 변환 변형 |
| `DPAMADAPTER_BAK` | 59 | 2026-08-28 | 어댑터 변환 대상 |
| `DPAMADPTVARIANT_BAK` | 59 | 2026-08-28 | 어댑터 변환 변형 |
| `DPAMOS_BAK` | 23 | 2026-08-28 | 운영체제 변환 대상 |
| `DPAMOSVARIANT_BAK` | 23 | 2026-08-28 | 운영체제 변환 변형 |
| `DPAMPROCESSOR_BAK` | 13 | 2026-08-28 | 프로세서 변환 대상 |
| `DPAMPROCVARIANT_BAK` | 13 | 2026-08-28 | 프로세서 변환 변형 |

2026-08-28 생성분은 행 수와 컬럼 수가 원본과 전건 일치한다.

## 만든 방식

```sql
CREATE TABLE MAXIMO.<원본>_BAK AS (
    SELECT * FROM MAXIMO.<원본>
) WITH DATA;
```

`WITH DATA` 는 컬럼 이름·타입·NOT NULL 만 가져온다. 다음은 복사되지 않는다.

- 기본키·유니크·체크·외래키 제약
- 인덱스. 관측 시점에 모든 백업 테이블의 인덱스가 0개다
- 트리거, 권한, 컬럼 기본값
- IDENTITY·생성 컬럼 속성

`MAXOBJECT` 에 등록되어 있지 않다. Maximo 애플리케이션은 이 테이블들을 모른다.

## 복원 시 주의

백업본에 유니크 제약이 없어 `INSERT ... SELECT` 로 원본에 되돌리면 중복을
막을 수단이 없다. 변환 계열은 원본이 이름 컬럼에 유일 인덱스를 걸어 동작하므로
특히 주의한다.

| 원본 | 유일 인덱스 컬럼 |
| --- | --- |
| `DPAMMANUFACTURER` | `MANUFACTURERNAME` |
| `DPAMMANUVARIANT` | `MANUFACTURERVAR` |
| `DPAMOS` / `DPAMOSVARIANT` | `OSNAME` / `OSVARIANT` |
| `DPAMPROCESSOR` / `DPAMPROCVARIANT` | `PROCESSORNAME` / `PROCESSORVAR` |
| `DPAMADAPTER` / `DPAMADPTVARIANT` | `ADAPTERNAME` / `ADAPTERVARIANT` |
| `TLOAMSOFTWARE` | `UNIQUEID` |

`conversion` 잡의 MERGE 는 `WHEN NOT MATCHED` 뿐이라 멱등하다. 변환 계열을
되돌릴 때는 원본을 비운 뒤 잡을 다시 실행하는 편이 안전하다. `DEPLOYEDASSET_BAK` 은 기존 수집분 106행이라 살아 있는
같은 행과 `NODEID` 가 통째로 충돌한다.

Device42 적재는 `NODEID = device_pk`로 MERGE한다. 적재분을 되돌릴 때는 해당
행을 지우고 잡을 다시 실행하면 같은 `NODEID`로 복원된다.

## 재조회

```sql
SELECT TABNAME, CARD, COLCOUNT, CREATE_TIME
FROM SYSCAT.TABLES
WHERE TABSCHEMA = 'MAXIMO' AND TYPE = 'T'
  AND (TABNAME LIKE '%\_BAK' ESCAPE '\' OR TABNAME LIKE '%\_TMP' ESCAPE '\')
ORDER BY TABNAME;
```

`CARD` 는 통계 기준 추정값이라 실제 행 수와 다를 수 있다. 정확한 값은
`COUNT(*)` 로 센다.
