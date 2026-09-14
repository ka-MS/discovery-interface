# ⚡ Discovery Interface

Device42에서 수집한 자산 정보를 Maximo로 연계하는 데이터 동기화 애플리케이션입니다.

하드웨어, 운영체제, 네트워크, 소프트웨어 및 변환 기준정보를 작업 단위로 조회하고 Maximo DB2에 반영합니다.

## 📢 주요 기능

- Device42 DOQL REST/JDBC 기반 원천 데이터 조회
- Maximo DB2 대상 데이터 등록 및 갱신
- 자산, 소프트웨어, 변환 기준정보별 독립 실행
- 여러 연계 작업의 순차 실행
- 작업별 오류 로깅 및 후속 작업 계속 처리
- Device42와 Maximo 간 데이터 매핑 문서 및 검증 쿼리 제공

## 🛠 개발 환경

- Java 25
- Spring Boot 4.1.1
- Gradle Wrapper
- IBM Db2 JDBC Driver
- Device42 JDBC Driver 1.0.2

## 🚀 시작하기

```bash
git clone https://github.com/ka-MS/discovery-interface.git
cd discovery-interface
```

프로젝트 루트의 `config/application.yaml`에 실행 환경에 맞는 접속정보를 설정합니다.
Spring Boot가 이 파일을 외부 설정으로 자동 로드하며, 자격정보 보호를 위해 Git에서는 제외됩니다.

```yaml
spring:
  datasource:
    url: "jdbc:db2://<host>:<port>/<database>"
    username: "<username>"
    password: "<password>"

device42:
  doql:
    rest:
      base-url: "https://<host>"
      username: "<username>"
      password: "<password>"
      truststore: "config/d42-truststore.p12"
      truststore-password: "<password>"
    jdbc:
      url: "jdbc:doql://<host>"
      username: "<username>"
      password: "<password>"
```

## 🔧 연계 작업 실행

### 실행 스크립트 사용

`run.sh`는 설정 파일 존재 여부를 확인한 뒤 Gradle `bootRun`을 실행합니다.

```bash
# 단일 작업
./run.sh asset

# 여러 작업
./run.sh conversion asset software
```

### Gradle로 직접 실행

```bash
# 단일 작업
./gradlew bootRun --args="asset"

# 여러 작업
./gradlew bootRun --args="conversion asset software"
```

### 실행 가능한 JAR로 실행

```bash
# JAR 생성
./gradlew bootJar

# 단일 작업
java -jar build/libs/discovery-interface-0.0.1-SNAPSHOT.jar asset

# 여러 작업
java -jar build/libs/discovery-interface-0.0.1-SNAPSHOT.jar \
  conversion asset software
```

| 작업 | 설명 |
| --- | --- |
| `conversion` | 제조사, 프로세서, OS, 어댑터 등의 변환 기준정보 연계 |
| `asset` | 컴퓨터, CPU, 디스크, OS, 네트워크 등의 자산정보 연계 |
| `software` | 설치 소프트웨어 및 라이선스 대상 소프트웨어 연계 |
| `ci` | CI 연계용 작업 진입점 |

## 🧪 테스트 및 빌드

```bash
# 테스트
./gradlew test

# 실행 가능한 JAR 빌드
./gradlew bootJar
```

## 📚 데이터 매핑 문서

수집 요구사항의 기준은 [사업 추진 범위](docs/requirements/business-scope.md)에 정리되어 있습니다.

Device42와 Maximo 간 데이터 구조, 테이블별 매핑, 검증 쿼리는
[`docs/data-analysis`](docs/data-analysis/README.md)에서 확인할 수 있습니다.

## 🔐 보안 유의사항

- `config/application.yaml`과 실제 운영 자격정보는 저장소에 커밋하지 않습니다.
- truststore 경로와 비밀번호는 외부 설정 파일이나 운영 환경의 Secret으로 관리합니다.
- 운영 실행 전 Device42 및 Maximo 접속 대상과 `MAXIMO` 스키마 접근 권한을 확인합니다.
