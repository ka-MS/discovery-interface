"""원천 조회의 타겟 테이블 이름을 제거하는 기계적 이름/참조 이전. 동결 기준 SQL은 제외한다."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
NAMES = {
    'DpamManufacturerQuery': 'ManufacturerNamesQuery',
    'DpamManuVariantQuery': 'ManufacturerNamesQuery',
    'DpamOsQuery': 'OperatingSystemNamesQuery',
    'DpamOsVariantQuery': 'OperatingSystemNamesQuery',
    'DpamProcessorQuery': 'ProcessorModelsQuery',
    'DpamProcVariantQuery': 'ProcessorModelsQuery',
    'DpamAdapterQuery': 'AdapterModelsQuery',
    'DpamAdptVariantQuery': 'AdapterModelsQuery',
    'TloamSoftwareQuery': 'SoftwareCatalogQuery',
    'DpaSoftwareQuery': 'InstalledSoftwareQuery',
    'DeployedAssetQuery': 'DeviceQuery',
}
# 변형 Query는 패치로 먼저 제거하고, 같은 원천 Query를 두 연계 Import에 주입한다.
for folder in ('src', 'docs/data-analysis/data-mapping'):
    for path in list((ROOT / folder).rglob('*')):
        if path.suffix not in ('.java', '.md') or path.name in ('BaselineSql.java', 'SourceSqlParityTest.java', 'SelectionEquivalenceTest.java'):
            continue
        text = path.read_text()
        for old, new in NAMES.items():
            text = re.sub(r'\b' + old + r'\b', new, text)
        # 합쳐진 같은 원천 클래스의 중복 import만 제거한다.
        seen = set()
        lines = []
        for line in text.splitlines(keepends=True):
            if line.startswith('import '):
                if line in seen:
                    continue
                seen.add(line)
            lines.append(line)
        text = ''.join(lines)
        dest = path.with_name(NAMES.get(path.stem, path.stem) + path.suffix)
        if dest != path:
            assert not dest.exists(), dest
            path.rename(dest)
        if dest.read_text() != text:
            dest.write_text(text)
