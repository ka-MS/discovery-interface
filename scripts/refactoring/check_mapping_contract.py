"""제출용 매핑 명세와 현재 코드의 구조·SQL 대조. DB 접속·문서 수정 없음.

SourceSqlParityTest를 먼저 실행해 build/refactoring/current-sql을 생성한다.
SQL 정규화 비교는 문서 전사 검증이며 실제 DOQL/DB2 실행 검증이 아니다.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs/data-analysis/data-mapping"
JAVA = ROOT / "src/main/java/com/itmsg/device42"
documents = {p: p.read_text(encoding="utf-8") for p in DOCS.rglob("*.md")}


def compact(sql):
    return re.sub(r"\s+", "", re.sub(r"--[^\n]*", "", sql)).rstrip(";").lower()


blocks = [compact(block) for text in documents.values()
          for block in re.findall(r"```sql\s*(.*?)```", text, re.S)]


def contains_sql(sql):
    expected = compact(sql)
    return any(expected in block.split(";") for block in blocks)


# Writer SQL 전부: 키·UPDATE 대상·INSERT 기본값까지 전사했는지 확인한다.
writers = sorted((JAVA / "target/maximo").rglob("*Writer.java"))
merge_count = 0
for writer in writers:
    for name, sql in re.findall(r'static final String (\w+) = """\n(.*?)""";',
                               writer.read_text(), re.S):
        if "MERGE INTO" in sql:
            assert contains_sql(sql), f"Writer SQL 문서 누락/불일치: {writer.name}:{name}"
            merge_count += 1
assert merge_count == 24, merge_count

# 실행 시 선택된 원천 SQL 30개와 관계 COUNT 7개.
generated = ROOT / "build/refactoring/current-sql"
pages = sorted(generated.glob("*-page.sql"))
assert len(pages) == 30, "SourceSqlParityTest를 --rerun-tasks로 먼저 실행해야 한다"
for page in pages:
    sql = re.sub(r"LIMIT\s+\d+\s+OFFSET\s+\d+", "LIMIT 1000 OFFSET 0", page.read_text())
    assert contains_sql(sql), f"PAGE 문서 불일치: {page.name}"

relation_source = (JAVA / "pipeline/d42maximo/ci/relation/CiRelationSource.java").read_text()
relations = re.findall(r'^    (\w+)\(Device42Relation\.(\w+), "([^"]+)"\)', relation_source, re.M)
relation_doc = (DOCS / "ci/relations.md").read_text()
assert len(relations) == 7
for pipeline, source, code in relations:
    assert f"| {pipeline} | {source} |" in relation_doc, pipeline
    line = next(line for line in relation_doc.splitlines() if f"| {pipeline} |" in line)
    assert f"| {code} |" in line, (pipeline, code)
    count = (generated / f"{source}-count.sql").read_text()
    assert contains_sql(count), f"COUNT 문서 불일치: {source}"

mapping = JAVA / "pipeline/d42maximo/ci/mapping"
classification_doc = (DOCS / "ci/classstructure.md").read_text()
classifications = re.findall(r'^    \w+\("([^"]+)"\)',
                             (mapping / "CiClassification.java").read_text(), re.M)
class_rows = set(re.findall(r'^\|.*?\| (SYS\.[\w.]+|DEV\.[\w.]+|NET\.[\w.]+|APP\.[\w.]+) \|',
                            classification_doc, re.M))
assert set(classifications) == class_rows, (set(classifications) - class_rows, class_rows - set(classifications))
specs = set()
spec_entries = 0
for source in mapping.glob("*Spec.java"):
    attributes = re.findall(r'^    \w+\("([A-Z_]+)"', source.read_text(), re.M)
    specs.update(attributes)
    spec_entries += len(attributes)
spec_rows = re.findall(r'^\| [CVSNODFIB] \| ([A-Z_]+) \|', classification_doc, re.M)
assert set(spec_rows) == specs, (specs - set(spec_rows), set(spec_rows) - specs)
assert len(spec_rows) == spec_entries, (len(spec_rows), spec_entries)

# 정의 로더 원문도 공통 명세에 있어야 한다.
loader = (JAVA / "target/maximo/ci/definition/CiDefinitionLoader.java").read_text()
for name, sql in re.findall(r'static final String (\w+) = """\n(.*?)""";', loader, re.S):
    assert contains_sql(sql), f"정의 SQL 불일치: {name}"

# 로컬 링크와 Markdown 제목 앵커. 과거 문서에서 새 명세로 연결하는 링크도 검사한다.
def anchors(text):
    result = set()
    counts = {}
    for heading in re.findall(r"^#{1,6}\s+(.+)$", text, re.M):
        slug = re.sub(r"[^\w\-\s]", "", heading.lower()).replace(" ", "-")
        count = counts.get(slug, 0)
        counts[slug] = count + 1
        result.add(slug if count == 0 else f"{slug}-{count}")
    result.update(re.findall(r'<a\s+(?:id|name)="([^"]+)"', text))
    return result


links = 0
for path in [ROOT / "CLAUDE.md", *sorted((ROOT / "docs").rglob("*.md"))]:
    text = path.read_text(encoding="utf-8")
    for link in re.findall(r"\]\(([^)]+)\)", text):
        if re.match(r"(https?:|mailto:)", link) or re.search(r"\s", link):
            continue
        target, _, anchor = link.partition("#")
        dest = (path.parent / target).resolve() if target else path
        assert dest.exists(), (path.relative_to(ROOT), link)
        # 범위 밖 기존 문서의 앵커 문제는 이번 매핑 검증과 분리한다.
        if anchor and dest.suffix == ".md" and (path.is_relative_to(DOCS) or dest.is_relative_to(DOCS)):
            assert anchor in anchors(dest.read_text(encoding="utf-8")), (path.relative_to(ROOT), link)
        links += 1

print(f"PASS: Writer MERGE {merge_count}개, PAGE {len(pages)}개, 관계 COUNT {len(relations)}개, 정의 조회 3개")
print(f"PASS: 분류 {len(classifications)}개, 스펙 행 {spec_entries}개/고유 {len(specs)}개, 관계 {len(relations)}개")
print(f"PASS: 로컬 링크 {links}개 및 매핑 관련 앵커")
print("범위: 코드·문서 전사와 커버리지. 실제 DB 등록 상태·실적재·UI 검증은 수행하지 않음.")
