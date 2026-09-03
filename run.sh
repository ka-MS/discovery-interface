#!/usr/bin/env bash
#
# 잡 실행기. config/application.env 의 환경변수를 주입한 뒤 지정한 잡을 순서대로 실행한다.
#
#   ./run.sh asset
#   ./run.sh asset software conversion
#
# 잡 이름은 Spring 빈 이름이다: asset, ci, software, conversion
# 등록되지 않은 이름은 JobRunner 가 경고 없이 무시한다.
#
# 환경 파일 경로는 APP_ENV_FILE 로 바꿀 수 있다.
#   APP_ENV_FILE=config/application-prod.env ./run.sh asset

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

env_file="${APP_ENV_FILE:-config/application.env}"

if [[ ! -f "$env_file" ]]; then
  echo "환경 파일이 없습니다: $env_file" >&2
  echo "config/application.env 에 실제 값을 채우고 다시 실행하세요." >&2
  exit 1
fi

if [[ $# -eq 0 ]]; then
  echo "실행할 잡을 지정하세요." >&2
  echo "사용법: $0 <잡 이름> [잡 이름...]" >&2
  echo "예시:   $0 conversion asset software" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
. "$env_file"
set +a

required=(
  MAXIMO_JDBC_URL MAXIMO_JDBC_USERNAME MAXIMO_JDBC_PASSWORD
  DEVICE42_REST_BASE_URL DEVICE42_REST_USERNAME DEVICE42_REST_PASSWORD
  DEVICE42_TRUSTSTORE DEVICE42_TRUSTSTORE_PASSWORD
  DEVICE42_JDBC_URL DEVICE42_JDBC_USERNAME DEVICE42_JDBC_PASSWORD
)
missing=()
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || missing+=("$name")
done
if [[ ${#missing[@]} -gt 0 ]]; then
  echo "환경변수가 비어 있습니다: ${missing[*]}" >&2
  echo "$env_file 를 확인하세요." >&2
  exit 1
fi

echo "환경 파일: $env_file"
echo "실행할 잡: $*"

./gradlew bootRun --args="$*"
