"""기준 구조의 패키지/참조만 기계적으로 이전하는 일회성 도구. 동작 변경은 별도 패치한다."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'src/main/java'
BASE = 'com.itmsg.device42.'

def destination(name):
    if name.startswith(BASE + 'device42.'):
        return name.replace(BASE + 'device42.', BASE + 'source.device42.', 1)
    if name.startswith(BASE + 'maximo.'):
        return name.replace(BASE + 'maximo.', BASE + 'target.maximo.', 1)
    if name.startswith(BASE + 'integration.d42maximo.'):
        simple = name.rsplit('.', 1)[1]
        if (simple.endswith('Query') or simple.endswith('Source')) and simple != 'CiRelationSource':
            return name.replace(BASE + 'integration.d42maximo.', BASE + 'source.device42.', 1)
        return name.replace(BASE + 'integration.d42maximo.', BASE + 'pipeline.d42maximo.', 1)
    return name

mapping = {}
for path in MAIN.rglob('*.java'):
    old = str(path.relative_to(MAIN).with_suffix('')).replace('/', '.')
    mapping[old] = destination(old)
if not any(old != new for old, new in mapping.items()):
    raise SystemExit('이미 이전된 작업 트리: 재실행하지 않음')

for tree in ('src/main/java', 'src/test/java'):
    root = ROOT / tree
    for path in list(root.rglob('*.java')):
        text = path.read_text()
        old_pkg = re.search(r'^package ([^;]+);', text, re.M)[1]
        old_class = old_pkg + '.' + path.stem
        new_class = mapping.get(old_class, destination(old_class))
        new_pkg = new_class.rsplit('.', 1)[0]
        additional = []
        for old, new in mapping.items():
            pkg, simple = old.rsplit('.', 1)
            if pkg == old_pkg and new.rsplit('.', 1)[0] != new_pkg and re.search(r'\b' + simple + r'\b', text):
                if 'import ' + old + ';' not in text:
                    additional.append('import ' + new + ';')
        for old in sorted(mapping, key=len, reverse=True):
            text = text.replace(old, mapping[old])
        text = re.sub(r'^package [^;]+;', 'package ' + new_pkg + ';', text, count=1, flags=re.M)
        if additional:
            text = text.replace('package ' + new_pkg + ';', 'package ' + new_pkg + ';\n\n' + '\n'.join(sorted(additional)), 1)
        dest = root / (new_class.replace('.', '/') + '.java')
        if dest != path:
            assert not dest.exists(), dest
            dest.parent.mkdir(parents=True, exist_ok=True)
            path.rename(dest)
        if dest.read_text() != text:
            dest.write_text(text)

# 매핑 문서의 Java 링크만 업데이트한다. 이전 완료 기록은 변경하지 않는다.
for path in (ROOT / 'docs/data-analysis/data-mapping').rglob('*.md'):
    text = path.read_text()
    for old, new in mapping.items():
        text = text.replace(old.replace('.', '/') + '.java', new.replace('.', '/') + '.java')
    if path.read_text() != text:
        path.write_text(text)
print('이전 클래스 수:', sum(old != new for old, new in mapping.items()))
