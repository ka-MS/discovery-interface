# DEPLOYEDASSET 모델

> 관측 2026-08-27 · Maximo BLUDB
> 재조회 docs/data-analysis/exploration-queries/maximo/dpa-child-coverage.sql

배치된 자산(Deployed Assets). 수집 도구가 발견한 자산의 공통 헤더다.

## 계층

`DEPLOYEDASSET` 이 부모, `DPA*` 가 자식이다. 모든 `DPA*` 테이블은 `NODEID`
단일 컬럼으로 부모를 참조한다. `DPA*` 에는 `IMPORTSOURCE` 컬럼이 없다.
출처 구분은 부모를 조인해야 한다.

## ASSETCLASS 판별자

`DEPLOYEDASSET.ASSETCLASS` 가 어떤 자식 테이블에 행이 붙는지 결정한다.

| ASSETCLASS | 자식 테이블 |
| --- | --- |
| COMPUTER | DPACOMPUTER, DPAOS, DPASOFTWARE, DPACPU, DPADISK, DPALOGICALDRIVE, DPANETADAPTER, DPATCPIP, DPAMEDIAADAPTER, DPADISPLAY, DPASWSUITE |
| NETDEVICE | DPANETDEVICE |
| NETPRINTER | DPANETPRINTER |

관측 시점 기준 ASSETCLASS 교차 사례와 부모 없는 자식 행은 없었다.

## 자식 테이블 커버리지

| 테이블 | 노드 수 | 행 수 |
| --- | --- | --- |
| DPACOMPUTER | 68 | 68 |
| DPAOS | 61 | 63 |
| DPADISK | 61 | 144 |
| DPASOFTWARE | 53 | 13031 |
| DPATCPIP | 53 | 53 |
| DPACPU | 49 | 57 |
| DPALOGICALDRIVE | 49 | 80 |
| DPANETADAPTER | 39 | 61 |
| DPAMEDIAADAPTER | 39 | 39 |
| DPADISPLAY | 37 | 52 |
| DPANETDEVICE | 34 | 34 |
| DPASWSUITE | 14 | 14 |
| DPANETPRINTER | 4 | 4 |

부분 커버리지가 정상이다. 관측 대상이 없으면 행을 만들지 않는다.

관측 시점 기준 `DPA*` 행은 전부 `IMPORTSOURCE` 가 비어 있는 기존 수집분에
붙어 있다. Device42 적재분(COMPUTER 28, NETDEVICE 2, NETPRINTER 1)에는 자식
행이 없다. 부모만 적재되고 자식 적재는 아직 구현되지 않았다.

기존 수집분 기준으로 `DPACOMPUTER` 만 68/68 로 전건 존재한다. 부모 1건당
자식 1건인 확장 테이블이기 때문이다. 나머지 자식 테이블은 부분이다.

## 키

- `NODEID` 는 `MAXIMO.DEPLOYEDASSETSEQ` 시퀀스로 발번된다.
- 현행 적재의 MERGE 키는 `(SOURCEID, IMPORTSOURCE)` 다.
  근거: `DeployedAssetIntegrate.java` `MERGE_DEPLOYED_ASSET_QUERY`
- `SOURCEID` 에는 Device42 `device_pk` 가 들어간다. 관련 미결 사항은
  `../../open-issues.md` 참조.
