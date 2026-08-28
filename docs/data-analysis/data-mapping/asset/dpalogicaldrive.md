# DPALOGICALDRIVE

배치된 자산 컴퓨터 논리 드라이브

> Target: MAXIMO.DPALOGICALDRIVE · ASSETCLASS: COMPUTER · 구현: DpaLogicalDriveIntegrate.java
> 관측 2026-08-27 · Device42 192.168.1.35, 192.168.2.68 / Maximo BLUDB

## 1. 관계

- 부모: MAXIMO.DEPLOYEDASSET (NODEID)
- 카디널리티: DEPLOYEDASSET 1 : N DPALOGICALDRIVE (PK 는 `LOGICALDRIVEID`. 관측 49노드/80행)
- 선행: DEPLOYEDASSET
- MERGE ID: `LOGICALDRIVEID = view_mountpoint_v1.mountpoint_pk`

## 2. 테이블 매핑

| Source | Target | 조인 조건 | 카디널리티 |
| --- | --- | --- | --- |
| `view_mountpoint_v1` | MAXIMO.DPALOGICALDRIVE | – | 1:1 (마운트포인트 1건 = 행 1건) |
| `view_device_v2` | (대상 판정) | `view_mountpoint_v1.device_fk = device_pk` | N:1 |

뷰 컬럼 구조는 두 서버가 동일하다. COMPUTER 대상과 의사 파일시스템 제외 결과는
다음과 같다.

| 서버 | 원천 | 제외 | 적재 대상 |
| --- | --- | --- | --- |
| 192.168.1.35 | 6장비/84행 | `overlay` 62, `devtmpfs` 2 | 6장비/20행 |
| 192.168.2.68 | 19장비/109행 | `overlay` 38, `devtmpfs` 9, `efivarfs` 1 | 19장비/61행 |

## 3. 조회 조건

| 조건 | 식 | 사유 |
| --- | --- | --- |
| 부모 적재 대상 | `d.type IN ('virtual','physical') AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)` | DEPLOYEDASSET 필터와 일치시킨다 |
| COMPUTER만 | `(d.network_device = false OR d.network_device IS NULL) AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer','PDU'))` | 다른 ASSETCLASS와 PDU의 자식을 만들지 않는다 |
| 의사 파일시스템 제외 | `LOWER(COALESCE(m.fstype_name,'')) NOT IN ('overlay','devtmpfs','efivarfs')` | 컨테이너 overlay, `/dev`, EFI 변수 의사 파일시스템은 논리 드라이브가 아니다 |

## 4. 컬럼 매핑

| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |
| --- | --- | --- | --- | --- | --- | --- |
| ATTACHEDNETNAME | 첨부된 네트워크 이름 | ALN(256) | Y | 변환 | `view_mountpoint_v1.filesystem`, `.fstype_name` | `fstype_name IN ('nfs','nfs4')` 일 때만 `filesystem`, 그 외 NULL |
| AVAILABLESIZE | 가용 크기 | DECIMAL(10,2) | Y | 직접 | `view_mountpoint_v1.free_capacity` | Device42 관측 단위 MB. 소수 2자리 |
| CHANGEDATE | 변경 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| COMPRESSED | 압축됨 | YORN(1) | N | 상수 | – | `0`. 기존 수집분도 80/80 전건 `0` |
| CREATEDATE | 작성 날짜 | DATETIME(10) | N | 채번 | – | 적재 시각 |
| DESCRIPTION | 설명 | ALN(256) | Y | 원천없음 | – | Device42 마운트포인트 뷰에 설명 컬럼이 없다 |
| DRIVETYPE | 드라이브 유형 | ALN(32) | Y | 상수 | – | `'UNKNOWN'`. 기존 수집분도 80/80 전건 `UNKNOWN` |
| ENCRYPTED | 비밀번호화됨 | YORN(1) | N | 상수 | – | `0`. Device42에 암호화 여부가 없고 기존 수집분도 전건 `0` |
| FILESYSTEM | 파일 시스템 | ALN(32) | Y | 직접 | `view_mountpoint_v1.fstype_name` | 필터 후 양 서버 관측 10종, 최대 7자. 2.68에 NULL 1건 |
| LOGICALDRIVEID | 논리 드라이브 ID | BIGINT(19) | N | 직접 | `view_mountpoint_v1.mountpoint_pk` | Maximo ID로 그대로 사용하며 MERGE 키로 삼는다 |
| MOUNT | 드라이브 | ALN(256) | Y | 직접 | `view_mountpoint_v1.mountpoint` | 관측 최대 131자 |
| NODEID | 노드 ID | BIGINT(19) | N | 직접 | `view_mountpoint_v1.device_fk` | 부모 DEPLOYEDASSET와 동일한 ID를 직접 사용한다 |
| SIZEUNIT | 크기 단위 | ALN(16) | Y | 상수 | – | `'MB'`. `capacity`, `free_capacity`의 Device42 관측 단위 |
| TOTALSIZE | 총 크기 | DECIMAL(10,2) | Y | 직접 | `view_mountpoint_v1.capacity` | Device42 관측 단위 MB. 소수 2자리 |
| VAVAILABLESIZE | 가용 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |
| VOLUMELABEL | 볼륨 레이블 | ALN(16) | Y | 변환 | `view_mountpoint_v1.label` | `LEFT(label, 16)`. 관측 최대 49자로 타겟 길이에 맞춘다 |
| VTOTALSIZE | 총 크기 | ALN(32) | Y | 원천없음 | – | 비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다 |

구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결

## 5. 조회 쿼리

```sql
SELECT
    m.mountpoint_pk,
    m.device_fk,
    m.mountpoint,
    m.filesystem,
    m.fstype_name,
    m.capacity,
    m.free_capacity,
    m.label
FROM view_mountpoint_v1 m
JOIN view_device_v2 d ON d.device_pk = m.device_fk
WHERE d.type IN ('virtual', 'physical')
  AND (d.virtualsubtype_id IS NULL OR d.virtualsubtype_id <> 15)
  AND (d.network_device = false OR d.network_device IS NULL)
  AND (d.physicalsubtype IS NULL OR d.physicalsubtype NOT IN ('Network Printer', 'PDU'))
  AND LOWER(COALESCE(m.fstype_name, '')) NOT IN ('overlay', 'devtmpfs', 'efivarfs')
ORDER BY m.device_fk, m.mountpoint, m.mountpoint_pk
```

## 6. 미결

없음.
