# 변환 데이터

> 관측 2026-08-28 · Maximo BLUDB
> 재조회 `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

Maximo UI 는 `DPA*` 자식 테이블을 직접 읽지 않는다. 자식 위에 얹힌 뷰를 읽고,
그 뷰가 변환 변형와 **INNER 조인**한다. 값이 변환 대상에 없으면 행이 오류 없이
사라진다.

따라서 변환 대상도 자식과 동일하게 **배치가 매 실행 Device42 에서 조회해 적재하는
대상**이다. 일회성 등록이 아니다. 새 제조사·모델·OS 가 원천에 나타나면 그
실행에서 함께 등록되어야 자식 행이 화면에 보인다.

배경은 `../../open-issues.md` ISSUE-6 참조.

## 자식 테이블이 UI 에 보이기 위한 조건

| 자식 테이블 | UI 뷰 | 요구 변환 대상 | 조인 대상 컬럼 |
| --- | --- | --- | --- |
| DPACOMPUTER | `COMPUTERSYSTEM` | 제조사 | `DEPLOYEDASSET.MANUFACTURER` |
| DPANETDEVICE | `NETDEVICE` | 제조사 | `DEPLOYEDASSET.MANUFACTURER` |
| DPANETPRINTER | `NETPRINTER` | 제조사 | `DEPLOYEDASSET.MANUFACTURER` |
| DPADISK | `DPACDISK` | 제조사 | `DPADISK.MANUFACTURER` |
| DPADISPLAY | `DPACDISPLAY` | 제조사 | `DPADISPLAY.MANUFACTURER` |
| DPASWSUITE | `DPACSWSUITE` | 제조사 | `DPASWSUITE.MANUFACTURER` |
| DPACPU | `DPACCPU` | 제조사 + 프로세서 | `MANUFACTURER`, `MAKEMODEL` |
| DPANETADAPTER | `DPACNETADAPTER` | 제조사 + 어댑터 | `MANUFACTURER`, `MAKEMODEL` |
| DPAMEDIAADAPTER | `DPACMEDIAADAPTER` | 제조사 + 어댑터 | `MANUFACTURER`, `MAKEMODEL` |
| DPAOS | `DPACOS` | 제조사 + 운영체제 | `MANUFACTURER`, `NAME` |
| DPASOFTWARE | `DPACSOFTWARE` | TLOAMSOFTWARE 경유 제조사 | `TLOAMSOFTWAREID` |

제조사는 모든 뷰가 요구한다. 도메인 뷰는 값 컬럼용 변환 대상를 하나 더 요구한다.

## 원천

다른 태스크와 같다. Device42 를 조회해 Maximo 에 적재한다.

| 변환 대상 | Device42 원천 | 도달 경로 |
| --- | --- | --- |
| 제조사 | `view_vendor_v1.name` | `hardware`·`partmodel`·`os`·`netport`·`software` 의 `vendor_fk` 합집합 |
| 운영체제 | `view_deviceos_v1.os_name` | – |
| 프로세서 | `view_partmodel_v1.name` | `view_part_v1` 경유, `type_name = 'CPU'` |
| 어댑터 | `view_partmodel_v1.name` | `view_part_v1` 경유, `type_name = 'GPU'` |

자식 태스크와 **같은 `DEVICE_FILTER`** 를 쓴다. 그래야 자식이 기록할 값만
등록된다. 부모 범위(`DEPLOYEDASSET`)와 COMPUTER 자식 범위가 다른 점에 주의한다.

| 범위 | 필터 |
| --- | --- |
| 부모 | `type IN ('virtual','physical')` + `virtualsubtype_id <> 15` + `physicalsubtype <> 'PDU'` |
| COMPUTER 자식 | 위 + `network_device` 거짓 + `physicalsubtype <> 'Network Printer'` |

제조사는 부모 범위까지 훑어야 한다. `Network Printer` 는 `DEPLOYEDASSET` 이
적재하므로 그 제조사(`Samsung`)도 변환 대상에 필요하다.

### 자식의 변환과 맞추기

자식이 기록하는 값은 원천 그대로가 아니다. 변환 대상도 같은 변환을 거친 값을
넣어야 뷰 조인이 성립한다.

| 자식 컬럼 | 변환 | 변환 대상에 필요한 값 |
| --- | --- | --- |
| 모든 `MANUFACTURER` | `defaultUnknown(vendor_name)` | 벤더명 + 상수 `UNKNOWN` |
| `DPAOS.NAME` | `defaultUnknown(os_name)` | OS명 + 상수 `UNKNOWN` |
| `DPACPU.MAKEMODEL` | `defaultUnknown(model_name)` | 모델명 + 상수 `UNKNOWN` |
| `DPANETADAPTER.MAKEMODEL` | **상수 `UNKNOWN`** | 상수 `UNKNOWN` |
| `DPAMEDIAADAPTER.MAKEMODEL` | `defaultUnknown(model_name)` | GPU 모델명 |

`DPANETADAPTER.MAKEMODEL` 만 Device42 에 대응 컬럼이 없다. `view_netport_v1` 에
어댑터 모델이 없어 매핑이 상수로 정한 값이다. 어댑터 변환 대상는 GPU 모델명에
상수 `UNKNOWN` 을 더해 만든다.

### 검증

`.35` 기준으로 Device42 에서 뽑은 값이 자식이 실제로 기록한 값을 전부 덮는다.

| 변환 대상 | Device42 원천 | 자식 기록 | 결과 |
| --- | --- | --- | --- |
| 제조사 | 15종 | 10종 | 덮음 |
| 운영체제 | 19종 | 19종 | 덮음 |
| 프로세서 | 4종 | 4종 | 덮음 |
| 어댑터 | 2종 | 2종 | 덮음 |

제조사 차이는 아직 자식이 쓰지 않은 벤더다. 상위집합이므로 문제되지 않는다.

소프트웨어 경로가 특히 크다. `.68` 기준 제조사가 17종에서 55종으로 늘어난다.
`DPACSOFTWARE` 뷰가 `TLOAMSOFTWARE.MANUFACTURER` 로 조인하므로 이 경로를 빼면
소프트웨어 카탈로그를 적재해도 UI 에서 탈락한다.

## 적재 대상

`conversion` 잡이 채우는 것은 네 도메인, 테이블 여덟이다. 도메인마다 변환 대상과
변환 변형 두 테이블에 함께 넣는다.

테이블 여덟이 각각 태스크 하나다.

| 도메인 | 변환 대상 | 변환 변형 | Device42 원천 |
| --- | --- | --- | --- |
| 제조사 | `DPAMMANUFACTURER` | `DPAMMANUVARIANT` | `view_vendor_v1.name` (다중 경로) |
| 운영체제 | `DPAMOS` | `DPAMOSVARIANT` | `view_deviceos_v1.os_name` |
| 프로세서 | `DPAMPROCESSOR` | `DPAMPROCVARIANT` | `view_partmodel_v1.name` (CPU) |
| 어댑터 | `DPAMADAPTER` | `DPAMADPTVARIANT` | `view_partmodel_v1.name` (GPU) + 상수 `UNKNOWN` |

## 제외 대상

| 테이블 | 제외 사유 |
| --- | --- |
| `DPAMSOFTWARE` `DPAMSWVARIANT` | 변환 계열이지만 UI 뷰가 조인하지 않는다. 적재해도 노출에 영향이 없다 |
| `TLOAMSOFTWARE` | 변환이 아니라 소프트웨어 제품 테이블이다. 발번한 ID 를 자식에 되돌려 써야 해서 `software` 잡 소속 |
| `DPAMSWSUITE` `DPAMSWSUITECOMP` | 변환이 아니다. Maximo 설명이 "Software Suite Setup" 이다 |
| `DPAMSWUSAGE` `DPAMSWUSAGERANGE` | 변환이 아니다. Maximo 설명이 "Software Usage Setup" 이다 |
| `DPAMADAPTMOVE` `DPAMMANUMOVE` `DPAMOSMOVE` `DPAMPROCMOVE` `DPAMSWMOVE` | `PERSISTENT = 0`. 물리 테이블이 없는 UI 액션 객체다 |
| `DPAMEDIAADAPTER` | **이름만 `DPAM` 으로 시작한다.** 변환이 아니라 DPA 자식 테이블이며 서비스명이 `DEPLOYEDASSET` 이다. `../asset/dpamediaadapter.md` 참조 |

`DPAM` 접두사가 곧 변환은 아니다. `DPAMEDIAADAPTER` 가 그 예이므로 테이블
이름으로 대상을 고르지 않는다. 판별은 `MAXOBJECT.DESCRIPTION` 이 `Conversion
Targets` 또는 `Conversion Variants` 인지로 한다.

```sql
SELECT OBJECTNAME, DESCRIPTION FROM MAXIMO.MAXOBJECT
WHERE DESCRIPTION LIKE 'Deployed Assets%Conversion%'
ORDER BY OBJECTNAME
```

`Deployed Assets` 접두사까지 넣는다. `%Conversion%` 만으로는 단위 환산표
(`CONVERSION`, "Conversion factors for measure units")가 함께 잡힌다.

결과는 10개다. 적재 대상 8개에 소프트웨어 2개(`DPAMSOFTWARE`,
`DPAMSWVARIANT`)가 더 나오며, 소프트웨어는 위 표대로 제외한다.

## 실행 순서

`conversion`을 별도 잡으로 두고 자식보다 먼저 실행한다. 특히
`TLOAMSOFTWARE.MANUFACTURER`는 `DPAMMANUVARIANT`와 INNER 조인되므로 제조사
변환 데이터를 소프트웨어보다 먼저 등록해야 한 번의 실행으로 UI 노출까지 끝난다.

```bash
java -jar ... conversion asset ci software
```

`JobRunner` 가 `args` 순서대로 실행한다. 별도 잡으로 두면 UI 미표시가 발견됐을
때 자식 전량 재적재 없이 `conversion` 만 다시 돌릴 수 있다.

`TLOAMSOFTWARE` 는 예외다. 카탈로그 행을 만들고 발번된 ID 를
`DPASOFTWARE.TLOAMSOFTWAREID` 에 되돌려 써야 하므로 `software` 잡 안에서
`DpaSoftwareImport` 앞에 둔다. 상세는 `../software/tloamsoftware.md` 참조.

## MERGE 규칙

변환 대상과 변환 변형 모두 이름 컬럼에 유일 인덱스가 있다. 이름을 MERGE 키로 쓰고
`NOT MATCHED` 분기에서만 시퀀스로 ID 를 발번한다. 갱신할 값이 없으므로
`MATCHED` 분기를 두지 않는다.

```sql
MERGE INTO MAXIMO.DPAMMANUFACTURER AS target
USING (VALUES (?)) AS source (MANUFACTURERNAME)
ON target.MANUFACTURERNAME = source.MANUFACTURERNAME
WHEN NOT MATCHED THEN
    INSERT (MANUFACTURERID, MANUFACTURERNAME, VALIDATED)
    VALUES (NEXT VALUE FOR MAXIMO.DPAMMANUFACTURERSEQ, source.MANUFACTURERNAME, 0)
```

변환 대상과 변환 변형는 쌍으로 넣는다. 뷰는 변환 변형만 조인하지만 변환 변형의 이름 컬럼이
변환 대상를 가리키는 구조다.

## 현재 미표시 현황

| 자식 테이블 | 실제 행 | 뷰 행 | 미표시 |
| --- | --- | --- | --- |
| DPANETADAPTER | 205 | 61 | 144 |
| DPACPU | 141 | 57 | 84 |
| DPAOS | 122 | 63 | 59 |
| DPASOFTWARE | 13031 | 12489 | 542 |
| DPANETDEVICE | 38 | 34 | 4 |
| DPACOMPUTER | 134 | 131 | 3 |
| DPAMEDIAADAPTER | 40 | 39 | 1 |
| DPANETPRINTER | 6 | 5 | 1 |
| DPADISK | 150 | 150 | 0 |
| DPADISPLAY | 52 | 52 | 0 |
| DPASWSUITE | 14 | 14 | 0 |

미표시는 대부분 Device42 적재분이다. 기존 수집분은 수집 도구가 변환 대상를 함께
채웠기 때문에 손실이 거의 없다. 관측 시점 기준 미등록 값 종류는 제조사 5종,
OS 19종, 프로세서 4종, 어댑터 2종이다.

## 문서

테이블 한 장에 문서 한 장, 태스크 하나다. 도메인마다 대상과 변형 둘이므로
문서도 둘이다.

| 문서 | Target | 태스크 | Job의 명시적 순서 |
| --- | --- | --- | --- |
| `dpammanufacturer.md` | `DPAMMANUFACTURER` | `DpamManufacturerImport` | 1 |
| `dpammanuvariant.md` | `DPAMMANUVARIANT` | `DpamManuVariantImport` | 2 |
| `dpamos.md` | `DPAMOS` | `DpamOsImport` | 3 |
| `dpamosvariant.md` | `DPAMOSVARIANT` | `DpamOsVariantImport` | 4 |
| `dpamprocessor.md` | `DPAMPROCESSOR` | `DpamProcessorImport` | 5 |
| `dpamprocvariant.md` | `DPAMPROCVARIANT` | `DpamProcVariantImport` | 6 |
| `dpamadapter.md` | `DPAMADAPTER` | `DpamAdapterImport` | 7 |
| `dpamadptvariant.md` | `DPAMADPTVARIANT` | `DpamAdptVariantImport` | 8 |

대상을 먼저, 변형을 나중에 적재한다. 변형의 이름 컬럼이 대상을 가리키기
때문이다. FK 제약은 없으나 기존 데이터가 전 도메인 1:1로 이 관계를 지키고 있다.

| 도메인 | 대상 | 변형 | 변형→대상 연결 |
| --- | --- | --- | --- |
| 제조사 | 315 | 315 | 315 |
| 운영체제 | 23 | 23 | 23 |
| 프로세서 | 13 | 13 | 13 |
| 어댑터 | 59 | 59 | 59 |

고아 변형과 변형 없는 대상이 모두 0건이다.

### 적재하지 않는 것

| 문서 | 대상 | 사유 |
| --- | --- | --- |
| `dpamsoftware.md` | `DPAMSOFTWARE` `DPAMSWVARIANT` | UI 뷰가 조인하지 않는다 |
| `../software/tloamsoftware.md` | `TLOAMSOFTWARE` | `software` 잡 소속 |

## 소프트웨어는 구조가 다르다

UI 노출에 필요한 것은 셋뿐이며 `DPAMSOFTWARE` 와 `DPAMSWVARIANT` 는 들어가지
않는다.

```
DPASOFTWARE.TLOAMSOFTWAREID  →  TLOAMSOFTWARE.TLOAMSOFTWAREID
TLOAMSOFTWARE.MANUFACTURER   →  DPAMMANUVARIANT.MANUFACTURERVAR
```

다른 변환 대상과 달리 이름만 넣는 것으로 끝나지 않는다. 카탈로그 행을 만들고
발번된 ID를 자식에 되돌려 써야 하므로 `software` 잡에서
`DpaSoftwareImport`보다 먼저 실행한다. `../software/tloamsoftware.md` 참조.

## 공통 구조

| 도메인 | 변환 대상 | 행 | 변환 변형 | 행 |
| --- | --- | --- | --- | --- |
| 제조사 | `DPAMMANUFACTURER` | 315 | `DPAMMANUVARIANT` | 315 |
| 운영체제 | `DPAMOS` | 23 | `DPAMOSVARIANT` | 23 |
| 프로세서 | `DPAMPROCESSOR` | 13 | `DPAMPROCVARIANT` | 13 |
| 어댑터 | `DPAMADAPTER` | 59 | `DPAMADPTVARIANT` | 59 |
| 소프트웨어 | `DPAMSOFTWARE` | 1982 | `DPAMSWVARIANT` | 1982 |

관측 시점에 다섯 도메인 모두 변환 변형가 변환 대상와 행 수가 같고 `정규명 = 원시명`
이다. 별칭이 하나도 등록되어 있지 않다. 변환 대상의 `VALIDATED` 도 전건 `0` 이다.

`*MOVE` 테이블은 `PERSISTENT = 0` 이라 물리 테이블이 없다.

## 미결

정규화 여부가 미정이다. 자식이 기록한 값을 그대로 정규명으로 쓰면 변환 변형가
항상 `정규명 = 원시명` 이 되어 정규화 기능이 동작하지 않는다. 같은 OS 가 표기만
달라 19종으로 흩어지고, CPU 는 4종 중 2종이 같은 모델이다. 각 문서의 미결 절
참조.
