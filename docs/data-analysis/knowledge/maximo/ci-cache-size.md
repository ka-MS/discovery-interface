# CI 정의 캐시 크기

> 관측: 2026-09-14 · Maximo BLUDB/MAXIMO · 읽기 전용 조회.
> 재조회: [전체 행·적용 범위](../../exploration-queries/maximo/ci-cache-size.sql),
> [컬럼 값·물리 크기](../../exploration-queries/maximo/ci-cache-payload-size.sql).
> 단위: MiB = 1,048,576 bytes. 현재 스냅샷이며 이후 데이터 증가분은 포함하지 않는다.

## 결과

전체 컬럼을 대상으로 했다. CLASSSPEC은 27개, ASSETATTRIBUTE는 10개 영속 컬럼이다.

| 범위 | 행 수 | 컬럼 값 합계 | Java 배열 행 캐시 | Java Map 행 캐시 |
| --- | ---: | ---: | ---: | ---: |
| CLASSSPEC 전체 | 39,237 | 3.79 MiB | 22.27 MiB | 61.78 MiB |
| CLASSSPEC — ACTCI 적용 분류 | 34,897 | 3.39 MiB | 19.82 MiB | 54.97 MiB |
| CLASSSPEC — ACTCI 또는 CI 적용 분류 | 36,562 | 3.55 MiB | 20.76 MiB | 57.58 MiB |
| ASSETATTRIBUTE 전체 | 6,715 | 0.87 MiB | 2.62 MiB | 5.13 MiB |

캐시 모델 두 가지는 서로 다른 구현의 실측값이다. 모든 구현의 최소·최대 범위를 뜻하지 않는다.
배열 행 기준 ACTCI 스펙과 전체 속성 캐시 합계는 22.45 MiB,
ACTCI+CI 스펙과 전체 속성 캐시 합계는 23.38 MiB다.
CLASSSPECUSEWITH·분류 캐시, JDBC 버퍼, 로딩 중 임시 객체는 이 합계에 포함하지 않았다.

## 오브젝트 범위

CLASSSPEC에는 OBJECTNAME이 없다. 다음 조건의 CLASSUSEWITH가 존재하는 분류를 포함했다.

- ACTCI: OBJECTNAME='ACTCI'
- ACTCI+CI: OBJECTNAME IN ('ACTCI','CI')
- EXISTS를 사용하므로 같은 분류가 양쪽 오브젝트에 적용되어도 CLASSSPEC 행을 중복 세지 않는다.

CLASSSPECUSEWITH의 속성별 적용 행 수는 ACTCI 32,992건, CI 1,665건이다.
ACTCI 분류의 CLASSSPEC 34,897건과는 1,905건 차이가 난다.
따라서 ‘ACTCI 분류에 속한 모든 스펙’과 ‘ACTCI용 속성 적용 설정까지 있는 스펙’을 구분해야 한다.

ASSETATTRIBUTE에서 같은 ASSETATTRID가 여러 행인 이름은 3개다.
속성명만을 키로 한 단일 Map에 전체 테이블을 담으면 덮어쓰기 가능성이 있으므로
숫자 ASSETATTRIBUTEID를 기본 키로 쓰거나 조직·사이트 범위를 확인한 조회 키가 필요하다.

## 측정 방법

- 컬럼 값: 문자열은 OCTET_LENGTH, 숫자·날짜는 LENGTH를 합산했다. NULL은 0으로 계산한다.
  행·가변길이·NULL 관리 헤더, 인덱스, 압축 및 빈 페이지는 포함하지 않는다.
  [IBM LENGTH](https://www.ibm.com/docs/en/db2/11.5.x?topic=functions-length),
  [IBM OCTET_LENGTH](https://www.ibm.com/docs/en/db2-as-a-service?topic=functions-octet-length).
- Java: 전체 조회 결과 TSV를 읽어 아래 두 객체 그래프를 각각 만들고,
  Instrumentation.getObjectSize로 참조되는 객체를 중복 없이 합산했다.
  측정 코드: [CacheFootprint.java](../../tools/CacheFootprint.java).
- 배열 행: HashMap<Long, String[]>.
- Map 행: HashMap<Long, Map<String, String>>. 각 행의 Map은 HashMap이며 컬럼명 String은 공유한다.
- JDBC 조회기의 TSV는 NULL을 빈 문자열로, 모든 값을 문자열로 표현하고 공백을 정리한다.
  따라서 이 수치는 실제 운영 DTO나 JDBC ResultSet의 힙 사용량은 아니다.
- 환경: WSL OpenJDK 25.0.4, -Xmx512m, compressed oops/class pointers,
  8-byte alignment, compact object headers 비활성.
  JVM 전체 사용량·로딩 최고점이 아닌 캐시 객체 그래프 크기다.
- TSV 파일 크기는 각각 3,220,552 / 2,879,677 / 3,013,689 / 556,845 bytes였다.
  원본 결과는 local/db-access-kit/work/ci-cache-size-20260914에만 보관한다.

## DB 디스크 할당량

| 전체 테이블 | 데이터 영역 | 인덱스 | 합계 |
| --- | ---: | ---: | ---: |
| CLASSSPEC | 3.75 MiB | 4.75 MiB | 8.50 MiB |
| ASSETATTRIBUTE | 1.375 MiB | 1.00 MiB | 2.375 MiB |

SYSIBMADM.ADMINTABINFO의 DATA_OBJECT_P_SIZE·INDEX_OBJECT_P_SIZE(KiB)를 사용했다.
LONG·LOB 영역은 둘 다 0이다. 오브젝트별 부분 집합에는 별도 디스크 할당량이 없으며,
DB 디스크 할당량은 Java 메모리 사용량과 다르다.
[IBM ADMINTABINFO](https://www.ibm.com/docs/en/db2/12.1.x?topic=aracp-admin-get-tab-info-retrieve-table-size-state-information).

## 캐시 판단

현재 건수는 전체 캐시를 검토할 만한 규모다. 특히 ACTCI와 CI를 함께 담아도
ACTCI만 담을 때보다 배열 행 캐시 기준 약 0.93 MiB 늘어난다.
각 행을 컬럼명 기반 Map으로 구성하면 객체 오버헤드가 크므로,
운영 구현에서는 필요한 필드를 가진 DTO와 조회용 Map을 사용하는 편이 적합하다.
운영 JVM 힙 여유와 실제 DTO·추가 인덱스 구성에 따른 측정은 캐시 구현 시 확인한다.

## 재현

DB 접속은 기존 run-maximo.sh로 두 SQL을 실행하고 결과를 local/db-access-kit/work 아래에 둔다.
이 관측에서는 WSL의 DB TCP 연결이 응답하지 않아 Windows JCC 경유로 동일 조회기를 실행했다.
인증정보·조회기는 변경하지 않았다.

조회 결과에 대해 아래 스크립트를 저장소 루트에서 실행한다.

```bash
bash docs/data-analysis/tools/measure-ci-cache.sh local/db-access-kit/work/ci-cache-size-20260914
```
