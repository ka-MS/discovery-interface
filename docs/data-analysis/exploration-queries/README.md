# 탐색 쿼리

Device42와 Maximo 원천을 반복 조회하는 재사용 쿼리다. 실행기는
`local/db-access-kit/scripts/` 에 있다.

## 실행

```bash
bash local/db-access-kit/scripts/run-device42.sh <쿼리파일> <출력디렉터리>
bash local/db-access-kit/scripts/run-maximo.sh   <쿼리파일> <출력디렉터리>
```

출력은 `local/db-access-kit/work/` 아래에만 둔다. 저장소에 커밋하지 않는다.

## 쿼리 파일 규칙

- 블록 구분은 `-- name: <이름>`. 이름에 쓸 수 있는 문자는 `[a-zA-Z0-9-]` 뿐이다.
  언더스코어가 들어가면 블록이 무시되고 쿼리가 실행되지 않는다.
- 블록 이름이 결과 파일명이 된다.
- Device42는 `SELECT` 또는 `WITH` 로 시작해야 한다. Maximo는 `SELECT` 와
  `VALUES` 만 허용된다.

## 등록 기준

- 재실행 가능하다. 대상 변경이 최상단 한 줄 또는 `IN` 목록 수정으로 끝난다.
- 특정 조사 1회로 끝나지 않는다.
- 파일명이 목적을 설명한다.
- pk 리터럴에 의존하지 않는다.

## 목록

| 쿼리 | 용도 |
| --- | --- |
| `maximo/table-description.sql` | 테이블 한글 설명 |
| `maximo/column-skeleton.sql` | 컬럼 매핑표 앞 4열 생성 |
