# CI 승격 범위

> 관측 2026-09-15 · Maximo BLUDB · 읽기 전용 조회
> 재조회 [승격 범위](../../exploration-queries/maximo/ci-promotion-scope.sql)

ACTCI를 CI로 승격할 때 어떤 ACTCI 분류가 어떤 CI 분류가 되는지는 **`MAXIMO.CITEMPLATE`** 이 정한다.
Maximo 화면의 「승격 범위」가 이 테이블이다. 분류 계열 두 개를 잇는 정본이므로
[CI 분류 모델](ci-classification.md)·[분류 조사](ci-component-classifications.md)와 함께 본다.

## 1. 구조

| 컬럼 | 타입 | Null | 뜻 |
| --- | --- | --- | --- |
| CITEMPLATEID | BIGINT | N | PK |
| TOPCICLASSID | UPPER(25) | Y | 범위의 최상위 CI 분류 |
| TOPACTCICLASSID | UPPER(25) | Y | 범위의 최상위 ACTCI 분류 |
| CICLASSID | UPPER(25) | N | 매핑된 CI 분류 |
| ACTCICLASSID | UPPER(25) | N | 매핑된 ACTCI 분류 |

고유 인덱스는 `CITEMPLATE_NDX1 (ACTCICLASSID, CICLASSID, TOPACTCICLASSID, TOPCICLASSID)`다.
**한 CI 분류에 ACTCI 분류를 여럿 매핑할 수 있다.** 인덱스가 네 컬럼 조합이라 막지 않는다.

`(TOPCICLASSID, TOPACTCICLASSID)` 한 쌍이 승격 범위 하나다. 전체 158행, 범위 14개다.

## 2. 범위 목록

| 최상위 CI 분류 | 최상위 ACTCI 분류 | 매핑 행 수 |
| --- | --- | ---: |
| CI.BUSINESSAPPLICATION | APP.APPLICATION | 28 |
| CI.BUSINESSSYSTEM | SYS.BUSINESSSYSTEM | 28 |
| CI.VMWARECOMPUTERSYSTEM | SYS.VMWARE.VMWAREUNITARYCOMPUTERSYSTEM | 20 |
| CI.ZSERIESCOMPUTERSYSTEM | SYS.ZOS.ZSERIESCOMPUTERSYSTEM | 10 |
| **CI.COMPUTERSYSTEM** | **SYS.COMPUTERSYSTEM** | **9** |
| CI.AIXCOMPUTERSYSTEM / CI.HPUXCOMPUTERSYSTEM / CI.LINUXCOMPUTERSYSTEM / CI.SUNCOMPUTERSYSTEM / CI.WINDOWSCOMPUTERSYSTEM | 각 대응 | 8 |
| CI.SYSTEMPCOMPUTERSYSTEM / CI.UNITARYCOMPUTERSYSTEM | 각 대응 | 7 |
| **CI.VIRTUALCOMPUTERSYSTEM** | **SYS.VIRTUALCOMPUTERSYSTEM** | **1** |
| CI.SOFTWAREIMAGE | APP.SOFTWAREIMAGE | 1 |

## 3. Computer 범위 — 9행

`CI.COMPUTERSYSTEM` ↔ `SYS.COMPUTERSYSTEM` 범위다. 이 ETL이 적재하는 유형 대부분이 여기 걸린다.

| ACTCI 분류 | CI 분류 | CITEMPLATEID | 비고 |
| --- | --- | ---: | --- |
| SYS.COMPUTERSYSTEM | CI.COMPUTERSYSTEM | 38 | |
| SYS.OPERATINGSYSTEM | CI.OS | 71 | |
| SYS.LOCALFILESYSTEM | CI.FILESYSTEM | 77 | |
| **SYS.FILESYSTEM** | **CI.FILESYSTEM** | **164** | **2026-09-15 추가** |
| NET.IPADDRESS | CI.IPADDRESS | 104 | |
| NET.IPV4ADDRESS | CI.IPV4ADDRESS | 120 | |
| NET.IPV6ADDRESS | CI.IPV6ADDRESS | 132 | |
| NET.IPINTERFACE | CI.IPINTERFACE | 55 | |
| NET.FQDN | CI.FQDN | 144 | |

기본 구성은 파일시스템을 `SYS.LOCALFILESYSTEM`으로만 매핑했다.
이 ETL이 `SYS.FILESYSTEM`으로 적재하므로 2026-09-15 사용자가 매핑 행을 추가했다.
ROWSTAMP가 8520731로 기본값 행들(1556xxx대)과 구분된다.

2026-09-15 이 범위에서 OS → 물리 Computer 한 쌍의 승격을 검증했다.
OS CI 생성과 RELATION.INSTALLEDON 관계·부모 보존을 UI 및 DB에서 확인했다.
[샘플 검증 기록](computer-ci-relations.md#oscomputer-승격-샘플-검증). Filesystem·IP 승격 검증을 의미하지 않는다.

**`CI.FILESYSTEM`에 ACTCI 분류 둘이 걸린 것은 158행 전체에서 유일한 경우다.**
IBM 기본 구성에 없는 형태이므로 승격 동작은 검증 대상이다. 화면의 「유효성 검증」으로 확인한다.

## 4. 적재 분류의 승격 가능 여부

| 적재 분류 | CITEMPLATE 등록 | 승격 |
| --- | ---: | --- |
| SYS.COMPUTERSYSTEM | 4개 범위 | 가능 |
| SYS.OPERATINGSYSTEM | 4개 범위 | 가능 |
| NET.IPADDRESS | 12개 범위 | 가능 |
| SYS.FILESYSTEM | 1개 범위 | 가능 (추가한 행) |
| SYS.VIRTUALCOMPUTERSYSTEM | 1개 범위 | 본체만 가능. 아래 참조 |
| **DEV.DISKDRIVE** | **0** | **불가** |

### Disk는 승격할 수 없다

`DEV.DISKDRIVE`가 `CITEMPLATE`에 한 행도 없다. CI 계열에 디스크 분류가 없다는 관측과 같은 결론이며
승격 설정 쪽에서도 확인된다. Disk CI는 ACTCI에만 머문다.

### 가상 Computer 범위에는 자식이 없다

`CI.VIRTUALCOMPUTERSYSTEM` 범위는 자기 자신 1행뿐이다(CITEMPLATEID 160, ROWSTAMP 8516923).
`CI.COMPUTERSYSTEM` 범위가 OS·Filesystem·IP를 포함한 9행인 것과 다르다.

2026-09-15 적재 기준 Computer 70대 중 **65대가 `SYS.VIRTUALCOMPUTERSYSTEM`**, 5대만 물리다.
현재 설정으로 가상 서버를 승격하면 본체만 올라가고 OS·Filesystem·IP는 따라가지 않는다.
비교 대상으로 `CI.VMWARECOMPUTERSYSTEM` 범위는 20행을 갖는다.

기준정보 변경은 이 ETL의 범위가 아니다. 관측 사실로만 남긴다. ISSUE-11.
