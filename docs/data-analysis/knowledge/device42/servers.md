# Device42 서버별 데이터 성격

> 관측 2026-08-27 · Device42 192.168.1.35
> 재조회 docs/data-analysis/exploration-queries/device42/source-coverage-by-subtype.sql

접속 대상은 서버별 접속 파일을 `DB_ACCESS_ENV` 로 지정해 고른다.
`connections.env` 를 편집하지 않는다. 다른 세션이 같이 쓰기 때문이다.

| 서버 | 접속 파일 |
| --- | --- |
| 192.168.2.68 | `local/db-access-kit/connections-d42-68.env` |
| 192.168.1.35 | `local/db-access-kit/connections-d42-35.env` |

두 파일은 `D42_RESOLVE` 한 줄만 다르고 나머지는 `connections.env` 와 같다.
`D42_BASE_URL` 은 인증서 이름과 맞추기 위해 `https://Device42Demo` 로 유지한다.

실행 예시는 `../../exploration-queries/README.md` 참조.

| 서버 | 용도 |
| --- | --- |
| 192.168.2.68 | 소프트웨어·파트·마운트 원천이 넓다 |
| 192.168.1.35 | 네트워크·OS 원천이 넓다 |

매핑 검증 시 한 서버로는 전 테이블을 덮지 못한다. 대상 테이블에 맞는
서버를 선택한다. 아래 두 표는 192.168.1.35 에서 관측한 수치다.

## subtype 별 원천 가용성

| subtype | devices | has_os | has_software | has_netport | has_ip | has_part | has_mount | has_hardware |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| VMWare | 55 | 55 | 5 | 55 | 50 | 6 | 6 | 0 |
| Docker Container | 12 | 12 | 0 | 10 | 10 | 0 | 0 | 0 |
| Generic | 6 | 6 | 0 | 4 | 4 | 6 | 0 | 6 |
| Amazon EC2 Instance | 5 | 0 | 0 | 5 | 5 | 0 | 0 | 0 |
| Hyper-V | 2 | 0 | 0 | 1 | 0 | 0 | 0 | 0 |
| cluster | 2 | 0 | 0 | 2 | 2 | 0 | 0 | 0 |
| Network Printer | 1 | 1 | 0 | 1 | 1 | 1 | 0 | 1 |
| PDU | 1 | 0 | 0 | 1 | 1 | 0 | 0 | 1 |
| unknown | 1 | 1 | 0 | 0 | 0 | 0 | 0 | 0 |

## 식별자 충전율

| subtype | devices | has_uuid | has_serial |
| --- | --- | --- | --- |
| VMWare | 55 | 55 | 6 |
| Generic | 6 | 4 | 5 |
| Amazon EC2 Instance | 5 | 0 | 0 |
| Hyper-V | 2 | 2 | 0 |
| PDU | 1 | 0 | 1 |
| Network Printer | 1 | 0 | 1 |

가상 장비는 `hardware_fk` 가 비어 있다. Device42 가 가상 장비에 하드웨어
모델을 부여하지 않는 구조이며 재수집으로 채워지지 않는다. 이 경우
`DEPLOYEDASSET.MANUFACTURER` 는 `MAXATTRIBUTE.DEFAULTVALUE` 인
`UNKNOWN` 이 된다.
