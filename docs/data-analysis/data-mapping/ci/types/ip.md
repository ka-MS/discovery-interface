# IP

> Target: MAXIMO.ACTCI · MAXIMO.ACTCISPEC
> 원천·메타데이터 확인: 2026-09-15 · D42 .68 / .35 · Maximo BLUDB
> 구현: IpCiIntegrate · 상태: 본체·스펙 적재 구현 및 자동 테스트 완료. **관계는 규칙이 없어 적재하지 않는다.**
> 분류 선택 이유·관계 선택지는 [IP 수집 설계](../../../design/ci/ip.md)에 있다.

공통 컬럼 정의는 [ACTCI](../actci.md), [ACTCISPEC](../actcispec.md)가 소유한다.

## 1. 대상과 식별자

| 항목 | 값 |
| --- | --- |
| 대상 | Computer에 연결된 `view_ipaddress_v2` 행. 50 / 97건 |
| 분류 | `NET.IPADDRESS` 한 개 |
| ACTCINUM | `D42:IPADDRESS:<ipaddress_pk>` |
| 스펙 참조 | ACTCINUM·CLASSSTRUCTUREID는 본체와 동일, REFOBJECTID=ACTCIID |
| 관계 | **Computer와 직접 규칙이 없다.** 5절 |

전체 IP 297 / 543건 중 어떤 장비에도 붙지 않은 행이 192 / 432건이다.
Computer 연결분만 대상으로 한다.

## 2. 원천과 조회 조건

`device_fks`가 배열이고 최대 3 / 7개 장비에 걸린다. 같은 `ipaddress_pk`가 여러 행이 되므로
`DISTINCT ON`이 **실제로 필요하다**. 근거는 ISSUE-9.

`ip_address`는 `inet`이다. `CAST(... AS VARCHAR)`는 실제 프리픽스가 아닌 `/32`를 붙이므로
`HOST()`로 주소만 뽑는다. 프리픽스 길이는 서브넷에서 가져온다.

```sql
WITH computer AS (
    SELECT d.device_pk, d.last_discovered
    FROM view_device_v2 d
    WHERE d.type IN ('physical', 'virtual')
      AND (d.network_device = false OR d.network_device IS NULL)
      AND (
          (d.type = 'physical' AND d.physicalsubtype IN
              ('Generic', 'Rackable', 'Blade', 'WorkStation', 'ThinClient', 'Laptop'))
          OR
          (d.type = 'virtual' AND d.virtualsubtype IN
              ('Internal VM', 'Amazon EC2 Instance', 'VMWare', 'Hyper-V'))
      )
)
SELECT DISTINCT ON (i.ipaddress_pk)
    i.ipaddress_pk, c.device_pk AS device_fk,
    'D42:IPADDRESS:' || CAST(i.ipaddress_pk AS varchar) AS source_id,
    HOST(i.ip_address) AS ip_address,
    NULLIF(TRIM(i.label), '') AS label,
    NULLIF(TRIM(i.notes), '') AS notes,
    i.netport_fk, b.mask_bits, i.last_discovered
FROM view_ipaddress_v2 i
JOIN computer c ON c.device_pk = ANY(i.device_fks)
LEFT JOIN view_subnet_v1 b ON b.subnet_pk = i.subnet_fk
ORDER BY i.ipaddress_pk, c.device_pk
LIMIT %d OFFSET %d
```

2026-09-15 두 서버에서 실행해 통과를 확인했다.
`netport_fk`와 `mask_bits`는 현재 적재 대상이 아니며 관계·서브넷 결정용으로 함께 조회한다.

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
CI 기준 6개 중 원천 대응이 있는 것은 주소 표기 둘뿐이며 같은 값이 들어간다.
선택 근거는 [IP 수집 설계](../../../design/ci/ip.md) 3절.

| ASSETATTRID | 한글 의미 | 값 컬럼 | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- |
| IPADDRESS_DOTNOTATION | 점 표기 주소 | ALNVALUE | 변환 | `i.ip_address` | `HOST()`. 전건 |
| IPADDRESS_STRINGNOTATION | 문자열 표기 주소 | ALNVALUE | 변환 | `i.ip_address` | `HOST()`. CI 기준 속성. DOTNOTATION과 같은 값 |

## 5. 미대응·미결

| 항목 | 상태 |
| --- | --- |
| **관계 경로** | `SYS.*COMPUTERSYSTEM`↔`NET.IPADDRESS` 규칙이 양방향 0건이다. CDM 경로는 `Computer → NET.IPINTERFACE → NET.IPADDRESS`다. Interface CI 도입은 범위 확대라 **사용자 결정 필요**. ISSUE-8 |
| 서브넷 마스크 | `b.mask_bits` 전건 보유하나 `NET.IPADDRESS`에 자리 없음. `NET.IPNETWORK` 별도 CI 필요. ISSUE-8 |
| `b.gateway` | 컬럼은 있으나 두 서버 모두 값 0건 |
| IPADDRESS_ADDRESSTYPE | NUMERIC. IPv4/IPv6 코드 규약 미확인. ISSUE-11 |
| `i.label` | 55 / 59건. `MODELOBJECT_LABEL` 채택 여부 미정 |
| 채울 속성이 하나뿐 | 독립 CI 유지 여부 재확인 대상. ISSUE-8 |
