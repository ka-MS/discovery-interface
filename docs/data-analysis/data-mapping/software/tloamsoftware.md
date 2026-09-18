# TLOAMSOFTWARE

소프트웨어 카탈로그

> Target: MAXIMO.TLOAMSOFTWARE · 구현: [TloamSoftwareImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/software/catalog/TloamSoftwareImport.java) · [TloamSoftwareQuery](../../../../src/main/java/com/itmsg/device42/source/device42/software/catalog/TloamSoftwareQuery.java) · [TloamSoftwareMapper](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/software/catalog/TloamSoftwareMapper.java) · [TloamSoftwareWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/software/TloamSoftwareWriter.java)
> 관측 2026-08-28 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB
> 재조회 `../../exploration-queries/device42/dpa-software-mapping.sql` · `../../exploration-queries/maximo/dpa-view-conversion-requirements.sql`

## 1. 관계

- 부모: 없음. 설치 소프트웨어가 참조하는 전역 카탈로그다
- 카디널리티: TLOAMSOFTWARE 1 : N DPASOFTWARE
- 선행: `DPAMMANUFACTURER` / `DPAMMANUVARIANT`
- MERGE 키: `UNIQUEID`
- 실행 위치: `software` 잡의 `DpaSoftwareImport` 앞

UI 뷰의 필수 연결은 다음 두 개다.

```text
DPASOFTWARE.TLOAMSOFTWAREID → TLOAMSOFTWARE.TLOAMSOFTWAREID
TLOAMSOFTWARE.MANUFACTURER  → DPAMMANUVARIANT.MANUFACTURERVAR
```

`DPASOFTWARE.TLOAMSOFTWAREID`가 NULL이면 `DPACSOFTWARE`에서 제외된다.
`DPAMSOFTWARE`와 `DPAMSWVARIANT`는 이 뷰가 조인하지 않는다.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_softwareinuse_v1` | MAXIMO.TLOAMSOFTWARE | – | N:1 (같은 이름·버전·제조사를 한 카탈로그 행으로 묶음) |
| `view_software_v1` | (보강) | `view_softwareinuse_v1.software_fk = software_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_software_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_softwareinuse_v1.device_fk = device_pk` | N:1 |

COMPUTER 대상 설치 행과 생성되는 카탈로그 키는 `.35` 485/485건, `.68`
4,900/3,319건이다. 정규화 후 서로 다른 원천 조합이 같은 키가 된 사례와 구분자
`|`가 포함된 원천값은 양쪽 모두 0건이다.

기존 Maximo는 2,175행이며 이름 1,982종, 이름·버전 2,007종이다.
`TLOAMSOFTWAREID`는 `TLOAMSOFTWARESEQ`가 발번하며 시퀀스는 START 2,175,001,
INCREMENT 1,000이다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 범위와 맞춘다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer','PDU'))` | 다른 ASSETCLASS와 PDU를 제외한다 |

## 4. UNIQUEID

기존 2,175행에 아래 규칙을 적용한 결과 전건 일치했다.

```text
nameToken = UPPER(TRIM(COALESCE(NULLIF(SWNAME, ''), 'UNKNOWN')))

versionToken =
  VERSION과 RELEASE가 모두 없으면 'UNKNOWN'
  RELEASE만 없으면 VERSION
  VERSION만 없으면 RELEASE
  둘 다 있으면 VERSION || '.' || RELEASE
  이후 공백 제거 및 대문자 변환

manufacturerToken = UPPER(TRIM(MANUFACTURER))

UNIQUEID = nameToken || '|' || versionToken || '|' || manufacturerToken
```

Device42는 `RELEASE` 원천이 없으므로 `VERSION`만 사용한다. 이름·제조사가 없으면
`UNKNOWN`을 사용한다. 이 값을 TLOAMSOFTWARE MERGE와 후속 ID 조회에 동일하게
사용한다.

## 5. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| TLOAMSOFTWAREID | 고유 ID | BIGINT(19) | N | 채번 | – | NOT MATCHED일 때 `NEXT VALUE FOR MAXIMO.TLOAMSOFTWARESEQ` |
| UNIQUEID | 고유 ID | ALN(640) | N | 변환 | 이름·버전·제조사 | 4절 규칙. 유일 인덱스이며 MERGE 키다 |
| SWNAME | 소프트웨어 이름 | ALN(256) | Y | 직접 | `view_software_v1.name` | 없으면 `UNKNOWN` |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | 없으면 `UNKNOWN`. 같은 값이 `DPAMMANUVARIANT`에 있어야 한다 |
| VERSION | 버전 | ALN(128) | Y | 직접 | `view_softwareinuse_v1.version` | 빈 문자열은 NULL |
| RELEASE | 릴리스 | ALN(128) | Y | 원천없음 | – | NULL |
| ROLE | 역할 | UPPER(30) | N | 상수 | – | `SOFTWAREPRODUCT` |
| CATALOGSOURCEID | 카탈로그 소스 | BIGINT(19) | Y | 원천없음 | – | NULL |
| CCID | CCID | ALN(32) | Y | 원천없음 | – | NULL |
| DEFAULTLICENSENUM | 기본 라이센스 | UPPER(12) | Y | 원천없음 | – | NULL |
| DEFAULTLICENSEORGID | 기본 라이센스 조직 | UPPER(8) | Y | 원천없음 | – | NULL |
| DELETEDATE | 삭제 날짜 | DATETIME(10) | Y | 원천없음 | – | NULL |
| DESCRIPTION | 설명 | ALN(100) | Y | 원천없음 | – | Device42 설명은 양쪽 서버 전건 비어 있다 |
| EID | EID | ALN(64) | Y | 원천없음 | – | NULL |
| FUNC | 함수 | ALN(256) | Y | 원천없음 | – | NULL |
| ISIPLA | IPLA임 | YORN(1) | N | 상수 | – | `0` |
| ISPVU | 사용 PVU | YORN(1) | N | 상수 | – | `0` |
| ISSUBCAP | 보조 용량 적합 | YORN(1) | N | 상수 | – | `0` |
| ISDELETED | 삭제됨 | YORN(1) | N | 상수 | – | `0` |
| ISREVIEWED | 검토됨 | YORN(1) | N | 상수 | – | `0` |
| MANAGEDAS | 다음으로 관리 | ALN(40) | Y | 원천없음 | – | NULL |
| PARENT | 상위 레코드 | INTEGER(12) | Y | 원천없음 | – | NULL |
| PLATFORMBASE | 플랫폼 | UPPER(20) | Y | 원천없음 | – | NULL |
| POINTS | 지점 | INTEGER(12) | Y | 원천없음 | – | NULL |
| TARGETSOFTWAREID | 대상 레코드 | INTEGER(12) | Y | 원천없음 | – | 카탈로그 병합용. NULL |
| PRODUCTID | 제품 ID | ALN(256) | Y | 원천없음 | – | NULL |
| EXTERNALID | 외부 ID | ALN(256) | Y | 원천없음 | – | NULL |
| SITENAME | 사이트 이름 | ALN(100) | Y | 원천없음 | – | NULL |
| SSEID | S&S EID | ALN(128) | Y | 원천없음 | – | NULL |
| SSPID | S&S PID | ALN(128) | Y | 원천없음 | – | NULL |
| TYPE | 유형 | UPPER(30) | Y | 원천없음 | – | NULL |
| URL | URL | ALN(1000) | Y | 원천없음 | – | NULL |
| VULEXHIBITID | 값 단위 표시 | ALN(32) | Y | 원천없음 | – | NULL |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

기존 2,175행은 `ROLE`이 전건 `SOFTWAREPRODUCT`, 위 다섯 플래그가 전건 `0`이고
`TARGETSOFTWAREID`, `PRODUCTID`, `EXTERNALID`는 전건 NULL이다.

## 6. 조회 쿼리

```sql
SELECT DISTINCT
    COALESCE(NULLIF(TRIM(s.name), ''), 'UNKNOWN') AS software_name,
    NULLIF(TRIM(u.version), '') AS version,
    COALESCE(NULLIF(TRIM(v.name), ''), 'UNKNOWN') AS manufacturer
FROM view_softwareinuse_v1 u
JOIN view_device_v2 d ON d.device_pk = u.device_fk
LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
ORDER BY software_name, version, manufacturer
```

## 7. 적재 순서

`software` 잡은 두 태스크를 순서대로 실행한다.

1. `TloamSoftwareImport`
   - 원천을 카탈로그 단위로 조회한다.
   - `UNIQUEID`로 MERGE하고 신규 행만 시퀀스로 발번한다.
2. `DpaSoftwareImport`
   - 설치 행에서 같은 `UNIQUEID`를 계산한다.
   - TLOAMSOFTWARE에서 `TLOAMSOFTWAREID`를 조회한다.
   - `TLOAMSOFTWAREID`와 `TLOAMPRODUCTID`에 같은 ID를 넣고 DPASOFTWARE를 MERGE한다.

카탈로그를 찾지 못한 설치 행은 NULL 참조로 적재하지 않고 오류 로그를 남긴다.

기존 DPASOFTWARE 13,031행 중 TLOAM ID가 있는 12,546행은 두 컬럼 값이 전건
같다. Device42 적재분 485행은 두 컬럼이 모두 NULL이라 현재 UI에 표시되지 않는다.

원천 PK나 서버 주소를 카탈로그 키로 사용하지 않는다. 운영에서는 하나의 Device42
원천을 사용하며, 카탈로그 식별은 소프트웨어 값으로 만든 `UNIQUEID`가 담당한다.

## 8. 미결

- 원천에서 사라진 카탈로그 행의 삭제·비활성화 정책은 ISSUE-7에서 논의한다.
- `TARGETSOFTWAREID`를 이용한 카탈로그 병합은 현재 범위에 포함하지 않는다.
