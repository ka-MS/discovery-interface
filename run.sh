#!/usr/bin/env bash
#
# 잡 실행기. Spring Boot가 config/application.yaml 을 자동 로드한 뒤 지정한 잡을 순서대로 실행한다.
#
#   ./run.sh asset
#   ./run.sh asset software conversion
#
# 잡 이름은 Spring 빈 이름이다: asset, ci, ci-relation, software, conversion
# ci 는 CI 본체·스펙에 이어 관계까지 적재한다. ci-relation 은 관계만 적재한다.
# ./run.sh ci ci-relation 처럼 둘 다 주면 관계가 두 번 돈다. MERGE 라 결과는 같다.
# 등록되지 않은 이름은 JobRunner 가 경고 없이 무시한다.
#
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

config_file="config/application.yaml"

if [[ ! -f "$config_file" ]]; then
  echo "설정 파일이 없습니다: $config_file" >&2
  echo "config/application.yaml 에 실제 값을 채우고 다시 실행하세요." >&2
  exit 1
fi

if [[ $# -eq 0 ]]; then
  echo "실행할 잡을 지정하세요." >&2
  echo "사용법: $0 <잡 이름> [잡 이름...]" >&2
  echo "예시:   $0 conversion asset software" >&2
  exit 1
fi

echo "설정 파일: $config_file"
echo "실행할 잡: $*"

./gradlew bootRun --args="$*"
