# DPASOFTWARE

배치된 자산 컴퓨터 애플리케이션

> Target: MAXIMO.DPASOFTWARE · ASSETCLASS: COMPUTER · 구현: [DpaSoftwareImport](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/software/installed/DpaSoftwareImport.java) · [InstalledSoftwareQuery](../../../../src/main/java/com/itmsg/device42/source/device42/software/installed/InstalledSoftwareQuery.java) · [DpaSoftwareMapper](../../../../src/main/java/com/itmsg/device42/pipeline/d42maximo/software/installed/DpaSoftwareMapper.java) · [DpaSoftwareWriter](../../../../src/main/java/com/itmsg/device42/target/maximo/software/DpaSoftwareWriter.java)
> 관측 2026-08-27 · Device42 192.168.1.35 · 192.168.2.68 / Maximo BLUDB

> SQL의 LIMIT/OFFSET은 예시 페이지 값이다. 본체·관계 조회는 Source, 타겟 식별자·값 생성은 Pipeline Mapper가 소유한다.

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPASOFTWARE (PK 는 `SOFTWAREID`. 관측 53노드/13031행)
- 선행: DEPLOYEDASSET, TLOAMSOFTWARE
- MERGE ID: `SOFTWAREID = view_softwareinuse_v1.softwareinuse_pk`

`software` 잡에서 `TloamSoftwareImport`가 카탈로그를 먼저 만들고
`DpaSoftwareImport`가 설치 행을 적재한다. 상세는 `tloamsoftware.md` 참조.

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_softwareinuse_v1` | MAXIMO.DPASOFTWARE | – | 1:1 (설치 1건 = 행 1건) |
| `view_software_v1` | (보강) | `view_softwareinuse_v1.software_fk = software_pk` | N:1 |
| `view_vendor_v1` | (보강) | `view_software_v1.vendor_fk = vendor_pk` | N:1 |
| `view_device_v2` | (대상 판정) | `view_softwareinuse_v1.device_fk = device_pk` | N:1 |

COMPUTER 대상 원천은 `.68` 4900행, `.35` 485행이다. 다른 자식 테이블(수십~수백
행)과 자릿수가 다르다. 페이징과 행 단위 예외 처리가 실제로 부하를 받는 첫
태스크다.

`SOFTWARENAME` 은 `view_software_v1.name` 에서 얻는다. `view_softwareinuse_v1`
에도 `software_name` 이 있으나 두 값은 양쪽 서버 전건 같다(`.35` 485/485,
`.68` 4900/4900).

UI는 `TLOAMSOFTWARE`를 경유해 이 테이블을 읽는다. `DPAMSOFTWARE`와
`DPAMSWVARIANT`는 UI 뷰가 조인하지 않으므로 현재 적재 대상이 아니다.

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer','PDU'))` | 다른 ASSETCLASS와 PDU의 자식을 만들지 않는다 |

## 4. 컬럼 매핑

충전율은 `1.35 / 2.68` 순으로 적는다. 기존 수집분 수치는 13031행 기준이다.

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 원천없음 | – | `view_software_v1.description` 이 양쪽 서버 전건 비어 있다. 기존 수집분도 825/13031 |
| FIRSTENCOUNTERED1 | 첫 번째 발견 날짜 | DATETIME(10) | Y | 직접 | `view_softwareinuse_v1.first_detected` | 관측 485/485 · 4900/4900 |
| INSTALLDATE | 설치 날짜 | DATETIME(10) | Y | 직접 | `view_softwareinuse_v1.install_date` | 관측 0/485 · 3529/4900 |
| INSTALLPATH | 설치 경로 | ALN(4000) | Y | 직접 | `view_softwareinuse_v1.install_path` | 관측 3/485 · 72/4900, 최대 111자 |
| LANGUAGE | 언어 | ALN(32) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분의 `English: United States` 는 다른 도구의 관례다 |
| LASTENCOUNTERED1 | 마지막 발견 날짜 | DATETIME(10) | Y | 직접 | `view_softwareinuse_v1.last_updated` | 관측 485/485 · 4900/4900 |
| LASTUSAGEDATE | 최종 사용일 | DATETIME(10) | Y | 원천없음 | – | Device42 는 사용 시각을 수집하지 않는다 |
| LICENSEDORG | 라이센스가 부여된 조직 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/13031 |
| LICENSEDUSER | 라이센스가 부여된 사용자 | ALN(64) | Y | 원천없음 | – | `view_softwareinuse_v1.enduser_fk` 가 양쪽 서버 0건이다. 기존 수집분도 0/13031 |
| MANUFACTURER | 제조업체 | ALN(128) | N | 직접 | `view_vendor_v1.name` | `view_software_v1.vendor_fk` 조인. 관측 124/485 · 1226/4900, 최대 31자. 없으면 `UNKNOWN`. DEFAULTVALUE=UNKNOWN |
| METRICID | 메트릭 ID | BIGINT(19) | Y | 원천없음 | – | 라이선스 메트릭. 대응 원천이 없고 기존 수집분도 0/13031 |
| NODEID | 노드 ID | BIGINT(19) | N | 직접 | `view_softwareinuse_v1.device_fk` | 부모 DEPLOYEDASSET와 동일한 ID를 직접 사용한다 |
| PRODUCTID | 제품 ID | ALN(128) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/13031 |
| SERIALNUMBER | 일련 번호 | ALN(64) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/13031 |
| SOFTWAREID | 소프트웨어 | BIGINT(19) | N | 직접 | `view_softwareinuse_v1.softwareinuse_pk` | Maximo ID로 그대로 사용하며 MERGE 키로 삼는다 |
| SOFTWARENAME | 애플리케이션 | ALN(256) | N | 직접 | `view_software_v1.name` | 관측 485/485 · 4900/4900, 최대 110자. DEFAULTVALUE=UNKNOWN |
| SUITEID | 스위트 ID | BIGINT(19) | Y | 상수 | – | `0`. 기존 수집분도 채워진 12206행 전건 `0` 이다. DPASWSUITE 원천이 없어 묶을 스위트가 없다 |
| SUITENAME | 스위트 | ALN(254) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| TLOAMPRODUCTID | 소프트웨어 | BIGINT(19) | Y | 변환 | 이름·버전·제조사 | 계산한 `UNIQUEID`로 TLOAMSOFTWARE를 조회한 ID. `TLOAMSOFTWAREID`와 같은 값 |
| TLOAMSOFTWAREID | 소프트웨어 | BIGINT(19) | Y | 변환 | 이름·버전·제조사 | 선행 태스크가 생성한 TLOAMSOFTWARE의 ID. `tloamsoftware.md`의 `UNIQUEID` 규칙을 사용한다 |
| TLOAMUNINSTDATE | 설치 제거 날짜 | DATE(4) | Y | 원천없음 | – | 대응 원천이 없다. 기존 수집분도 0/13031 |
| TLOAMUSEEXCP | 사용 예외 | UPPER(30) | Y | 원천없음 | – | 라이선스 조정용 수동 입력값. 기존 수집분도 0/13031 |
| TLOAMUSEEXCPJUST | 조정 비고 | ALN(50) | Y | 원천없음 | – | 라이선스 조정용 수동 입력값 |
| TYPE | 애플리케이션 유형 | ALN(64) | Y | 원천없음 | – | 기존 수집분은 `Database`·`Utility` 같은 분류다. Device42 `software_type` 은 `unmanaged`/`managed`/`prohibited` 로 라이선스 관리 상태이며 다른 개념이다. `view_software_v1.category_name` 은 2234/2273 이 비어 있다 |
| USAGECOUNT | 사용 회수 | INTEGER(12) | Y | 원천없음 | – | Device42 `license_use_count` 는 라이선스 소모량이지 사용 횟수가 아니다. 기존 수집분은 채워진 12206행 전건 `5` 로 실사용 값이 아니다 |
| VERSION | 버전 | ALN(128) | Y | 직접 | `view_softwareinuse_v1.version` | 관측 485/485 · 4894/4900, 최대 56자 |
| VUSAGEDISPLAYTEXT | 사용 | ALN(64) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

`view_softwareinuse_v1.arch`(관측 476/485 · 4560/4900, `x86_64`)와
`alias_name`(21/485 · 674/4900)은 대응 타겟 컬럼이 없어 적재하지 않는다.
`alias_name` 은 `DPAMSWVARIANT` 와 같은 개념이라 ISSUE-6 을 다룰 때 쓸 수 있다.

## 5. 조회 쿼리

```sql
SELECT
    u.softwareinuse_pk,
    u.device_fk,
    s.name AS software_name,
    u.version,
    u.install_path,
    u.install_date,
    u.first_detected,
    u.last_updated,
    v.name AS vendor_name
FROM view_softwareinuse_v1 u
JOIN view_device_v2 d ON d.device_pk = u.device_fk
LEFT JOIN view_software_v1 s ON s.software_pk = u.software_fk
LEFT JOIN view_vendor_v1 v ON v.vendor_pk = s.vendor_fk
WHERE
d.type IN ('virtual', 'physical')
AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
AND (d.network_device = false OR d.network_device IS NULL)
AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
ORDER BY u.device_fk, s.name, u.version, u.softwareinuse_pk
LIMIT 1000 OFFSET 0
```

## 6. 미결

- 원천에서 사라진 행의 삭제·비활성화 정책은 ISSUE-7에서 논의한다.
