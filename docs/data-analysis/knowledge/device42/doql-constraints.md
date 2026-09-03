# DOQL 실행 제약

Device42 접근은 PostgreSQL 직접 접속이 아니라 DOQL REST API
(`POST /services/data/v1.0/query/`) 다.

## 차단되는 것

카탈로그 조회는 모두 HTTP 500 으로 거부된다.

| 대상 | 결과 |
| --- | --- |
| `information_schema.tables` / `.views` | 500 |
| `pg_views` | 500 |
| `pg_class` | 500 |

뷰 목록을 쿼리로 얻을 수 없다. 실재 여부는 `SELECT * FROM <뷰> LIMIT 1` 로
개별 확인한다. 존재하지 않는 뷰도 500 을 반환하므로, DOQL 의 500 은 접속
실패가 아니라 뷰명 또는 권한 오류로 해석한다.

## 허용되는 것

- `SELECT *` 사용 가능. 헤더 행으로 컬럼 목록을 얻을 수 있다.
- `WITH` 절 사용 가능. CTE 를 여러 개 이어 붙이는 것도 된다.
- `UNION ALL`, 스칼라 서브쿼리, `EXISTS` 사용 가능.
- `HOST()`, `MASKLEN()`, `CAST(... AS VARCHAR)` 사용 가능.

## 쿼리 작성 제약

- `FROM` 절 서브쿼리는 500 으로 거부된다. 같은 형태를 CTE 로 바꾸면 통과한다.

  ```sql
  -- 거부: SELECT ... FROM ( SELECT ... ) s
  -- 통과: WITH counted AS ( SELECT ... ) SELECT ... FROM counted
  ```

- JSON 컬럼은 `->>` 와 `LATERAL jsonb_object_keys()` 가 동작하지만
  `CAST(details AS VARCHAR) <> '{}'` 는 500 이다. 비어 있는지는 키 개수로
  판정한다. 상세는 `json-columns.md` 참조.
- `inet` 타입 컬럼에 문자열 함수를 그대로 쓰면 500 이다.
  `view_ipaddress_v2.ip_address` 와 `view_subnet_v1.gateway` 가 해당한다.
  `LENGTH`, `NULLIF`, `POSITION` 앞에 `CAST(... AS VARCHAR)` 를 넣는다.
  단 캐스팅 결과에는 `/32` 접미가 붙는다. 주소 값만 필요하면 `HOST()` 를 쓴다.

## 실행기 제약

- 쿼리는 `SELECT` 또는 `WITH` 로 시작해야 한다.
- 블록 이름은 `[a-zA-Z0-9-]` 만 허용된다. 언더스코어가 들어가면 블록이
  조용히 무시되고 쿼리가 실행되지 않는다. 실패로 보고되지 않으므로
  결과 파일 생성 여부로 확인한다.
- 설명 주석은 첫 `-- name:` 앞 헤더에만 둔다. 실행기가 블록 본문을 한 줄로
  이어 붙이기 때문이다. 이름 줄 바로 뒤에 주석을 두면 `SELECT`/`WITH` 로
  시작하지 않아 거부되고, 다음 이름 줄 앞에 두면 앞 블록 SQL 끝에 붙어
  `;` 뒤에 텍스트가 생겨 500 이 된다.
- 바인드 파라미터를 지원하지 않는다. 대상 지정은 쿼리 최상단 CTE 를
  수정한다.
