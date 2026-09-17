#!/usr/bin/env python3
"""Maximo 메타데이터 TSV 로 매핑 문서 골격을 생성한다.

입력:
  local/db-access-kit/work/maximo/column-skeleton.tsv
  local/db-access-kit/work/maximo/table-description.tsv

MAXATTRIBUTE 에는 DB 컬럼이 아닌 비영속 속성이 섞여 있다. PERSISTENT=0 인
행은 적재 대상이 아니라는 표시를 붙인다.
출력:
  docs/data-analysis/data-mapping/<패키지>/<테이블 소문자>.md

이미 존재하는 문서는 건너뛴다. 분석으로 채운 내용을 덮어쓰지 않는다.
저장소 루트에서 실행한다.
"""
import csv
import sys
from pathlib import Path

WORK = Path("local/db-access-kit/work/maximo")
OUT = Path("docs/data-analysis/data-mapping")

# 테이블 -> (문서 영역, maximo 영역별 Writer, ASSETCLASS)
TABLES = {
    "DEPLOYEDASSET":   ("asset", "DeployedAssetWriter", "COMPUTER, NETDEVICE, NETPRINTER"),
    "DPACOMPUTER":     ("asset", "DpaComputerWriter", "COMPUTER"),
    "DPAOS":           ("asset", "DpaOsWriter", "COMPUTER"),
    "DPACPU":          ("asset", "DpaCpuWriter", "COMPUTER"),
    "DPADISK":         ("asset", "DpaDiskWriter", "COMPUTER"),
    "DPALOGICALDRIVE": ("asset", "DpaLogicalDriveWriter", "COMPUTER"),
    "DPANETADAPTER":   ("asset", "DpaNetAdapterWriter", "COMPUTER"),
    "DPATCPIP":        ("asset", "DpaTcpIpWriter", "COMPUTER"),
    "DPAMEDIAADAPTER": ("asset", "DpaMediaAdapterWriter", "COMPUTER"),
    "DPADISPLAY":      ("asset", None, "COMPUTER"),
    "DPASWSUITE":      ("asset", None, "COMPUTER"),
    "DPANETDEVICE":    ("asset", "DpaNetDeviceWriter", "NETDEVICE"),
    "DPANETPRINTER":   ("asset", "DpaNetPrinterWriter", "NETPRINTER"),
    "DPASOFTWARE":     ("software", "DpaSoftwareWriter", "COMPUTER"),
}


def read_tsv(path):
    with path.open(encoding="utf-8") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def clean(value):
    """실행기가 빈 문자열을 큰따옴표 두 개로 출력하는 경우를 정리한다."""
    v = (value or "").strip()
    return "" if v == '""' else v


def type_label(row):
    """SCALE 이 0 이 아니면 MAXTYPE(LENGTH,SCALE) 로 적는다.

    DECIMAL 계열은 SCALE 없이는 반올림 자리수를 복원할 수 없다.
    대상 테이블에 DECIMAL(10,2) 컬럼이 13개 있다.
    """
    maxtype = clean(row["MAXTYPE"])
    length = clean(row["LENGTH"])
    scale = clean(row["SCALE"])
    if not length:
        return maxtype
    if scale and scale != "0":
        return f"{maxtype}({length},{scale})"
    return f"{maxtype}({length})"


def build(table, desc, columns):
    _package, impl, assetclass = TABLES[table]
    implementation = f"maximo/{_package}/{impl}.java" if impl else "없음 (원천 없음)"
    lines = [
        f"# {table}",
        "",
        desc or "(설명 없음)",
        "",
        f"> Target: MAXIMO.{table} · ASSETCLASS: {assetclass} · 저장: {implementation}",
        "> 조회·매핑 구현 참조는 해당 integration/d42maximo 기능 패키지에서 확인해 추가한다.",
        "",
        "## 1. 관계",
        "",
    ]
    if table == "DEPLOYEDASSET":
        lines += [
            "- 계층의 루트. 부모 없음.",
            "- `NODEID` 는 `MAXIMO.DEPLOYEDASSETSEQ` 로 발번한다.",
            "- 적재 대상 필터와 키 전략은 3번에 기술한다.",
        ]
    else:
        lines += [
            "- 부모: MAXIMO.DEPLOYEDASSET (NODEID)",
            f"- 카디널리티: DEPLOYEDASSET 1 : <1|N> {table}  <!-- MERGE 키로 확정한다 -->",
            "- 선행: DEPLOYEDASSET",
        ]
    lines += [
        "",
        "## 2. 테이블 매핑",
        "",
        "| Source | Target | 조인 조건 | 카디널리티 |",
        "| --- | --- | --- | --- |",
        f"|  | MAXIMO.{table} |  |  |",
        "",
        "## 3. 조회 조건",
        "",
        "| 조건 | 식 | 사유 |",
        "| --- | --- | --- |",
        "|  |  |  |",
        "",
        "## 4. 컬럼 매핑",
        "",
        "| Target 컬럼 | 한글명 | 타입 | Null | 구분 | Source | 변환·조건 |",
        "| --- | --- | --- | --- | --- | --- | --- |",
    ]
    for row in columns:
        nullable = "N" if clean(row["REQUIRED"]) == "1" else "Y"
        default = clean(row["DEFAULTVALUE"])
        notes = []
        if clean(row.get("PERSISTENT", "1")) == "0":
            notes.append("비영속 속성(PERSISTENT=0). DB 컬럼이 아니므로 적재 대상이 아니다")
        if default:
            notes.append(f"DEFAULTVALUE={default}")
        note = ". ".join(notes)
        lines.append(
            f"| {clean(row['ATTRIBUTENAME'])} | {clean(row['KO_TITLE'])} "
            f"| {type_label(row)} | {nullable} |  |  | {note} |"
        )
    lines += [
        "",
        "구분 허용값: 직접 / 변환 / 상수 / 채번 / 원천없음 / 미결",
        "",
        "## 5. 조회 쿼리",
        "",
        "3번 조건이 반영된, Device42 에서 원천을 끌어오는 SELECT 를 둔다.",
        "",
        "## 6. 미결",
        "",
        "`../../open-issues.md` 의 이슈 ID와 한 줄 요약만 둔다.",
        "",
    ]
    return "\n".join(lines) + "\n"


def main():
    col_rows = read_tsv(WORK / "column-skeleton.tsv")
    desc_rows = read_tsv(WORK / "table-description.tsv")
    descs = {r["OBJECTNAME"]: clean(r["KO_DESC"]) for r in desc_rows}

    by_table = {}
    for row in col_rows:
        by_table.setdefault(row["OBJECTNAME"], []).append(row)

    missing = sorted(set(TABLES) - set(by_table))
    if missing:
        print(f"메타데이터 없음: {', '.join(missing)}", file=sys.stderr)
        return 1

    created = skipped = 0
    for table, (package, _impl, _cls) in TABLES.items():
        path = OUT / package / f"{table.lower()}.md"
        if path.exists():
            skipped += 1
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(build(table, descs.get(table, ""), by_table[table]),
                        encoding="utf-8")
        created += 1
    print(f"생성 {created}건, 건너뜀 {skipped}건")
    return 0


if __name__ == "__main__":
    sys.exit(main())
