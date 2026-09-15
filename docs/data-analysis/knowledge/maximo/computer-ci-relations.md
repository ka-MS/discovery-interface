# Computer CI 관계 정의

> 관측: 2026-09-15 · Maximo BLUDB / MAXIMO. 정의 조회 및 OS–Computer 한 쌍의 적재·승격 검증.
> 재조회: [computer-ci-relations.sql](../../exploration-queries/maximo/computer-ci-relations.sql).
> 로컬 결과: `local/db-access-kit/work/ci-relations-20260915/maximo/`.
> 현재 설치 환경의 설정이며 국제표준의 필수 분류·코드 목록이 아니다.

## 정확한 관계 코드와 분류 쌍

C는 SYS.COMPUTERSYSTEM 또는 SYS.VIRTUALCOMPUTERSYSTEM을 뜻한다.
둘은 서로 다른 CLASSSTRUCTUREID이며, 각 분류의 명시적 ACTCI 적용 규칙을 조회했다.

| SOURCECLASS 분류명 | RELATIONNUM | TARGETCLASS 분류명 | CARDINALITY | CONTAINMENT | REVRELATIONSHIP | 규칙 SWAPPED |
| --- | --- | --- | --- | ---: | ---: | ---: |
| C | RELATION.CONTAINS | DEV.DISKDRIVE | 1:N | 1 | 0 | 0 |
| C | RELATION.CONTAINS | SYS.FILESYSTEM | 1:N | 1 | 0 | 0 |
| SYS.OPERATINGSYSTEM | RELATION.INSTALLEDON | C | N:1 | 1 | 1 | 1 |
| SYS.OPERATINGSYSTEM | RELATION.RUNSON | C | 1:1 | 0 | 1 | 1 |
| C | RELATION.CONTAINS | NET.IPINTERFACE | 1:N | 1 | 0 | 0 |
| NET.IPINTERFACE | RELATION.BINDSTO | NET.IPADDRESS | 1:1 | 0 | 1 | 0 |
| NET.IPINTERFACE | RELATION.ROUTESVIA | NET.IPADDRESS | 1:1 | 0 | 1 | 0 |
| SYS.VIRTUALCOMPUTERSYSTEM | RELATION.VIRTUALIZES | C | 1:1 | 1 | 1 | 1 |
| SYS.COMPUTERSYSTEM | RELATION.VIRTUALIZES | SYS.COMPUTERSYSTEM | 1:1 | 1 | 1 | 1 |
| SYS.OPERATINGSYSTEM | RELATION.BOOTSFROM | SYS.FILESYSTEM | 1:1 | 0 | 0 | 0 |
| C | RELATION.CONTAINS | SYS.CPU | 1:N | 1 | 0 | 0 |

Computer와 IPADDRESS의 직접 규칙은 양방향 모두 없다.
SYS.COMPUTERSYSTEM → SYS.VIRTUALCOMPUTERSYSTEM의 VIRTUALIZES 규칙도 없다.
Computer끼리의 MANAGES·CONNECTS·CONNECTEDTO는 대조한 분류 쌍에 없다.
같은 장비에 속한다는 이유만으로 OS–Filesystem 또는 Disk–Filesystem 관계를 생성할 수 없다.
Filesystem끼리는 REALIZES/VIRTUALIZES, IP끼리는 DEFINEDUSING 규칙이 있지만 현재 조회에서
그 의미에 해당하는 원천 연결을 확보하지 않았으므로 채택하지 않는다.

실제 분류 ID: Computer CCI10470, Virtual Computer CCI10793, OS CCI10538,
Disk CCI10631, Filesystem CCI10497, IP CCI10810, Interface CCI10490, CPU CCI10634.
ID는 환경별 조회값이며 코드 상수로 복사하지 않는다.

`CONTAINS`와 `RELATION.CONTAINS`, `RUNSON`과 `RELATION.RUNSON`은 별개 RELATION 행이다.
위 분류 쌍의 규칙은 접두어를 포함한 코드다. 조회한 RELATION.* 설명은 비어 있으며,
표의 코드를 한국어 표시명으로 치환해 저장하지 않는다.

## 참조·고유키

- ACTCIRELATION 물리 PK: ACTCIRELATIONID.
- 고유 인덱스: (SOURCECI, TARGETCI, RELATIONNUM).
- SOURCECI/TARGETCI는 ACTCI.ACTCINUM 문자열을 참조한다. ACTCIID가 아니다.
- ACTCIRELATION의 분류쌍 관계식은 코드와 **실제 양 끝 ACTCI의 분류**를 대조한다.
- 이번 SYSCAT.TABCONST 조회에는 PK만 있다. 물리 FK·CHECK로 양 끝 존재나 카디널리티를 강제하지 않는다.
- ACTCIRELATIONSEQ가 존재하며 증가폭은 1000이다. 아래 샘플 INSERT에서 NEXT VALUE로 ID 6001을 발급했다.
- CARDINALITY·CONTAINMENT·REVRELATIONSHIP은 RELATIONRULES의 필드다. ACTCIRELATION에 복사할 열이 아니다.

## 플래그 의미와 주의

MAXATTRIBUTE의 REVRELATIONSHIP 설명은 **도착 분류가 출발 분류의 부모인지**다.
따라서 OS → Computer에서 1인 것은 Computer가 부모라는 뜻이며,
SOURCECI/TARGETCI를 다시 뒤집으라는 지시가 아니다.

IBM의 [ITIC Actual CI 관계 처리 설명](https://www.ibm.com/docs/SSSQ39/ICDwiki.pdf)은
원천 순서와 규칙이 맞지 않을 때 역방향 규칙을 찾아 양 끝을 바꾸며,
이렇게 저장 순서를 바꾼 경우 ACTCIRELATION.SWAPPED=1로 표시한다고 설명한다.
따라서 **규칙의 SWAPPED를 관계 행에 무조건 복사한다는 근거는 없다**.
D42에는 관계 DTO의 출발·도착이 사전 정의되어 있지 않으므로 우리 매핑이 기준 방향을 명시해야 한다.
규칙에 맞는 OS → 물리 Computer 방향을 생성하고 SWAPPED=0으로 저장한 한 쌍은
아래 샘플에서 CI 승격·관계 표시까지 확인했다. 다른 관계·가상 Computer에는 일반화하지 않는다.
이 IBM 근거는 과거 ITIC 동작 설명이지 우리 직접 JDBC 적재의 실행 검증은 아니다.

조회한 관계의 USEWITH는 CI, TYPE은 UNIDIRECTIONAL이다.
USEWITH='ACTCI'가 없다는 사실만으로 ACTCIRELATION 저장이 불가능하다고 결론내릴 수 없다.
같은 IBM 설명은 규칙을 찾지 못한 Actual 관계도 저장하는 경로를 설명한다.
이 프로젝트에서 미등록 분류 쌍을 보류하는 것은 **의미와 승격을 검증한 매핑만 쓰려는 선택**이다.

## 카디널리티 확인 범위

- VM–호스트: 원천은 한 VM당 한 호스트, 한 호스트당 여러 VM. 등록 규칙은 1:1.
- Interface–IP: 원천의 한 포트에 여러 IP. 등록 규칙은 1:1.
- IBM [CDM 가이드](https://www.redbooks.ibm.com/redpapers/pdfs/redp4389.pdf)의 표 3·7은
  각각 Interface–IP 1:m, 가상화 m:1 예시를 제시한다.
  이를 근거로 현재 설치 DB의 규칙을 자동 변경하지 않는다.
- 복수 관계 저장에 따른 UI·승격·탐색 동작은 별도 검증 대상이다.
  임의로 첫 번째 VM/IP만 선택해서 1:1에 맞추지 않는다.

## OS–Computer 승격 샘플 검증

2026-09-15 사용자 승인으로 ACTCIRELATION 한 건을 직접 INSERT·커밋한 뒤,
사용자가 기존 Computer 승격 범위로 승격을 수행하고 CI 목록·관련 CI 화면을 제공했다.
이후 DB를 읽기 전용으로 재조회해 OS CI 및 CIRELATION 생성을 확인했다.
샘플 번호는 검증 증거이며 구현 상수가 아니다.

| 항목 | 확인 결과 |
| --- | --- |
| 원천 연결 | 사용자 제공 OS 쿼리 결과: deviceos_pk=147, device_fk=173, VMware ESXi 6.7.0 |
| 출발 ACTCI | D42:DEVICEOS:147 · SYS.OPERATINGSYSTEM |
| 도착 ACTCI | D42:DEVICE:173 · SYS.COMPUTERSYSTEM · 192.168.2.252 |
| 적재 전 | 양 끝 ACTCI 존재, 해당 관계 없음. Computer CI는 이미 존재하고 OS CI는 없음 |
| ACTCI 관계 | ACTCIRELATIONID=6001, OS → Computer, RELATION.INSTALLEDON, SWAPPED=0 |
| 승격 범위 | CI.COMPUTERSYSTEM ↔ SYS.COMPUTERSYSTEM. SYS.OPERATINGSYSTEM → CI.OS 매핑 포함 |
| ACTCI 룰 | SYS.OPERATINGSYSTEM → SYS.COMPUTERSYSTEM, N:1, CONTAINMENT=1, REVRELATIONSHIP=1, 규칙 SWAPPED=1 |
| CI 룰 | CI.OS → CI.COMPUTERSYSTEM, N:N, CONTAINMENT=1, REVRELATIONSHIP=1, 규칙 SWAPPED=0 |
| 승격 후 OS | CINUM=D42:DEVICEOS:147, CINAME=VMWARE ESXI, 분류 CI.OS. ACTCINUM은 원래 OS ACTCI와 연결 |
| 승격 후 Computer | CINUM=D42:DEVICE:173, 분류 CI.COMPUTERSYSTEM. ACTCINUM은 원래 Computer ACTCI와 연결 |
| 승격 후 관계 | CIRELATIONID=16, SOURCECI=D42:DEVICEOS:147, TARGETCI=D42:DEVICE:173, RELATIONNUM=RELATION.INSTALLEDON |
| 상위 식별자 | CIRELATION.PARENTCI와 ANCESTORCI 모두 D42:DEVICE:173 |
| UI | 사용자 제공 화면에서 OS CI 및 OS → Computer 관련 CI 한 건 확인 |

**결론:** 이 샘플은 기존 관계·룰·승격 범위를 재사용해 OS → 물리 Computer 관계를
SWAPPED=0으로 적재하고, CI로 승격한 뒤 관계 방향과 부모 Computer를 보존했다.
추가 관계 코드나 룰은 만들지 않았다. ACTCIRELATION의 ANCESTORCI·GUID·BASELINEDATE는
NULL로 적재했으며 ROWSTAMP는 기존 DB 트리거가 채웠다.

검증은 **단건 INSERT와 사용자 승격 결과**다. 제품의 관계 저장 구현, 공통 MERGE,
재실행 멱등성, 속성별 승격 전달, 가상 Computer 및 다른 관계는 이번 검증에 포함하지 않는다.
화면에서 선택한 세부 승격 옵션은 기록되지 않았다.

재조회: [OS–Computer 승격 대조](../../exploration-queries/maximo/os-computer-promotion-check.sql).
로컬 원본: `local/db-access-kit/work/os147-device173/`의 before·after·promoted 결과와
insert-result.txt, rollback.sql. 복구 SQL은 이번 ACTCI 관계 ID와 세 컬럼 키만 대상으로 하며
실행하지 않았다. 승격된 CI·CIRELATION을 지우는 SQL이 아니다.
