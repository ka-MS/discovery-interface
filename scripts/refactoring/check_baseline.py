"""One-off structural parity audit against the refactoring baseline; no DB access.

Run from any directory with Python 3.12+: python3 scripts/refactoring/check_baseline.py
This is a source-level audit, not proof that either SQL dialect runs on a live DB.
"""
from pathlib import Path
import re
import sys
import difflib
import io
import subprocess
import tarfile
import tempfile
import zipfile
from collections import Counter

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'src/main/java'
TEST = ROOT / 'src/test/java'
BASE = 'com.itmsg.device42'
REVISION = 'a9a5ea397761f11e5c4381da5512cfab25f4c05a'

def mask(s):
    return re.sub(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/',
                  lambda m: re.sub(r'[^\n]', ' ', m.group()), s)

def members(text):
    masked = mask(text)
    start = masked.index('{', masked.index('public class')) + 1
    depth = 1
    end = start
    result = {}
    for pos in range(start, len(masked)):
        c = masked[pos]
        if c == '{': depth += 1
        if c == '}': depth -= 1
        if depth == 0: break
        if (c == ';' and depth == 1) or (c == '}' and depth == 1):
            block = text[end:pos+1].strip('\n')
            clean = re.sub(r'@\w+(?:\([^)]*\))?\s*', '', mask(block).strip())
            header = clean.split('{')[0]
            if '(' in header and ('=' not in header or header.index('(') < header.index('=')):
                name = re.search(r'(\w+)\s*\(', header).group(1)
            else:
                name = re.search(r'(\w+)\s*(?:=|;)', clean).group(1)
            result[name] = block
            end = pos+1
    return result

temporary = tempfile.TemporaryDirectory(prefix='d42-structure-audit-')
archive = subprocess.check_output(['git', 'archive', REVISION], cwd=ROOT)
with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
    tar.extractall(temporary.name, filter='data')
BASELINE_ROOT = Path(temporary.name)

BASELINE=BASELINE_ROOT/'src/main/java'/Path(BASE.replace('.','/'))
CURRENT=MAIN/Path(BASE.replace('.','/'))
families={
 'asset': [('DeployedAsset','device','DeployedAsset'),('DpaComputer','computer','Computer'),('DpaCpu','cpu','Cpu'),('DpaDisk','disk','Disk'),('DpaLogicalDrive','logicaldrive','LogicalDrive'),('DpaMediaAdapter','mediaadapter','MediaAdapter'),('DpaNetAdapter','netadapter','NetAdapter'),('DpaNetDevice','netdevice','NetDevice'),('DpaNetPrinter','netprinter','NetPrinter'),('DpaOs','os','Os'),('DpaTcpIp','tcpip','TcpIp')],
 'ci':[(n+'Ci',n.lower(),n+'Ci') for n in ['Device','Os','Disk','Filesystem','Ip','DatabaseInstance']],
 'conversion':[('Dpam'+n,f,'Dpam'+n) for n,f in [('Manufacturer','manufacturer'),('ManuVariant','manufacturer'),('Os','os'),('OsVariant','os'),('Processor','processor'),('ProcVariant','processor'),('Adapter','adapter'),('AdptVariant','adapter')]],
 'software':[('TloamSoftware','catalog','TloamSoftware'),('DpaSoftware','installed','DpaSoftware')]}

def normalize(s):
    # Normalize only the reviewed structural transformations, never literal values.
    s=re.sub(r'try \(Connection connection = connectionFactory.openConnection\(\);[\s\S]*?ResultSet (\w+) = statement.executeQuery\(.*?\)\) \{','try {',s)
    s=re.sub(r'return doql\.(?:preparedQuery|query)\(.*?, \w+ -> \{','',s)
    s=re.sub(r'\}\);\s*\} catch \(SQLException','} catch (SQLException',s)
    s=re.sub(r'for \(long offset = 0; offset < totalCount; offset \+= (\w+)\) \{\s*int limit = \(int\) Math.min\(\1, totalCount - offset\);',r'for (var page : PageLoop.pages(totalCount, \1)) { long offset = page.offset(); int limit = page.limit();',s)
    s=re.sub(r'\b(?:query|mapper)\.(getTotalCount|getData|mapData)\(',r'\1(',s)
    s=s.replace('writer.write(', 'putData(').replace('void write(', 'void putData(')
    s=s.replace('SoftwareIdentity.buildUniqueId','buildUniqueId').replace('TloamSoftwareIntegrate.buildUniqueId','buildUniqueId')
    s=s.replace('FilesystemSelection.EXCLUDED_TYPES_SQL','EXCLUDED_TYPES_SQL').replace('FilesystemCiIntegrate.EXCLUDED_TYPES_SQL','EXCLUDED_TYPES_SQL')
    s=re.sub(r'\.classification\((classification|CiClassification\.\w+)\.classificationId\(\)\)',r'.classification(\1)',s)
    s=s.replace('@Override','')
    tokens=re.findall(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/|\w+|[^\s]',s)
    return '\n'.join(t for t in tokens if t not in ['public','private','protected'] and not t.startswith('//') and not t.startswith('/*'))

def check_execution_selectors(original, current):
    old=[]
    pattern=r'(PreparedStatement statement = connection.prepareStatement\((\w+)\)|Statement statement = connection.createStatement\(\));\s*ResultSet (\w+) = statement.executeQuery\((.*?)\)\) \{'
    for m in re.finditer(pattern,original):old.append(('preparedQuery' if m.group(2) else 'query',m.group(2) or m.group(4),m.group(3)))
    new=re.findall(r'return doql\.(preparedQuery|query)\((.*?), (\w+) -> \{',current)
    assert old==new,('Statement path or query expression changed',old,new)

matched=0;issues=[];flows=[]
for area,items in families.items():
    for old,feature,stem in items:
        original=members((BASELINE/'integration'/area/(old+'Integrate.java')).read_text())
        folder=CURRENT/'integration/d42maximo'/area/feature
        check_execution_selectors((BASELINE/'integration'/area/(old+'Integrate.java')).read_text(),(folder/(stem+'Query.java')).read_text())
        candidates=list(folder.glob(stem+'*.java'))
        if area!='ci':candidates.append(CURRENT/'maximo'/area/(old+'Writer.java'))
        else:candidates.append(CURRENT/'maximo/ci/ActCiWriter.java')
        if stem=='FilesystemCi':candidates.append(CURRENT/'integration/d42maximo/ci/selection/FilesystemSelection.java')
        if area=='software':candidates.append(CURRENT/'integration/d42maximo/software/mapping/SoftwareIdentity.java')
        dest={}
        for p in candidates:
            text=p.read_text().replace('public final class','public class')
            if 'public class' not in text:continue
            for n,block in members(text).items():dest.setdefault(n,[]).append((p,block))
        for name,block in original.items():
            if name in [old+'Integrate','log','connectionFactory','maximoJdbcTemplate','writer','specMapper']:continue
            target='write' if name=='putData' else name
            if area=='ci' and name=='putData':
                # Original CI putData is only a passthrough to the same unchanged writer.
                assert re.search(r'return writer.write\(data\);',block);matched+=1;continue
            if not any(normalize(block)==normalize(b) for p,b in dest.get(target,[])):
                issues.append((old,name))
                print('DIFFERENCE',old,name)
                if dest.get(target):
                    print('\n'.join(list(difflib.unified_diff(normalize(block).splitlines(),normalize(dest[target][0][1]).splitlines()))[:90]))
            else:matched+=1
        flows.append(old)

# The 28th execution path remains definition-driven, not one class per relation.
old_relation=(BASELINE/'integration/ci/relation/CiRelationJob.java').read_text()
query=(CURRENT/'integration/d42maximo/ci/relation/CiRelationQuery.java').read_text()
check_execution_selectors(old_relation,query)
relation_parts=members(query)|members((CURRENT/'integration/d42maximo/ci/relation/CiRelationJob.java').read_text())
for name in ['run','integrate','getTotalCount','getData','DEFAULT_BATCH_SIZE']:
    assert normalize(members(old_relation)[name])==normalize(relation_parts[name]),('relation',name)
    matched+=1

# Definitions/DTOs/spec rules/configuration are moved intact except the reviewed ID-list API.
preserved=0
for p in BASELINE.rglob('*.java'):
    if p.stem.endswith(('Integrate','IntegrationTask','IntegrationJob')) or p.stem in ['JobRunner','CiRelationJob']:continue
    old=p.read_text();old=old[old.index('{'):]
    candidates=list(CURRENT.rglob(p.name))
    equal=False
    for candidate in candidates:
        new=candidate.read_text();new=new[new.index('{'):]
        if p.stem=='CiClassification':
            new=re.sub(r'    public static java.util.List<String> ids\(\) \{[^}]+\}', '', new)
        if p.stem=='CiDefinitionLoader':
            new=new.replace('load(Collection<String> classificationIds)','load()').replace('classificationIds.stream().distinct().toList()', 'Arrays.stream(CiClassification.values()).map(CiClassification::classificationId).distinct().toList()')
        if p.stem=='CiDefinitionCache':
            new=new.replace('classification(String classificationId)','classification(CiClassification classification)').replace('classifications.get(classificationId)','classifications.get(classification.classificationId())')
        if normalize(old)==normalize(new):equal=True;break
    if not equal:issues.append((p.name,'class body'));print('DIFFERENCE',p.name,'class body')
    else:preserved+=1

# No original test case may disappear during fixture/package migration.
def tests(root):
    found=[]
    for p in root.rglob('*.java'):
        found+=re.findall(r'@Test\s+(?:public\s+)?void\s+(\w+)\(',p.read_text())
    return found
before=tests(BASELINE_ROOT/'src/test/java')
after=tests(TEST)
missing=Counter(before)-Counter(after)
print('FLOWS',len(flows)+1,'MATCHED_MEMBERS',matched,'PRESERVED_CLASSES',preserved,'DIFFERENCES',len(issues),'BASELINE_TESTS',len(before),'MISSING_TESTS',missing)
temporary.cleanup()

if '--artifact' in sys.argv:
    # Compiled dependencies also catch fully qualified references and lambda captures.
    output=subprocess.check_output(['jdeps','--ignore-missing-deps','-verbose:class','-filter:none',
                                    str(ROOT/'build/classes/java/main')],text=True)
    graph={};edges=0
    for owner,dependency in re.findall(r'^\s+(com\.itmsg\.device42\.[\w.$]+)\s+->\s+(com\.itmsg\.device42\.[\w.$]+)',output,re.M):
        a=owner.rsplit('.',1)[0];b=dependency.rsplit('.',1)[0];edges+=1
        for layer in ['runtime','device42','maximo']:
            if a.startswith(BASE+'.'+layer):assert b.startswith(BASE+'.'+layer),(owner,dependency)
        if a.startswith(BASE+'.cli'):assert b.startswith(BASE+'.runtime'),(owner,dependency)
        if a!=b:graph.setdefault(a,set()).add(b)
    def visit(pkg,stack,done):
        assert pkg not in stack,('package cycle',stack,pkg)
        if pkg in done:return
        done.add(pkg)
        for dependency in graph.get(pkg,[]):visit(dependency,stack+[pkg],done)
    for pkg in graph:visit(pkg,[],set())
    jars=list((ROOT/'build/libs').glob('*.jar'))
    jars=[p for p in jars if not p.name.endswith('-plain.jar')]
    assert len(jars)==1,jars
    with zipfile.ZipFile(jars[0]) as jar:
        prefix='BOOT-INF/classes/'+BASE.replace('.','/')+'/'
        classes=[n[len(prefix):] for n in jar.namelist() if n.startswith(prefix) and n.endswith('.class')]
        assert classes,'No application classes in bootJar'
        for name in classes:
            assert not name.startswith(('config/','dto/','enums/')),(name,'legacy package')
            assert not (name.startswith('integration/') and not name.startswith('integration/d42maximo/')),name
            assert not name.endswith(('Integrate.class','IntegrationTask.class')),name
        assert sum(n.endswith('Import.class') for n in classes)==27
        assert sum(n.endswith('Query.class') for n in classes)==28
    print('ARTIFACT_INTERNAL_EDGES',edges,'PACKAGES',len(graph),'PACKAGED_CLASSES',len(classes),'BOUNDARIES_AND_CYCLES_OK')
sys.exit(bool(issues or missing))
