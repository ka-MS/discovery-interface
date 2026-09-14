#!/usr/bin/env bash
set -euo pipefail
source_dir="$(cd "$(dirname "$0")" && pwd)"
output_dir="$(cd "${1:?TSV 결과 디렉터리를 지정하세요}" && pwd)"
build_dir="$output_dir/footprint-classes"
mkdir -p "$build_dir"
javac -d "$build_dir" "$source_dir/CacheFootprint.java"
printf 'Premain-Class: CacheFootprint\n' > "$build_dir/agent-manifest.mf"
jar cfm "$build_dir/footprint-agent.jar" "$build_dir/agent-manifest.mf" -C "$build_dir" CacheFootprint.class
java -Xmx512m -cp "$build_dir" -javaagent:"$build_dir/footprint-agent.jar" \
  --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED \
  CacheFootprint "$output_dir/classspec-all.tsv" "$output_dir/classspec-actci.tsv" \
  "$output_dir/classspec-actci-ci.tsv" "$output_dir/assetattribute-all.tsv" > "$output_dir/heap.tsv"
