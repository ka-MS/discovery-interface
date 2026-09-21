# CI 매핑 명세

현재 코드의 Device42 → Maximo Actual CI 연계 명세다. 제출·가이드 작성은 아래 순서로 읽는다.
이번 문서 대조에서는 운영 DB에 접속하지 않았다. 과거 검증 기록은 날짜와 범위를 함께 인용한다.

## 읽는 순서

1. [분류·스펙 매핑](classstructure.md): CI 유형별 12개 분류, 전체 수집 속성, 정의 조회와 누락 처리.
2. [관계 통합 명세](relations.md): 일곱 관계도·방향·정확한 코드, 연결 키와 COUNT/PAGE SQL.
3. [ACTCI](actci.md)·[ACTCISPEC](actcispec.md)·[ACTCIRELATION](actcirelation.md): 전체 컬럼·키·바인딩·실제 MERGE.
4. 아래 유형별 원천 SQL·값 변환.
5. [문서 점검 기록](../documentation-audit.md): 코드 기준과 검증 범위.

## 현재 구현

| 유형 | 원천·변환 명세 | 현재 실행 범위 |
| --- | --- | --- |
| Device | [device.md](types/device.md), [실행 준비](types/device-run.md) | 물리 Computer·VM·판정 가능한 Switch·Network Cluster. Printer·Router 제외 |
| DB Instance | [database-instance.md](types/database-instance.md) | 엔진별 네 분류, 다섯 속성. Component를 경유하는 RUNSON 관계 |
| OS | [os.md](types/os.md) | SYS.OPERATINGSYSTEM, 다섯 속성. Computer 대상 INSTALLEDON |
| Disk | [disk.md](types/disk.md) | DEV.DISKDRIVE, 세 속성. Computer 출발 CONTAINS |
| Filesystem | [filesystem.md](types/filesystem.md) | SYS.FILESYSTEM, 다섯 속성. 선택 조건을 통과한 모든 Computer–마운트 연결 |
| IP | [ip.md](types/ip.md) | NET.IPADDRESS, 네 속성. 본체는 PK당 하나, USES는 연결 쌍별 |

실행: `ci`는 정의 준비 → DB Instance → Device → Disk → Filesystem → IP → OS → 관계다.
`ci-relation`은 관계만 실행한다. 본체 실패 후에도 관계를 실행하되 정의 준비 실패는 전파한다.
구현 책임은 Source(Query·원천 모델), Pipeline(정책·Mapper·Import·Job), Target(DTO·Writer·정의 조회)이다.
본체·속성은 공통 ActCiWriter, 관계는 공통 ActCiRelationWriter를 쓴다.

## 실행 명세 밖의 자료

- [독립 Database](types/database.md): 미구현 검토안. 현재 `ci`가 생성하지 않는다.
- [CI 대상 후보](ci-targets.md): 2026-09-04 조사 스냅샷이며 현재 구현 목록이 아니다.
- [CI 모델](../../knowledge/maximo/ci-model.md)·[분류 모델](../../knowledge/maximo/ci-classification.md): DB 구조 관측.
- [분류 관측](../../knowledge/maximo/computer-classification-specs.md)·[관계 관측](../../knowledge/maximo/computer-ci-relations.md): 당시 등록값·검증 이력.
- [관계 설계](../../design/ci/relations.md) 및 [ISSUE-8·11](../../open-issues.md): 선택 이유·후속 범위와 미결 정책.

현재 코드에 분류·관계가 있다는 사실은 운영 기준정보 등록·실제 적재·CI 승격·UI 검증 완료를 뜻하지 않는다.
ETL은 기준정보 등록이나 Authorized CI 승격을 수행하지 않는다.
