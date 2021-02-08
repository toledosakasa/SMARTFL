import os
from typing import Any, Dict, List, Set
alld4jprojs = ["Chart", "Cli", "Closure", "Codec", "Collections", "Compress", "Csv", "Gson",
               "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Lang", "Math", "Mockito", "Time"]


def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')


def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')


def getd4jprojinfo():
    for proj in alld4jprojs:
        getinstclassinfo(proj)


def getinstclassinfo(proj: str):
    cmdline = f"defects4j query -p {proj} -q \"bug.id,classes.relevant.src,classes.relevant.test,tests.trigger,tests.relevant\"  -o ./d4j_resources/{proj}.csv"
    os.system(cmdline)


def getmetainfo(proj: str, id: str) -> Dict[str, str]:
    ret = {}
    # caching
    print('Checking for cache...')
    cachepath = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}{id}.log')
    if os.path.exists(cachepath):
        print('cache found')
        lines = utf8open(cachepath).readlines()
        for line in lines:
            line = line.strip()
            splits = line.split('=')
            ret[splits[0]] = splits[1]
        return ret

    print('Cache not found. Generating metadata.')
    # if not cached, generate metadata
    workdir = os.path.abspath(
        f'./tmp_checkout/{proj}{id}')
    if not os.path.exists(workdir):
        checkout(proj, id)
    fields = ['tests.all', 'classes.relevant',
              'tests.trigger', 'tests.relevant']

    print('Exporting metadata')
    for field in fields:
        tmp_logfieldfile = f'{workdir}/{field}.log'
        os.system(
            f'defects4j export -p {field} -w {workdir} -o {tmp_logfieldfile}')
        ret[field] = utf8open(tmp_logfieldfile).read().replace('\n', ';')

    print('Instrumenting all test methods')
    cmdline_getallmethods = f'mvn compile -q && mvn exec:java "-Dexec.mainClass=ppfl.defects4j.Instrumenter" "-Dexec.args={proj} {id}"'
    os.system(cmdline_getallmethods)
    allmethodslog = f'./d4j_resources/metadata_cached/{proj}{id}.alltests.log'
    ret['methods.test.all'] = utf8open(
        allmethodslog).read().replace('\n', ';')
    # write to cache
    print('Writing to cache...')
    cachedir = os.path.abspath('./d4j_resources/metadata_cached')
    if not os.path.exists(cachedir):
        os.mkdir(cachedir)
    cachefile = utf8open_w(cachepath)
    for (k, v) in ret.items():
        cachefile.write(f'{k}={v}\n')
    # cleanup
    print('Removing temporary file')
    # os.system(f'rm -rf {workdir}')
    os.system(f'rm {allmethodslog}')
    return ret


def parseprofile(line: str, trigger_tests: Set[str], testmethods: Set[str]):
    line = line[3:]
    sp = line.split('::')
    class_name = sp[0].strip()
    method_name = sp[1].strip()
    is_trigger = line in trigger_tests
    is_test = (class_name, method_name) in testmethods
    return class_name, method_name, is_trigger, is_test


def resolve_profile(profile: List[str], classes_relevant: List[str], trigger_tests: List[str], testmethods: List[str]) -> List[str]:
    print(len(profile))
    ret = []
    pass_coverage = {}
    fail_coverage = set()
    curclass = ''
    curmethod = ''
    curtrigger = False
    trigger_tests_set = set(trigger_tests)
    testmethods_set = set()
    for testmethod in testmethods:
        testmethod = testmethod.strip()
        if testmethod == '':
            continue
        sp = testmethod.split('::')
        methods = sp[1].split(',')
        for method in methods:
            testmethods_set.add((sp[0], method.strip()))
    for line in profile:
        if line.strip() == '':
            continue
        class_name, method_name, is_trigger, is_test = parseprofile(
            line, trigger_tests_set, testmethods_set)
        if is_test:
            curclass, curmethod = class_name, method_name
            curtrigger = is_trigger
            continue
        if curtrigger:
            fail_coverage.add((class_name, method_name))
    print('fail_coverage:')
    print(fail_coverage)
    input()
    for line in profile:
        if line.strip() == '':
            continue
        class_name, method_name, is_trigger, is_test = parseprofile(
            line, trigger_tests_set, testmethods_set)
        if is_test:
            curclass, curmethod = class_name, method_name
            curtrigger = is_trigger
            continue
        if curtrigger:
            continue
        if (curclass, curmethod) in pass_coverage:
            pass_coverage[(curclass, curmethod)].add(
                (class_name, method_name))
        else:
            pass_coverage[(curclass, curmethod)] = set()
    for (class_name, method_name), coverage in pass_coverage.items():
        if len(coverage & fail_coverage) > 0:
            ret.append((class_name, method_name))
    print(fail_coverage)
    return sorted(ret)


def getd4jcmdline(proj: str, id: str) -> List[str]:
    print('getting metainfo')
    metadata = getmetainfo(proj, id)
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    tests_relevant = metadata['tests.relevant'].strip()
    classes_relevant = metadata['classes.relevant'].strip()
    instclasses = classes_relevant + ';' + tests_relevant
    instclasses = instclasses.replace(";", ":")
    testmethods = metadata['methods.test.all'].split(';')
    relevant_classes = tests_relevant.split(';')
    trigger_tests = metadata['tests.trigger'].strip()
    d4jdatafile = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}{id}.log')
    checkoutdir = f'tmp_checkout/{proj}{id}'

    profile = checkoutdir + '/trace/logs/mytrace/profile.log'
    if not os.path.exists(profile):
        cdcmd = f'cd {checkoutdir} && '
        simplelogcmd = f"defects4j test -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=simplelog=true,d4jdatafile={d4jdatafile}\""
        os.system(cdcmd + simplelogcmd)
    relevant_testmethods = resolve_profile(
        utf8open(profile).readlines(), classes_relevant.split(';'), trigger_tests, testmethods)
    print(relevant_testmethods)
    input()

    ret = []
    for testmethod in testmethods:
        if testmethod.strip() == '':
            continue
        testclassname = testmethod.split('::')[0]
        if testclassname in relevant_classes:
            app = f"defects4j test -t {testmethod} -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=instrumentingclass={instclasses},d4jdatafile={d4jdatafile}\""
            ret.append(app)
    return ret


def checkout(proj: str, id: str):
    checkoutpath = './tmp_checkout'
    if not(os.path.exists(checkoutpath)):
        os.makedirs(checkoutpath)
    os.system(
        f'defects4j checkout -p {proj} -v {id}b -w ./tmp_checkout/{proj}{id}')


def clearcache():
    cachepath = os.path.abspath('./d4j_resources/metadata_cached/')
    os.system('rm -rf '+cachepath)
