# IP

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-15 · D42 .68 / .35 · Maximo BLUDB
> 구현: [IpCiImport](../../../../../src/main/java/com/itmsg/device42/integration/d42maximo/ci/ip/IpCiImport.java) · [IpCiQuery](../../../../../src/main/java/com/itmsg/device42/integration/d42maximo/ci/ip/IpCiQuery.java) · [IpCiMapper](../../../../../src/main/java/com/itmsg/device42/integration/d42maximo/ci/ip/IpCiMapper.java) · [ActCiWriter](../../../../../src/main/java/com/itmsg/device42/maximo/ci/ActCiWriter.java) · 상태: 본체·스펙 적재 구현 및 자동 테스트 완료. 현재 Device→IP `USES` 관계는 별도 관계 단계에서 적재한다.
> 분류 선택 이유·관계 선택지는 [IP 수집 설계](../../../design/ci/ip.md)에 있다.

공통 컬럼 정의는 [ACTCI](../actci.md), [ACTCISPEC](../actcispec.md)가 소유한다.

2026-09-18 구조 대조: 아래의 「직접 규칙 없음·경로 결정 필요」는 2026-09-15 관측이다.
후속 확정된 `USES` 경로와 전체 장비–IP 쌍 조회·검증 상태의 정본은 [ACTCIRELATION](../actcirelation.md)이다.
IP 본체의 대표 장비 한 건을 관계 입력으로 사용하지 않는다.

## 1. 대상과 식별자

| 항목 | 값 |
| --- | --- |
| 대상 | **장비에 연결된** `view_ipaddress_v2` 행. 105 / 111건. 부모 유형으로 좁히지 않는다 |
| 분류 | `NET.IPADDRESS` 한 개 |
| ACTCINUM | `D42:IPADDRESS:<ipaddress_pk>` |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 관계 | **Computer와 직접 규칙이 없다.** 5절 |

전체 IP 297 / 543건 중 어떤 장비에도 붙지 않은 행이 192 / 432건이며 이것만 제외한다.
IP는 장비 종속 개체가 아니라 독립 CI이므로 Computer·네트워크·컨테이너를 구분하지 않는다.
장비 유형을 추가해도 이 조회를 고치지 않는다. 근거는 [IP 수집 설계](../../../design/ci/ip.md) 7절.

## 2. 원천과 조회 조건

`device_fks`가 배열이고 최대 3 / 7개 장비에 걸린다. 같은 `ipaddress_pk`가 여러 행이 되므로
`DISTINCT ON`이 **실제로 필요하다**. 근거는 ISSUE-9.

`ip_address`는 `inet`이다. `CAST(... AS VARCHAR)`는 실제 프리픽스가 아닌 `/32`를 붙이므로
`HOST()`로 주소만 뽑는다. 프리픽스 길이는 서브넷에서 가져온다.

```sql
SELECT DISTINCT ON (i.ipaddress_pk)
    i.ipaddress_pk, d.device_pk AS device_fk,
    HOST(i.ip_address) AS ip_address,
    NULLIF(TRIM(d.name), '') AS device_name,
    NULLIF(TRIM(i.label), '') AS label,
    NULLIF(TRIM(i.notes), '') AS notes,
    i.last_discovered
FROM view_ipaddress_v2 i
JOIN view_device_v2 d ON d.device_pk = ANY(i.device_fks)
ORDER BY i.ipaddress_pk, d.device_pk
LIMIT %d OFFSET %d
```

2026-09-15 두 서버에서 실행해 통과를 확인했다.
`netport_fk`와 `mask_bits`는 관계·서브넷 조사용이다. 현재 IpCiImport의 원천 SQL·DTO에는 포함하지 않는다.
관계 구현 시 netport_fk 및 전체 device_fks 보존이 필요하다.
2026-09-15의 실제 경로·다중 연결 대조는 [관계 원천](../../../knowledge/device42/computer-ci-relations.md)을 참조한다.

## 3. 본체 매핑

| Target 컬럼 | 한글명 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- |
| ACTCINUM | 실제 CI 번호 | 변환 | `i.ipaddress_pk` | `'D42:IPADDRESS:' || pk` |
| ACTCINAME | 실제 CI 이름 | 변환 | `i.ip_address` | `HOST()`로 주소만. 전건 |
| CLASSSTRUCTUREID | 분류 | 변환 | 상수 분류명 | `NET.IPADDRESS` 조회값 |
| DESCRIPTION | 설명 | 직접 | `i.notes` | 값 분포 미확인 |
| LASTSCANDT | 최종 발견 시각 | 직접 | `i.last_discovered` | **전건 보유.** 네 유형 중 유일하게 자체 발견 시각이 있다 |
| HASLD | 상세 설명 있음 | 상수 | – | 0 |
| CHANGEBY | 변경자 | 상수 | – | `Device42` |
| CHANGEDATE | 변경 날짜 | 변환 | 매핑 시각 | JVM 기본 시간대 |
| LANGCODE | 언어 코드 | 상수 | – | `KO` |

## 4. 속성 매핑

적재 분류 `NET.IPADDRESS` · **대조 기준 `CI.IPADDRESS`(CCI00011, 6개)**.
선택 근거는 [IP 수집 설계](../../../design/ci/ip.md) 3절.
해당 분류의 CLASSSPEC이 있는 속성은 값이 없어도 행을 만들어 값 컬럼을 NULL로 동기화한다.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| IPADDRESS_DOTNOTATION | 점 표기 주소 | ALNVALUE | 변환 | `i.ip_address` | `HOST()`. 전건 |
| IPADDRESS_STRINGNOTATION | 문자열 표기 주소 | ALNVALUE | 변환 | `i.ip_address` | `HOST()`. CI 기준 속성. DOTNOTATION과 같은 값 |
| IPADDRESS_MANAGEDSYSTEMNAME | 관리 시스템 이름 | ALNVALUE | 직접 | 연결 장비의 `d.name` | 여러 장비면 `device_pk` 최소인 장비 |
| MODELOBJECT_LABEL | 레이블 | ALNVALUE | 직접 | `i.label` | 55 / 59건. **CI 기준 밖 의도적 추가** |

## 5. 미대응·미결

| 항목 | 상태 |
| --- | --- |
| **관계 경로** | `SYS.*COMPUTERSYSTEM`↔`NET.IPADDRESS` 규칙이 양방향 0건이다. CDM 경로는 `Computer → NET.IPINTERFACE → NET.IPADDRESS`다. Interface CI 도입은 범위 확대라 **사용자 결정 필요**. ISSUE-8 |
| 서브넷 마스크 | `b.mask_bits` 전건 보유하나 `NET.IPADDRESS`에 자리 없음. `NET.IPNETWORK` 별도 CI 필요. ISSUE-8 |
| `b.gateway` | 컬럼은 있으나 두 서버 모두 값 0건 |
| IPADDRESS_ADDRESSTYPE | DOMAINID 없음, 시스템 기존 값 0건이라 코드 규약을 알 수 없다. ISSUE-11 |
| MODELOBJECT_LABEL 승격 전달 | CI 계열 분류에 `MODELOBJECT_` 속성이 0개다. 승격에서 누락될 수 있다. 미검증. ISSUE-11 |
| MODELOBJECT_CDMSOURCE·SOURCETOKEN | 미채택 확정. CI 계열에 없고 `ACTCINUM`·`CHANGEBY`와 중복 |
| 분류 v4/v6 분리 | `NET.IPV4ADDRESS`·`NET.IPV6ADDRESS` 속성 22개 동일, 승격 범위 등록됨. 원천 v6 0 / 2건. ISSUE-8 |
