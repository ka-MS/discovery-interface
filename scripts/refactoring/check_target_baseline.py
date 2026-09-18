"""8cabf920 대비 실행/매핑/저장 본문 보존과 패키징을 대조한다. DB 접근 없음.

SQL의 정책/투영 변경은 SourceSqlParityTest 및 *EquivalenceTest가 별도로 검증한다.
이 검사는 그 테스트를 대체하거나 실제 DOQL/DB2 적재를 입증하지 않는다.
"""
from pathlib import Path
import re
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
REVISION = '8cabf92030d882e732613e07f493de41de800b61'
PREFIX = 'src/main/java/com/itmsg/device42/'
NAMES = {
    'DpamManufacturerQuery': 'ManufacturerNamesQuery', 'DpamManuVariantQuery': 'ManufacturerNamesQuery',
    'DpamOsQuery': 'OperatingSystemNamesQuery', 'DpamOsVariantQuery': 'OperatingSystemNamesQuery',
    'DpamProcessorQuery': 'ProcessorModelsQuery', 'DpamProcVariantQuery': 'ProcessorModelsQuery',
    'DpamAdapterQuery': 'AdapterModelsQuery', 'DpamAdptVariantQuery': 'AdapterModelsQuery',
    'TloamSoftwareQuery': 'SoftwareCatalogQuery', 'DpaSoftwareQuery': 'InstalledSoftwareQuery',
    'DeployedAssetQuery': 'DeviceQuery',
}

def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True)

def destination(path):
    rel = path.removeprefix(PREFIX)
    if rel.startswith('device42/'):
        rel = 'source/' + rel
    elif rel.startswith('maximo/'):
        rel = 'target/' + rel
    elif rel.startswith('integration/d42maximo/'):
        tail = rel.removeprefix('integration/d42maximo/')
        name = Path(tail).stem
        area = 'source/device42/' if ((name.endswith(('Query', 'Source')) and name != 'CiRelationSource')
                                      or name == 'ViewDeviceV2') else 'pipeline/d42maximo/'
        rel = area + str(Path(tail).with_name(NAMES.get(name, name) + '.java'))
    return ROOT / PREFIX / rel

def mask(text):
    return re.sub(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: re.sub(r'[^\n]', ' ', m.group()), text)

def members(text):
    masked = mask(text)
    start = masked.index('{', masked.index('public class')) + 1
    depth, end, result = 1, start, {}
    for pos in range(start, len(masked)):
        char = masked[pos]
        if char == '{': depth += 1
        if char == '}': depth -= 1
        if depth == 0: break
        if (char == ';' and depth == 1) or (char == '}' and depth == 1):
            block = text[end:pos + 1].strip()
            header = re.sub(r'@\w+(?:\([^)]*\))?\s*', '', mask(block).strip()).split('{')[0]
            if '(' in header and ('=' not in header or header.index('(') < header.index('=')):
                name = re.search(r'(\w+)\s*\(', header)[1]
                result[name] = block
            end = pos + 1
    return result

def normalize(text):
    text = re.sub(r'^(?:package|import) [^\n]+\n', '', text, flags=re.M)
    # 문자열 리터럴을 훼손하지 않고 주석만 제거한다.
    text = re.sub(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: '' if m[0].startswith(('//', '/*')) else m[0], text)
    for old, new in NAMES.items():
        text = re.sub(r'\b' + old + r'\b', new, text)
    # 조회 정책 전달만 제거하여 기존 JDBC 실행 종류와 읽기/예외 본문을 대조한다.
    text = re.sub(r'\bsql\(([A-Z_]+)\)', r'\1', text)
    text = re.sub(r'MaximoCiIdentity\.of\(Device42Entity\.(\w+), (source\.\w+\(\))\)',
                  r'"D42:\1:" + \2', text)
    text = text.replace('source.macAddress() == null ? null : source.macAddress().toUpperCase(Locale.ROOT)',
                        'source.macAddress()')  # ProjectionEquivalenceTest에서 이전 SQL UPPER와 대조.
    # 공유된 조회의 오류 문구에서만 종전 변형/대상 표현을 통합한다. 예외 타입/범위는 그대로 비교한다.
    text = text.replace('변환 변형', '변환 대상')
    return re.sub(r'\s+', '', text)

paths = git('ls-tree', '-r', '--name-only', REVISION).splitlines()
reviewed = {'DiscoveryInterfaceApplication.java', 'cli/JobRunner.java',
            'integration/d42maximo/ci/relation/CiRelationSource.java',
            'integration/d42maximo/ci/relation/CiRelationQuery.java',
            'integration/d42maximo/ci/selection/CiSourceFilter.java',
            'integration/d42maximo/ci/selection/FilesystemSelection.java'}
whole, methods, queries = 0, 0, 0
for path in paths:
    if not path.startswith(PREFIX) or not path.endswith('.java'):
        continue
    rel = path.removeprefix(PREFIX)
    if rel in reviewed:
        continue
    before = git('show', REVISION + ':' + path)
    dest = destination(path)
    assert dest.exists(), dest
    after = dest.read_text()
    if path.endswith('Query.java'):
        previous, current = members(before), members(after)
        for name, body in previous.items():
            if name == Path(path).stem:
                continue  # 명시적 정책 주입 생성자.
            assert name in current, (path, name)
            assert normalize(body) == normalize(current[name]), (path, name)
            methods += 1
        queries += 1
    elif path.endswith('CiRelationJob.java'):
        previous, current = members(before), members(after)
        for name in ('run', 'integrate'):
            actual = current[name].replace('source.source()', 'source')
            actual = actual.replace('writer.write(mapper.mapData(source, data))', 'writer.write(data)')
            actual = actual.replace('var data =', 'List<ActCiRelationUpsert> data =')
            assert normalize(previous[name]) == normalize(actual), (path, name)
            methods += 1
    else:
        assert normalize(before) == normalize(after), path
        whole += 1

def test_names(text):
    return set(re.findall(r'@(?:Test|ParameterizedTest)\b[\s\S]*?\bvoid\s+(\w+)\s*\(', text))

old_tests = {}
for path in paths:
    if path.startswith('src/test/java/') and path.endswith('.java'):
        old_tests[Path(path).name] = test_names(git('show', REVISION + ':' + path))
new_tests = {p.name: test_names(p.read_text()) for p in (ROOT / 'src/test/java').rglob('*.java')}
for file, names in old_tests.items():
    assert names <= new_tests.get(file, set()), (file, names - new_tests.get(file, set()))
print(f'PASS: {whole}개 전체 클래스, {queries}개 기존 Query의 {methods}개 메서드, 기존 테스트 이름 {sum(map(len, old_tests.values()))}개 보존')
print('별도 검증 대상: 타겟 선택/초기화, Source SQL 정책·투영, 관계 Query/Mapper, 의존 경계')

if '--artifact' in sys.argv:
    dependencies = subprocess.check_output(
        ['jdeps', '-verbose:class', '-filter:none', str(ROOT / 'build/classes/java/main')], text=True)
    graph, edges = {}, set()
    base = 'com.itmsg.device42.'
    for line in dependencies.splitlines():
        match = re.match(r'\s*(com\.itmsg\.device42\.[\w.$]+)\s+->\s+(com\.itmsg\.device42\.[\w.$]+)', line)
        if not match:
            continue
        owner, dependency = match.groups()
        edges.add((owner, dependency))
        for layer in ('runtime.', 'source.device42.', 'target.maximo.'):
            if owner.startswith(base + layer):
                assert dependency.startswith(base + layer), (owner, dependency)
        if owner.startswith(base + 'cli.'):
            assert dependency.startswith(base + 'runtime.'), (owner, dependency)
        source_pkg, target_pkg = owner.rsplit('.', 1)[0], dependency.rsplit('.', 1)[0]
        if source_pkg != target_pkg:
            graph.setdefault(source_pkg, set()).add(target_pkg)
    def visit(node, visiting, done):
        assert node not in visiting, ('package cycle', visiting, node)
        if node in done:
            return
        for dependency in graph.get(node, set()):
            visit(dependency, visiting | {node}, done)
        done.add(node)
    done = set()
    for node in graph:
        visit(node, set(), done)
    print(f'PASS: jdeps 내부 의존 {len(edges)}개, 금지 역참조/패키지 순환 없음')
    jars = [p for p in (ROOT / 'build/libs').glob('*.jar') if not p.name.endswith('-plain.jar')]
    assert len(jars) == 1, jars
    with zipfile.ZipFile(jars[0]) as jar:
        prefix = 'BOOT-INF/classes/com/itmsg/device42/'
        classes = [n.removeprefix(prefix) for n in jar.namelist() if n.startswith(prefix) and n.endswith('.class')]
        for name in classes:
            assert not name.startswith(('integration/', 'device42/', 'maximo/')), name
            assert not any(part in name for part in ('AlternativeTarget', 'BaselineSql', 'IntegrationTask')), name
        imports = [n for n in classes if n.endswith('Import.class')]
        queries_in_jar = [n for n in classes if n.endswith('Query.class')]
        assert len(imports) == 27, imports
        assert len(queries_in_jar) == 24, queries_in_jar
        print(f'PASS: JAR {len(classes)}개 클래스, Import {len(imports)}개, 공유 Query {len(queries_in_jar)}개, 옛/테스트 경로 없음')

if '--docs' in sys.argv:
    link_count = 0
    for path in (ROOT / 'docs').rglob('*.md'):
        for link in re.findall(r'\]\(([^)]+)\)', path.read_text()):
            target = link.split('#', 1)[0]
            if not target or re.match(r'(https?:|mailto:)', target) or re.search(r'\s', target):
                continue
            assert (path.parent / target).exists(), (path, target)
            link_count += 1

    def compact_sql(sql):
        return re.sub(r'\s+', '', re.sub(r'--[^\n]*', '', sql)).rstrip(';').lower()

    documented = []
    for path in (ROOT / 'docs/data-analysis/data-mapping').rglob('*.md'):
        for block in re.findall(r'```sql\s*(.*?)```', path.read_text(), re.S):
            documented.append(compact_sql(block))
    pages = list((ROOT / 'build/refactoring/current-sql').glob('*-page.sql'))
    assert len(pages) == 30, 'SourceSqlParityTest 실행 후 원천 조회 23개 + 관계 7개 SQL이 필요하다'
    for page in pages:
        sql = compact_sql(re.sub(r'LIMIT\s+\d+\s+OFFSET\s+\d+', 'LIMIT 1000 OFFSET 0', page.read_text()))
        # 한 SQL 블록에 관계 조회 두 개를 나란히 기록한 문서도 있다.
        assert any(sql in block.split(';') for block in documented), page
    print(f'PASS: 문서 로컬 링크 {link_count}개, 본체/관계 실효 PAGE SQL {len(pages)}개 대응')
