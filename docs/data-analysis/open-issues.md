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

## ISSUE-5 1:N 자식 테이블의 MERGE 매칭 키

**상태:** 적재 정책 확정. 삭제 정책 논의 필요.

자식 테이블의 자체 ID 는 Maximo 시퀀스로 발번한다. 재실행 시 같은 원천 행을
찾기 위해 `SOURCE_TARGET_MAP` 을 신설한다.

| 컬럼 | 값 |
| --- | --- |
| `SOURCE_SYSTEM` | `DEVICE42` |
| `SOURCE_OBJECT` | Device42 원천 뷰 |
| `SOURCE_ID` | 원천 레코드 PK |
| `SOURCE_PARENT_ID` | `device_pk` |
| `TARGET_SYSTEM` | `MAXIMO` |
| `TARGET_OBJECT` | Maximo 대상 테이블 |
| `TARGET_ID` | 대상 테이블 시퀀스 ID |
| `TARGET_PARENT_ID` | `NODEID` |
| `CREATED_DATE` | 매핑 생성 일시 |
| `UPDATED_DATE` | 매핑 변경 일시 |

원천 매칭 키는 `(SOURCE_SYSTEM, SOURCE_OBJECT, SOURCE_ID)` 다. 대상에는
`(TARGET_SYSTEM, TARGET_OBJECT, TARGET_ID)` 유일 제약을 둔다. 서버 주소는
식별자에 포함하지 않는다.

| 대상 | 대상 ID·시퀀스 | 원천 ID |
| --- | --- | --- |
| `DPACPU` | `CPUID` · `DPACPUSEQ` | `view_part_v1.part_pk` |
| `DPADISK` | `DISKID` · `DPADISKSEQ` | `view_part_v1.part_pk` |
| `DPADISPLAY` | `DISPLAYID` · `DPADISPLAYSEQ` | 원천 없음 |
| `DPALOGICALDRIVE` | `LOGICALDRIVEID` · `DPALOGICALDRIVESEQ` | `view_mountpoint_v1.mountpoint_pk` |
| `DPAMEDIAADAPTER` | `ADAPTERID` · `DPAMEDIAADAPTERSEQ` | `view_part_v1.part_pk` |
| `DPANETADAPTER` | `ADAPTERID` · `DPANETADAPTERSEQ` | `view_netport_v1.netport_pk` |
| `DPAOS` | `OSID` · `DPAOSSEQ` | `view_deviceos_v1.deviceos_pk` |
| `DPASOFTWARE` | `SOFTWAREID` · `DPASOFTWARESEQ` | `view_softwareinuse_v1.softwareinuse_pk` |
| `DPASWSUITE` | `DPASWSUITEID` · `DPASWSUITESEQ` | 원천 없음 |
| `DPATCPIP` | `TCPIPID` · `DPATCPIPSEQ` | `view_ipaddress_v1.ipaddress_pk` |

`DPACOMPUTER`, `DPANETDEVICE`, `DPANETPRINTER`는 `NODEID`가 기본키인 1:1
테이블이므로 이 정책을 적용하지 않는다. 원천이 없는 테이블은 행과 매핑을
생성하지 않는다.

매핑이 있으면 `TARGET_ID` 로 대상 행을 갱신한다. 매핑이 없으면 Maximo 시퀀스로
대상 행을 삽입한 뒤 매핑을 삽입한다. 대상 쓰기와 매핑 쓰기는 같은 Db2에서
원천 행 하나 단위의 트랜잭션으로 처리한다. 실패한 행만 롤백하고 다음 행을
계속 처리한다.

원천 PK 가 변경되면 새 레코드로 삽입한다. 더 이상 조회되지 않는 원천과 기존
대상·매핑의 삭제 또는 비활성화 정책은 별도로 논의한다.

## ISSUE-6 DPA 마스터 데이터 등록

**상태:** 추후 고려. 현재는 자식 테이블만 적재한다.

Maximo 는 자식 테이블에 이름을 넣기 전에 정규화하는 계층을 둔다. 도메인마다
마스터와 변형 두 테이블이 있다.

| 도메인 | 마스터 | 행 | 변형 | 행 |
| --- | --- | --- | --- | --- |
| 소프트웨어 | `DPAMSOFTWARE` | 1982 | `DPAMSWVARIANT` | 1982 |
| 제조사 | `DPAMMANUFACTURER` | 315 | `DPAMMANUVARIANT` | 315 |
| 어댑터 | `DPAMADAPTER` | 59 | `DPAMADPTVARIANT` | 59 |
| OS | `DPAMOS` | 23 | `DPAMOSVARIANT` | 23 |
| 프로세서 | `DPAMPROCESSOR` | 13 | `DPAMPROCVARIANT` | 13 |

마스터는 정규명 목록이고(`<도메인>NAME` 유일 인덱스), 변형은 수집기가 주워온
원시 문자열을 정규명으로 접는 매핑표다(`<도메인>VARIANT` 유일 인덱스).
자식 테이블은 정규명을 저장한다. `*MOVE` 는 `PERSISTENT = 0` 이라 물리 테이블이
없다.

### 결정

자식 테이블만 적재한다. 마스터 등록은 추후 고려한다.

근거는 강제성이 없다는 점이다. `SYSCAT.REFERENCES` 에 DPA 계열 FK 제약이
하나도 없어 미등록 이름을 넣어도 적재가 성공한다. 현행 `DEPLOYEDASSET` 적재분
31건 중 3건(`Cisco` 2, `ASUS` 1)이 이미 `DPAMMANUFACTURER` 미등록 상태로
들어가 있고 문제가 발생하지 않았다.

정규화 기계가 실제로 돌고 있지도 않다. 변형표는 전 도메인 전건
`정규명 = 원시명` 이라 별칭이 하나도 없고, 마스터의 `VALIDATED` 도 전건 `0` 이다.
기존 수집기가 발견한 이름을 1:1 로 등록만 해놓은 상태다.

### 추후 고려할 때 필요한 것

기존 수집분은 자식 이름을 전건 마스터에 등록해 놓았다.

| 자식 컬럼 | 마스터 | 기존 수집분 등록률 |
| --- | --- | --- |
| `DPAOS.NAME` | `DPAMOS` | 63/63 |
| `DPAOS.MANUFACTURER` | `DPAMMANUFACTURER` | 63/63 |
| `DPACPU.MAKEMODEL` | `DPAMPROCESSOR` | 57/57 |
| `DPANETADAPTER.MAKEMODEL` | `DPAMADAPTER` | 61/61 |
| `DPASOFTWARE.MANUFACTURER` | `DPAMMANUFACTURER` | 12974/13031 |

등록하지 않으면 해당 이름은 사전에 존재하지 않는다. 라이선스 준수나 이름 통합을
시작할 때 후보 목록에 뜨지 않는다. 그때 소급 등록할지, 등록 태스크를 추가할지
정한다.

등록 태스크를 만든다면 자식 태스크와 결합도가 낮다. `DPAM*` 는 노드와 무관한
전역 사전이라 부모의 `NODEID` 발번을 기다릴 필요가 없고, `@Order` 로 자식보다
앞에 두면 된다. 각 마스터에 대응 시퀀스가 있다(`DPAMSOFTWARESEQ` 등).

Device42 원천도 있다. `view_softwareinuse_v1.alias_name`(관측 674/4900)이
변형 테이블과 같은 개념이다.
