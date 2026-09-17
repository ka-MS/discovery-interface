# 종료된 사항

## ISSUE-1 SOURCEID가 서버 간 불일치

**결론:** 이슈 아님.

데모 Device42 서버마다 같은 장비의 `device_pk`가 다르다. 운영은 단일 Device42
서버를 사용하므로 서버 간 ID 일치는 요구사항이 아니다.

## ISSUE-3 PDU가 COMPUTER로 분류됨

**결론:** 해결.

`physicalsubtype = 'PDU'`가 COMPUTER로 분류됐다. Maximo에 대응 ASSETCLASS와
DPA 테이블이 없어 `DEPLOYEDASSET`과 COMPUTER 자식의 조회 대상에서 제외했다.

필터 적용 전에 적재된 PDU 1건(`SOURCEID=172`)은 Maximo에 남아 있다. 삭제
정책은 별도로 논의한다.

## ISSUE-4 자식 태스크의 조회 조건이 부모와 다름

**결론:** 해결.

COMPUTER 자식이 부모에서 제외한 Docker Container 등을 조회했다. 부모의 타입,
Docker Container, PDU 제외 조건을 동일하게 적용한 뒤 COMPUTER 대상만 조회한다.

## ISSUE-5 1:N 자식 테이블의 MERGE 매칭 키

**결론:** `SOURCE_TARGET_MAP`을 사용하지 않는다.

운영은 단일 Device42를 사용한다. `DEPLOYEDASSET.NODEID`에는 `device_pk`, 각 DPA
자식의 `NODEID`에는 `device_fk`, 자체 ID에는 해당 원천 레코드 PK를 직접 넣는다.
Maximo 시퀀스와 별도 교차키 없이 대상 ID를 MERGE 키로 사용한다.

원천이 사라졌을 때의 삭제·비활성화 정책은 별도로 논의한다.

## 가상 장비 vendor 없음

**결론:** 이슈 아님.

`MAXATTRIBUTE.DEFAULTVALUE`가 `UNKNOWN`으로 정의되어 있어 정상 결과다.

## ISSUE-9 구현이 낮은 버전 뷰를 씀

**상태:** 반영 완료. 2026-08-31.

`view_hardware_v1`·`view_ipaddress_v1`·`view_mountpoint_v1` 을 쓰던 7곳을
`_v2` 로 바꿨다. 규칙은 `../../CLAUDE.md` 와
`knowledge/device42/views.md` 버전 규칙 절.

`view_hardware_v2` 는 조인 키가 같아 이름만 바꿨다.
`DeployedAssetIntegrate`, `DpamManufacturerIntegrate`, `DpamManuVariantIntegrate`.

`view_ipaddress_v2` 와 `view_mountpoint_v2` 는 `device_fk` 가 `device_fks`
배열로 바뀌어 `= ANY(...)` 로 조인한다. `DpaTcpIpIntegrate`,
`DpaNetDeviceIntegrate`, `DpaNetPrinterIntegrate`, `DpaLogicalDriveIntegrate`.

### 배열 조인이 만든 ID 중복

한 IP 가 장비 여럿에 걸리면 같은 `ipaddress_pk` 가 여러 행이 되어 `TCPIPID` 가
중복된다. 2026-08-31에는 MERGE 키를 유지하기 위해 `DISTINCT ON`으로 장비 하나만
남겼다. 2026-09-17 이 축약이 실제 장비–IP 연결을 잃는다는 점을 재검토해 폐기했다.
현재는 배열의 모든 연결을 보존하고, `(NODEID, TCPIPADDRESS)`를 MERGE 자연키로,
`DPATCPIPSEQ` 채번값을 `TCPIPID`로 쓴다. 정본은 `data-mapping/asset/dpatcpip.md`다.

당시 `MIN(device_fks)` 로 고르는 방법은 버렸다. 필터를 통과하지 못하는 장비가
뽑혀 행이 사라진다. `.68` 의 `192.168.1.81` 이 `unknown` 타입 장비와
`DESKTOP-P7KJHB7` 에 함께 걸려 있어 45행이 44행이 됐다. 현재 구현은 대표 장비를
선택하지 않으므로 이 문제 자체가 없다.

`DpaNetDeviceIntegrate` 와 `DpaNetPrinterIntegrate` 는 스칼라 서브쿼리라
행이 늘지 않는다.

### 적재 결과 변화

| 대상 | .35 v1 | .35 v2 | .68 v1 | .68 v2 |
| --- | ---: | ---: | ---: | ---: |
| DPATCPIP | 70 | 70 | 45 | 45 |
| DPALOGICALDRIVE | 20 | **29** | 61 | 61 |

`.35` 의 9행은 ESXi 호스트의 VMFS 데이터스토어다. 논리 드라이브로 적재하기로
했다. 상세는 `data-mapping/asset/dpalogicaldrive.md`.

`DEPLOYEDASSET`, `DPANETDEVICE`, `DPANETPRINTER` 는 변화 없다.

탐색 쿼리 10개도 함께 고쳤고 양쪽 서버에서 전 블록 실행을 확인했다.
