# Device42 → Maximo 데이터 분석

Device42(원천)와 Maximo(타겟) 사이의 매핑 작업 문서다.

## 읽는 순서

1. `knowledge/maximo/deployedasset-model.md` — 타겟 구조와 ASSETCLASS 판별자
2. `knowledge/device42/views.md` — 원천 뷰와 device 연결 키
3. `data-mapping/README.md` — 라우팅과 진행 현황
4. `data-mapping/<패키지>/<테이블>.md` — 테이블별 매핑

## 구성

| 디렉터리 | 내용 |
| --- | --- |
| `knowledge/` | 관측 사실. 수치는 스냅샷이며 상단에 관측 시점과 재조회 쿼리를 명시한다 |
| `data-mapping/` | 테이블 단위 매핑 정본 |
| `exploration-queries/` | 재사용 조회 쿼리 |
| `tools/` | 매핑 문서 골격 생성 |
| `open-issues.md` | 미결·정책 대기 항목 |

접속 수단은 `local/db-access-kit/` 에 있다. git 추적 대상이 아니다.

## 작성 규칙

- 관측된 값과 매핑 규칙만 기술한다. 판단과 의견은 `open-issues.md` 에 둔다.
- 같은 사실을 반복하지 않는다. 결론에 필요한 수치만 남긴다.
- 테이블·컬럼은 한글명을 함께 적고, Source 와 조건은 실행 가능한 식으로 쓴다.
- pk 리터럴에 의존하지 않는다. 원천 pk 는 재수집 시 바뀐다.
- 수치를 담는 문서는 상단에 관측 스탬프를 단다.

## 컬럼 매핑 표

7열 고정이다. 앞 4열은 Maximo 메타데이터에서 생성한다.

| 열 | 출처 |
| --- | --- |
| Target 컬럼 | `MAXATTRIBUTE.ATTRIBUTENAME` |
| 한글명 | `L_MAXATTRIBUTE.TITLE` (LANGCODE='KO') |
| 타입 | `MAXATTRIBUTE.MAXTYPE` + `LENGTH` |
| Null | `MAXATTRIBUTE.REQUIRED` 반전 |
| 구분 | 분석. 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결 |
| Source | 분석. Device42 뷰.컬럼 |
| 변환·조건 | 분석. 변환식, 기본값, 조건 |

골격 생성:

```bash
bash local/db-access-kit/scripts/run-maximo.sh \
  docs/data-analysis/exploration-queries/maximo/column-skeleton.sql \
  local/db-access-kit/work/maximo
python3 docs/data-analysis/tools/gen-mapping-skeleton.py
```

설계 문서는 `../superpowers/specs/2026-08-27-data-analysis-docs-design.md` 다.
