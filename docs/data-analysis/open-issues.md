# 미결 사항

판단과 의견은 이 문서에만 둔다. 다른 문서는 관측된 값과 매핑 규칙만 기술한다.

## ISSUE-1 SOURCEID 가 서버 간 불일치

**상태:** 정책 정의 대기. 기록만 한다.

`DEPLOYEDASSET` 의 MERGE 키는 `(SOURCEID, IMPORTSOURCE)` 이고 `SOURCEID` 에는
Device42 `device_pk` 가 들어간다. `device_pk` 는 수집 서버가 다르거나
재수집하면 값이 바뀐다. 동일 장비가 서버에 따라 다른 `SOURCEID` 로 적재된다.

동일 장비 확인 근거는 `serial_no` 와 `uuid` 다. 두 값은 서버가 달라도 같다.

`DEPLOYEDASSET.TLOAMNRSUUID` 에 Device42 `uuid` 가 이미 적재된다. 다만
`uuid` 는 전건 존재하지 않는다. 충전율은
`knowledge/device42/servers.md` 의 식별자 충전율 표를 참조한다.

## ISSUE-2 스위치가 두 레코드로 분리됨

**상태:** 연결 수단은 해결. 다중 멤버 스택의 값 귀속 규칙은 미결.

네트워크 장비가 Device42 에서 두 레코드로 나뉜다.

| 레코드 | 보유 | 미보유 |
| --- | --- | --- |
| `type = 'cluster'` | netport, MAC, 관리 IP | serial_no, OS |
| `type = 'physical'`, 이름에 ` - Switch N` 접미 | serial_no, OS, hardware_fk | netport, IP |

부모 DEPLOYEDASSET 은 `type IN ('virtual','physical')` 이므로 physical 쪽만
적재한다. 그런데 포트·MAC·IP 는 cluster 쪽에 있다.

**두 레코드는 `view_netport_v1.second_device_fk` 로 이어진다.** cluster 소속
포트의 `second_device_fk` 가 물리 레코드를 가리킨다.

```sql
SELECT DISTINCT device_fk AS cluster_pk, second_device_fk AS physical_pk
FROM view_netport_v1 WHERE second_device_fk IS NOT NULL
```

양쪽 서버에서 1:1 로 성립하며 결과가 일치한다.

| 서버 | cluster → physical | 연결 포트 수 |
| --- | --- | --- |
| .68 | 11 → 13, 12 → 108 | 28, 26 |
| .35 | 282 → 284, 281 → 283 | 28, 26 |

장비 레벨 FK(`host_chassis_device_fk`, `virtual_host_device_fk`,
`vm_manager_device_fk`, `chassisslot_fk`)는 네 레코드 모두 비어 있다. 연결은
포트 레벨에만 있다. 이전 기록은 장비 레벨만 확인하고 연결 수단이 없다고
판단한 것이며 사실과 다르다.

관리 IP 는 cluster 의 `Vlan1` 인터페이스에 붙어 있고 그 포트의
`second_device_fk` 는 비어 있다. 따라서 IP 는 포트 경유가 아니라 위 대응표로
cluster 를 특정한 뒤 조회한다.

MAC 은 cluster 포트 중 포트 이름이 MAC 과 같은 항목이 장비 베이스 MAC 이다.
물리 포트 MAC 은 그 값 바로 위 범위에서 증가한다.

**다만 베이스 MAC 포트는 `second_device_fk` 가 비어 있어 물리 멤버로 연결되지
않는다.** cluster 귀속 값이다. cluster 는 Cisco 스택이라 물리 멤버를 여러 개
가질 수 있고(이름의 ` - Switch N` 이 멤버 번호), 멤버가 여럿이면 전원이 같은
베이스 MAC 을 받게 된다. `DPANETDEVICE.NETMACADDR` 은 단수 컬럼이므로
귀속 규칙을 정해야 한다. 멤버 자신의 포트 MAC 최솟값이 대안이다.

관측 시점에는 양쪽 서버 모두 cluster 당 멤버가 1개라 1:N 동작을 실측하지
못했다. 관리 IP 도 cluster 당 1개이며, 스택은 IP 를 공유하므로 멤버가 여럿이면
여러 행이 같은 `NETWORKADDRESS` 를 갖는다.

적용은 `data-mapping/asset/dpanetdevice.md` 참조.

## ISSUE-3 PDU 가 COMPUTER 로 분류됨

**상태:** 수집 정책에 반영.

`physicalsubtype = 'PDU'` 인 장비가 현행 판정 순서에서 3번 분기로 떨어져
COMPUTER 가 된다. Maximo 에 PDU 용 ASSETCLASS 와 DPA 테이블이 없다.
수집 대상에서 제외하는 방향으로 정책에 반영한다.

판정 순서는 `knowledge/device42/device-types.md` 참조.

## ISSUE-4 자식 태스크의 조회 조건이 부모와 다르다

**상태:** 기록만 한다.

`DeployedAssetIntegrate` 와 `DpaComputerIntegrate` 의 원천 조회 조건이 서로 다르다.

| 태스크 | 조건 |
| --- | --- |
| DeployedAssetIntegrate | `type IN ('virtual','physical')` AND `virtualsubtype_id IS NULL OR <> 15` |
| DpaComputerIntegrate | `network_device` 거짓 AND `physicalsubtype <> 'Network Printer'` AND `type` 이 `unknown` 아님 |

두 조건의 차이를 관측 기준으로 대조한 결과다.

| 부모 | 자식 | 대상 | 건수 |
| --- | --- | --- | --- |
| 제외 | 포함 | virtual / Docker Container | 12 |
| 포함 | 제외 | physical / Generic (network_device) | 2 |
| 포함 | 제외 | physical / Network Printer | 1 |

`포함/제외` 3건은 정상이다. NETDEVICE·NETPRINTER 로 분류된 자산이라 DPACOMPUTER 대상이 아니다.

`제외/포함` 12건이 문제다. 부모가 적재하지 않은 Docker Container 를 자식이 대상으로 잡아, 매 실행마다 교차키 조회에 실패하고 경고 로그만 남긴다. 데이터가 잘못 들어가지는 않지만 불필요한 조회와 로그가 발생한다.

자식 태스크를 새로 만들 때 부모와 같은 필터를 쓰도록 맞춰야 한다.

## ISSUE-5 1:N 자식 테이블의 MERGE 매칭 키

**상태:** 정책 정의 대기. 대상은 `DPADISK` 하나로 좁혀졌다.

자체 ID 를 쓰는 10개 테이블은 ID 를 시퀀스로 발번한다. 발번은 해결됐지만
재실행 시 같은 원천 행을 다시 찾아낼 매칭 키가 따로 있어야 한다. 없으면
실행할 때마다 행이 늘어난다.

### 스키마는 테이블마다 자연키 컬럼을 제공한다

10개 테이블 모두 노드 내 식별자 컬럼을 갖고 있다. 별도 컨테이너가 필요하지
않다.

| 테이블 | 자연키 컬럼 | 한글명 | 기존 수집분 채움 |
| --- | --- | --- | --- |
| DPANETADAPTER | `NETMACADDR1` | MAC 주소 1 | 61/61 |
| DPALOGICALDRIVE | `MOUNT` | 드라이브 | 80/80 |
| DPATCPIP | `TCPIPADDRESS` | TCP/IP 주소 | 53/53 |
| DPAOS | `NAME` | 운영 체제 | 63/63 |
| DPASWSUITE | `SUITEID` | 스위트 ID | 14/14 |
| DPASOFTWARE | `TLOAMSOFTWAREID` | 소프트웨어 | 13031/13031 |
| DPACPU | `CPUNUM` | 프로세서 ID | 0/57 |
| DPADISK | `SERIALNUMBER` | 일련 번호 | 0/144 |
| DPADISPLAY | `SERIALNUMBER` | 일련 번호 | 0/52 |
| DPAMEDIAADAPTER | `SERIALNUMBER` | 일련 번호 | 0/39 |

장비 고유 속성(MAC, 마운트 경로, IP, OS 이름)은 전건 채워져 있고, 하드웨어
일련번호와 슬롯 번호는 전건 비어 있다. 스캐너가 읽지 못하는 값들이다.

`SERIALNUMBER` 는 디스크·모니터·미디어어댑터의 자연키다. 범용 컨테이너가
아니다. CPU 는 일련번호가 없어 자연키 자리를 `CPUNUM` 으로 따로 둔다.

### 매칭 정책은 자연키 하나로 통일한다

자연키로 매칭하면 재수집이 UPDATE 가 된다. 원천 pk 를 키에 넣으면 재수집이
INSERT 가 되어 행이 중복된다. 두 성격은 공존할 수 없으므로 자연키로 통일한다.

기존 수집분의 `DPACPU` 는 `CPUNUM` 을 비우고 `SERIALNUMBER` 에
`Source ID: <n>` 을 넣었다(57/57). 스키마 의도를 벗어난 우회이며 따르지 않는다.

### Device42 가 자연키를 댈 수 있는지

| 테이블 | 자연키 | Device42 원천 | 가능 |
| --- | --- | --- | --- |
| DPACPU | `CPUNUM` | `view_part_v1.slot` (43/43, `(device_fk, slot)` 유일) | 가능 |
| DPANETADAPTER | `NETMACADDR1` | `view_netport_v1.hwaddress` | 가능 |
| DPALOGICALDRIVE | `MOUNT` | `view_mountpoint_v1.mountpoint` | 가능 |
| DPATCPIP | `TCPIPADDRESS` | `view_ipaddress_v1.ip_address` | 가능 |
| DPAOS | `NAME` | `view_os_v1.name` | 가능 |
| DPADISK | `SERIALNUMBER` | `view_part_v1.serial_no` (.68 12/22 · .35 1/6) | 폴백 필요 |
| DPAMEDIAADAPTER | `SERIALNUMBER` | `view_part_v1.serial_no` (GPU 파트) | 미조사 |

### 남은 결정

**DPADISK 의 일련번호 폴백.** `.35` 만 보면 6건 중 1건이라 불가로 보이지만
`.68` 은 22건 중 12건이다. 일련번호가 없는 건은 전부 `sda NNN GB` 형태의
리눅스 수집분이고 해당 장비는 디스크가 1개뿐이다.

`COALESCE(NULLIF(serial_no,''), 모델명)` 을 쓰면 `.68` 22/22, `.35` 6/6 으로
노드 내 유일해진다. 이 폴백을 정식 규칙으로 채택할지 정한다.

**DPACPU 의 슬롯 중복.** `.35` 는 43/43 유일이지만 `.68` 은 67건 중 65개만
구별된다. 중복 2쌍은 수집 잔재다. `DESKTOP-P7KJHB7` 은 같은 소켓에 모델이 다른
두 건(E5-2620 v3, E5-2650 v4)이, `itmsg-gpu1` 은 같은 모델의 정규화 차이 두 건이
있다. 적재 전 중복 제거 규칙이 필요하다. 최신 것만 남길지, 건너뛰고 로그만
남길지 정한다.

**서버 한 대만 보고 판단하지 않는다.** 위 두 건 모두 한 서버만 봤을 때와
양쪽을 봤을 때 결론이 달랐다.

## 처리 완료

| 항목 | 결론 |
| --- | --- |
| 가상 장비 vendor 없음 | 이슈 아님. `MAXATTRIBUTE.DEFAULTVALUE` 가 `UNKNOWN` 으로 정의되어 있어 정상 결과다 |
