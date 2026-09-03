# 미결 사항

판단과 의견은 이 문서에만 둔다. 다른 문서는 관측된 값과 매핑 규칙만 기술한다.
종료된 사항은 `close-issues.md` 에 둔다.

## ISSUE-2 스위치가 두 레코드로 분리됨

**상태:** 해결.

네트워크 장비가 Device42 에서 두 레코드로 나뉜다.

| 레코드 | 보유 | 미보유 |
| --- | --- | --- |
| `type = 'cluster'` | netport, MAC, 관리 IP | serial_no, OS |
| `type = 'physical'` (` - Switch N` 접미) | serial_no, OS, hardware_fk | netport, IP |

부모 DEPLOYEDASSET 은 `physical` 만 적재하는데 네트워크 정보는 cluster 에 있다.
장비 레벨 FK(`host_chassis_device_fk`, `virtual_host_device_fk`,
`vm_manager_device_fk`, `chassisslot_fk`)는 네 레코드 모두 비어 있다.

연결은 포트 레벨에 있다. cluster 소속 포트의 `second_device_fk` 가 물리
레코드를 가리킨다.

```sql
SELECT DISTINCT device_fk AS cluster_pk, second_device_fk AS physical_pk
FROM view_netport_v1 WHERE second_device_fk IS NOT NULL
```

`second_device_fk` 가 채워진 포트는 cluster → physical 방향뿐이며 양쪽 서버에서
결과가 일치한다(`.68` 11→13, 12→108 · `.35` 282→284, 281→283).

### 결정

- **MAC** — 위 대응으로 묶은 포트들의 `hwaddress` 최솟값. 멤버 고유값이라 스택
  멤버가 늘어도 겹치지 않는다. 베이스 MAC 포트는 `second_device_fk` 가 비어
  자동 제외된다.
- **IP** — cluster 의 `device_pk` 로 `view_ipaddress_v1` 직접 조회. 관리 IP 가
  `Vlan1` 논리 인터페이스에 붙어 있고 그 포트는 `second_device_fk` 가 비어
  포트 경유가 안 된다. 스택은 IP 를 공유하므로 멤버가 여럿이면 여러 행이 같은
  값을 갖는다.

적용은 `data-mapping/asset/dpanetdevice.md` 참조.

### 잔여

관측 시점에 cluster 당 물리 멤버가 1개뿐이라 1:N 동작을 실측하지 못했다.
멤버가 둘 이상인 스택이 생기면 재확인한다.

## ISSUE-6 DPA 변환 데이터 등록

**상태:** 기본 적재 구현 완료. 정규화 정책 논의 필요.

Maximo UI 는 `DPA*` 자식 테이블을 직접 읽지 않는다. 자식 위에 얹힌 뷰를 읽고,
그 뷰가 변환 변형과 INNER 조인한다. 값이 변환 변형에 없으면 행이 오류 없이
사라진다.

### 결정

- 일반 변환 데이터는 Device42를 직접 조회하는 `conversion` 잡에서 적재한다.
- 전체 실행 순서는 `conversion → asset → ci → software`다.
- 제조사·OS·프로세서·어댑터의 대상과 변형을 이름 기준 MERGE하고 신규 ID는
  Maximo 시퀀스로 발번한다.
- TLOAMSOFTWARE는 `software` 잡에서 DPASOFTWARE보다 먼저 적재한다.
- TLOAMSOFTWARE는 검증된 `UNIQUEID`로 MERGE하고, 조회한 ID를
  `DPASOFTWARE.TLOAMSOFTWAREID`와 `TLOAMPRODUCTID`에 넣는다.
- `DPAMSOFTWARE`와 `DPAMSWVARIANT`는 DPACSOFTWARE 뷰가 조인하지 않으므로
  현재 적재하지 않는다.

상세 매핑은 `data-mapping/conversion/`과
`data-mapping/software/tloamsoftware.md`에 둔다.

### 남은 결정

- 원시 문자열을 그대로 등록할지 별칭을 정규명으로 통합할지 결정이 필요하다.
- `DPANETADAPTER.MAKEMODEL`의 상수 `UNKNOWN`을 변환 데이터에 등록할지 결정이
  필요하다.

## ISSUE-7 원천에서 사라진 행의 처리

**상태:** 논의 필요.

현재 MERGE는 조회된 원천만 INSERT 또는 UPDATE한다. Device42에서 사라진 부모나
자식은 Maximo에 그대로 남는다. 삭제할지 비활성화할지 별도로 결정한다.

## ISSUE-8 Actual CI 대상 범위

**상태:** 논의 필요.

Device42의 본체 후보 View가 모두 별도 Actual CI를 뜻하지 않는다. 조사 결과는
`data-mapping/ci/ci-targets.md`에 있다.

확인된 중복 표현은 다음과 같다.

- Database 전용 View는 `view_resource_v2`와 PK와 이름이 전건 일치한다.
- Kubernetes Cluster·Deployment·Node·Service 전용 View는
  `view_resource_v2`와 PK, identifier, 이름이 전건 일치한다.
- Cloud Instance는 Device와 전건 1:1로 연결되지만 PK와 이름은 전건 동일하지 않다.
- Database Instance는 Application Component와 연결되지만 PK와 이름은 다르다.

### 남은 결정

- Actual CI로 관리할 개체 유형
- `view_resource_v2`의 유형별 포함 목록
- Cloud Instance와 Database Instance를 별도 CI로 만들지 여부
- Application Group의 상태별 포함 기준
- Service Instance의 적재 단위와 상태별 포함 기준
- Subnet, VLAN, VRF를 Actual CI로 관리할지 여부

## ISSUE-10 전력·설비 서브타입의 적재 제외 범위

**상태:** 정책 정의 대기.

`DEPLOYEDASSET.ASSETCLASS` 는 도메인이 없는 자유 `ALN(32)` 다. 값 제약이
없으므로 실제 기준은 받아 줄 자식 테이블이 있느냐다. 현재 DPA 자식 18개는
전부 컴퓨터·네트워크·프린터·이미징·통신 계열이고 전력·설비 대응 테이블이
없다.

현행 필터는 `physicalsubtype <> 'PDU'` 하나만 제외한다.
근거: `DeployedAssetIntegrate.java` `DEVICE_FILTER`.

같은 성격인데 빠져 있는 값이 다섯이다.

| pk | 값 | 성격 |
| ---: | --- | --- |
| 6 | `CRAC` | 항온항습 |
| 7 | `UPS` | 무정전 전원 |
| 9 | `Branch Circuit Power Meter` | 분전반 계측 |
| 10 | `Power Unit` | 전원 |
| 15 | `Environment Monitor` | 환경 센서 |

`view_physicalsubtype_v2.building` 이 거짓인 것은 `CRAC` 과
`Environment Monitor` 둘뿐이라, 원천 플래그만으로는 다섯을 다 가려낼 수 없다.

관측 시점에 다섯 모두 데이터에 없다. 지금 필터를 넓혀도 걸러지는 건수는 0이다.

`TAP`(8) 은 판단이 갈린다. 네트워크 장비라 `DPANETDEVICE` 에 넣을 수는 있다.

### 남은 결정

- 다섯 값을 필터에 넣을지
- `TAP` 의 취급
- 넣는다면 `DeployedAssetIntegrate` 외에 conversion 부모 범위 쿼리 4곳도
  같이 맞춰야 한다. 근거: `data-mapping/conversion/dpammanufacturer.md` 부모 범위
