import os
from pylib.countmap import CountMap
import time
from func_timeout import func_set_timeout
import func_timeout
from typing import Any, Dict, List, Set
import json
from multiprocessing import Pool, TimeoutError
import re
from os.path import join, getsize

alld4jprojs = ["Chart", "Cli", "Closure", "Codec", "Collections", "Compress", "Csv", "Gson",
               "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Lang", "Math", "Mockito", "Time"]
project_bug_nums = {"Lang": 65, "Math": 106,
                    "Time": 27, "Closure": 176, "Chart": 26}
use_simple_filter = False


def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')


def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')

def utf8open_a(filename):
    return open(filename, 'a+', encoding='utf-8', errors='ignore')


checkoutbase = utf8open('checkout.config').readline().strip()
projectbase = os.path.abspath(".")

def getdirsize(dir:str):
    size = 0
    for root, dirs, files in os.walk(dir):
        size += sum([getsize(join(root, name)) for name in files])
    return size

def getd4jprojinfo():
    for proj in alld4jprojs:
        getinstclassinfo(proj)


def getinstclassinfo(proj: str):
    cmdline = f"defects4j query -p {proj} -q \"bug.id,classes.relevant.src,classes.relevant.test,tests.trigger,tests.relevant\"  -o ./d4j_resources/{proj}.csv"
    cmdline += f' > trace/runtimelog/{proj}query.log'
    os.system(cmdline)


def getmetainfo(proj: str, id: str, debug=True) -> Dict[str, str]:
    ret = {}
    # caching
    if debug:
        print('Checking for metainfo...', end='')
    cachedir = os.path.abspath(f'./d4j_resources/metadata_cached/{proj}')
    if not os.path.exists(cachedir):
        os.mkdir(cachedir)
    cachepath = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.log')
    if os.path.exists(cachepath):
        if debug:
            print('found')
        lines = utf8open(cachepath).readlines()
        for line in lines:
            line = line.strip()
            splits = line.split('=')
            ret[splits[0]] = splits[1]
        return ret

    if debug:
        print('not found. Generating metadata.')
    # if not cached, generate metadata
    workdir = os.path.abspath(
        f'{checkoutbase}/{proj}/{id}')
    checkout(proj, id)
    fields = ['tests.all', 'classes.relevant',
              'tests.trigger', 'tests.relevant']

    if debug:
        print('Exporting metadata')
    for field in fields:
        tmp_logfieldfile = f'{workdir}/{field}.log'
        cmdline = f'defects4j export -p {field} -w {workdir} -o {tmp_logfieldfile}'
        cmdline += f' >/dev/null 2>&1'
        # cmdline += f' > trace/runtimelog/{proj}{id}d4j.export.log 2>&1'
        os.system(cmdline)
        ret[field] = utf8open(tmp_logfieldfile).read().replace('\n', ';')

    if debug:
        print('Instrumenting all test methods')
    cmdline_getallmethods = f'mvn compile -q && mvn exec:java "-Dexec.mainClass=ppfl.defects4j.Instrumenter" "-Dexec.args={proj} {id}"'
    cmdline_getallmethods += f' > trace/runtimelog/{proj}{id}.instrumenter.log 2>&1'
    os.system(cmdline_getallmethods)
    allmethodslog = f'./d4j_resources/metadata_cached/{proj}/{id}.alltests.log'
    ret['methods.test.all'] = utf8open(
        allmethodslog).read().replace('\n', ';')
    # write to cache
    if debug:
        print('Writing to cache')
    cachefile = utf8open_w(cachepath)
    for (k, v) in ret.items():
        cachefile.write(f'{k}={v}\n')
    # cleanup
    if debug:
        print('Removing temporary file')
    # os.system(f'rm -rf {workdir}')
    os.system(f'rm {allmethodslog}')
    return ret


def parseprofile(line: str, trigger_tests: Set[str], testmethods: Set[str]):
    line = line[3:]
    sp = line.split('::')
    class_name = sp[0].strip()
    method_name = sp[1].strip()
    is_trigger = (class_name, method_name) in trigger_tests
    is_test = (class_name, method_name) in testmethods
    return class_name, method_name, is_trigger, is_test


def resolve_profile(profile: List[str], classes_relevant: List[str], trigger_tests: List[str], testmethods: List[str], debug=True) -> List[str]:
    if debug:
        print(f'parsing profile, length:{len(profile)}')
        print('trigger tests are: ', trigger_tests)
    relevant = []
    relevant_cnt = CountMap()

    # currelevant = False
    trigger_tests_set, trigger_tests_map = parse_trigger_tests(trigger_tests)
    testmethods_set = parse_test_methods(testmethods)
    fail_coverage = get_fail_coverage(
        profile, trigger_tests_set, testmethods_set)
    curclass = ''
    curmethod = ''
    for line in profile:
        if line.strip() == '':
            continue
        class_name, method_name, is_trigger, is_test = parseprofile(
            line, trigger_tests_set, testmethods_set)
        if is_test:
            curclass, curmethod = class_name, method_name
            # currelevant = False
            continue
        # if currelevant:
        #     continue
        # relevant
        if curclass == '' or curmethod == '':
            continue
        if (class_name, method_name) in fail_coverage:
            # FIXME weird use-before-def bug for curclass,curmethod at Math-2.
            curtest = (curclass, curmethod)
            relevant.append(curtest)
            relevant_cnt.add(curclass, curmethod)
            # currelevant = True
    # TODO use relevant_cnt for filtering
    # relevant = list(set(relevant))
    # return sorted(relevant)

    if use_simple_filter:
        return relevant_cnt.method_filter_simple(trigger_tests_map, testmethods_set)
    else:
        return relevant_cnt.method_filter(trigger_tests_map, testmethods_set)


def get_fail_coverage(profile, trigger_tests_set, testmethods_set):
    fail_coverage = set()
    curtrigger = False
    for line in profile:
        if line.strip() == '':
            continue
        class_name, method_name, is_trigger, is_test = parseprofile(
            line, trigger_tests_set, testmethods_set)
        if is_test:
            # curclass, curmethod = class_name, method_name
            curtrigger = is_trigger
            continue
        if curtrigger:
            fail_coverage.add((class_name, method_name))
            # print(fail_coverage)
    return fail_coverage


def parse_test_methods(testmethods):
    testmethods_set = set()
    for testmethod in testmethods:
        testmethod = testmethod.strip()
        if testmethod == '':
            continue
        sp = testmethod.split('::')
        methods = sp[1].split(',')
        # print(sp[0], sp[1])
        for method in methods:
            method = method.strip()
            if method != '':
                testmethods_set.add((sp[0], method))
    return testmethods_set


def parse_trigger_tests(trigger_tests):
    trigger_tests_set = set()
    trigger_tests_map = {}
    for trigger_test in trigger_tests:
        trigger_test = trigger_test.strip()
        if trigger_test == '':
            continue
        sp = trigger_test.split('::')
        classname = sp[0].strip()
        methodname = sp[1].strip()
        trigger_tests_set.add((classname, methodname))
        if classname in trigger_tests_map:
            if not methodname in trigger_tests_map[classname]:
                trigger_tests_map[classname].append(methodname)
        else:
            trigger_tests_map[classname] = [methodname]
    return trigger_tests_set, trigger_tests_map


def getd4jtestprofile(metadata: Dict[str, str], proj: str, id: str, debug=True):
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    classes_relevant = metadata['classes.relevant'].strip()
    testmethods = metadata['methods.test.all'].split(';')
    trigger_tests = metadata['tests.trigger'].strip()
    d4jdatafile = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.log')
    checkoutdir = f'{checkoutbase}/{proj}/{id}'

    profile_result = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.profile.log')
    if debug:
        print('checking profiling result...', end='')
    if(os.path.exists(profile_result)):
        if debug:
            print('found')
        tmpstr = utf8open(profile_result).read()
        return json.loads(tmpstr)

    profile = checkoutdir + '/trace/logs/mytrace/profile.log'
    if debug:
        print('not found')
        print('checking profile...', end='')
    if not os.path.exists(profile):
        if debug:
            print('not found. generating...')
        cdcmd = f'cd {checkoutdir} && '
        simplelogcmd = f"defects4j test -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=simplelog=true,d4jdatafile={d4jdatafile},project={proj}\""
        simplelogcmd += f' > {projectbase}/trace/runtimelog/{proj}{id}.profile.log 2>&1'
        os.system(cdcmd + simplelogcmd)
    else:
        if debug:
            print('found')
    relevant_testmethods = resolve_profile(
        utf8open(profile).readlines(), classes_relevant.split(';'), trigger_tests.split(';'), testmethods, debug)
    # print(relevant_testmethods)
    if debug:
        print('writing profiling result...')
    json.dump(relevant_testmethods, utf8open_w(profile_result))
    return relevant_testmethods


def getd4jcmdline(proj: str, id: str, debug=True) -> List[str]:
    metadata = getmetainfo(proj, id, debug)
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    classes_relevant = metadata['classes.relevant'].strip()

    relevant_testmethods = getd4jtestprofile(metadata, proj, id, debug)

    reltest_dict = {}  # {classname : [methodnames]}
    for (cname, mname) in relevant_testmethods:
        if cname in reltest_dict:
            reltest_dict[cname].append(mname)
        else:
            reltest_dict[cname] = [mname]

    relevant_testclass_number = len(reltest_dict)
    relevant_method_number = len(relevant_testmethods)

    instclasses = classes_relevant + ';' + ';'.join(reltest_dict.keys())

    if debug:
        print('writing relevant insts')
    instclasses_cache = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}/{id}.inst.log')
    utf8open_w(instclasses_cache).write(instclasses)

    instclasses = instclasses.replace(";", ":")
    print(f'{proj}{id} relevant tests:{relevant_testclass_number} classes, {relevant_method_number} methods')
    # print(reltest_dict)
    # input()

    ret = []
    # for testclass_rel in reltest_dict:
    #     app = f"defects4j test -t {testclass_rel}::{','.join(reltest_dict[testclass_rel])} -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=instrumentingclass={instclasses}\""
    #     app += ' > /dev/null'
    #     ret.append(app)
    for testclass_rel in reltest_dict:
        for testmethod_rel in reltest_dict[testclass_rel]:
            app = f"defects4j test -t {testclass_rel}::{testmethod_rel} -a \"-Djvmargs=-noverify -Djvmargs=-javaagent:{jarpath}=instrumentingclass={instclasses},logfile={testclass_rel}.{testmethod_rel}.log,project={proj}\""
            app += '>/dev/null 2>&1'
            ret.append(app)
    return ret


def checkout(proj: str, id: str):
    checkoutpath = f'{checkoutbase}/{proj}/{id}'
    if not(os.path.exists(checkoutpath)):
        os.makedirs(checkoutpath)
    if not(os.path.exists(checkoutpath + '/.defects4j.config')):
        print("in checkout")
        os.system(
            f'defects4j checkout -p {proj} -v {id}b -w {checkoutbase}/{proj}/{id} >/dev/null 2>&1')


def deletecheckout(proj: str, id: str):
    checkoutpath = f'{checkoutbase}/{proj}/{id}'
    if (os.path.exists(checkoutpath)):
        os.system(f'rm -rf {checkoutbase}/{proj}/{id}')


def cleanupcheckout(proj: str, id: str):
    checkoutpath = f'{checkoutbase}/{proj}/{id}'
    if (os.path.exists(checkoutpath)):
        os.system(f'rm -rf {checkoutpath}/trace/logs/mytrace/')
        os.system(f'rm -rf {checkoutpath}/trace/logs/run/')
        os.system(f'rm -rf {checkoutpath}/trace/classcache/')


def clearcache(proj: str, id: str):
    cachepath = f'./d4j_resources/metadata_cached/{proj}/{id}.*'
    os.system('rm '+cachepath)


@func_set_timeout(1200)
def rund4j(proj: str, id: str, debug=True):
    cleanupcheckout(proj,id)
    banlist = ['','Lang2','Time21']
    if((proj+id).strip() in banlist):
        return
    time_start = time.time()
    checkout(proj, id)
    cmdlines = getd4jcmdline(proj, id, debug)
    checkoutdir = f'{checkoutbase}/{proj}/{id}'
    # cleanup previous log
    previouslog = f'{checkoutdir}/trace/logs/mytrace/all.log'
    if os.path.exists(previouslog):
        # print('removing previous trace logs.')
        os.system(f'rm {checkoutdir}/trace/logs/mytrace/all.log')
    cdcmd = f'cd {checkoutdir} && '
    cmdlines = [cdcmd + cmdline for cmdline in cmdlines]
    with Pool(processes=1) as pool:
        pool.map(os.system, cmdlines[0:1])
        pool.close()
        pool.join()
    # os.system(cmdlines[0])
    processesnum = 16
    if proj == 'Time':
        processesnum = 1
    with Pool(processes=processesnum) as pool:
        pool.map(os.system, cmdlines[1:])
        pool.close()
        pool.join()
    # for cmdline in cmdlines:
    #     testclassname = cmdline.split('::')[0].split(' ')[-1]
    #     print('testing', testclassname)
    #     # input()
    #     os.system(cdcmd + cmdline)
    time_end = time.time()
    if debug:
        print('d4j tracing complete after', time_end-time_start, 'sec')
    utf8open_a(f'./tracetime/tracetime_{proj}.log').write(f'{id}:{time_end-time_start}\n')

def rerun(proj: str, id: str):
    clearcache(proj, id)
    #cleanupcheckout(proj, id)
    os.system('mvn package -DskipTests')
    rund4j(proj, id)


def parse(proj: str, id: str, debug=True):
    path = f"{checkoutbase}/{proj}/{id}/trace/logs/run"
    size = getdirsize(path)/(1024*1024)
    # if(size>3000):
    #     return
    cmdline = f'mvn compile -q && mvn exec:java "-Dexec.mainClass=ppfl.defects4j.GraphBuilder" "-Dexec.args={proj} {id}"'
    if(not debug):
        cmdline += '>/dev/null 2>&1'
    os.system(cmdline)


def parseproj(proj: str, debug=True):
    time_start = time.time()
    os.system('mvn compile')
    # cmdlines = [f'mvn exec:java "-Dexec.mainClass=ppfl.defects4j.GraphBuilder" "-Dexec.args={proj} {id}"' for id in range(
    #     1, project_bug_nums[proj]+1)]
    cmdlines = []
    banlist = []
    banlist = ['Lang2', 'Lang8' ,'Time21','Math7','Math10','Math13','Math14','Math15','Math16','Math17','Math39','Math44','Math54','Math59','Math64','Math65','Math68','Math71','Math74','Math78','Math100','Time25', 'Chart15']
    if proj == 'MathandTime':
        proj = 'Math'
        for id in range(1, project_bug_nums[proj]+1):
            if((proj+str(id)).strip() in banlist):
                continue
            cmdlines.append(f'mvn exec:java "-Dexec.mainClass=ppfl.defects4j.GraphBuilder" "-Dexec.args={proj} {id}"')
        proj = 'Time'
        for id in range(1, project_bug_nums[proj]+1):
            if((proj+str(id)).strip() in banlist):
                continue
            cmdlines.append(f'mvn exec:java "-Dexec.mainClass=ppfl.defects4j.GraphBuilder" "-Dexec.args={proj} {id}"')
    else:
        for id in range(1, project_bug_nums[proj]+1):
            if((proj+str(id)).strip() in banlist):
                continue
            cmdlines.append(f'mvn exec:java "-Dexec.mainClass=ppfl.defects4j.GraphBuilder" "-Dexec.args={proj} {id}"')
    if(not debug):
        for cmdline in cmdlines:
            cmdline = cmdline + '>/dev/null 2>&1'
    with Pool(processes=16) as pool:
        pool.map(os.system, cmdlines)
        pool.close()
        pool.join()
    time_end = time.time()
    totaltime = time_end-time_start
    print(f'total time: {totaltime/60}min')
    evalproj(proj)


@func_set_timeout(3600)
def fl(proj: str, id: str, debug=True):
    banlist = ['Lang2','Time21']
    if((proj+id).strip() in banlist):
        return
    #cleanupcheckout(proj, id)
    clearcache(proj, id)
    rund4j(proj, id, debug)
    parse(proj, id, debug)


def fl_wrap(proj: str, id: str):
    print(f'running {proj}{id}')
    try:
        fl(proj, id, False)
    # except func_timeout.exceptions.FunctionTimedOut:
    #     print(f'timeout at {proj}-{id}')
    except:
        print(f'{proj}{id} failed.')
    cleanupcheckout(proj, id)
    eval(proj, id)

def traceproj(proj:str,id: str, doBuild = True):
    if doBuild:
        os.system('mvn package -DskipTests')
    if(id == 'all'):
        time_start = time.time()
        # cmdlines = [(proj, str(i))for i in range(1, project_bug_nums[proj]+1)]
        cmdlines = [f'python3 s.py rund4j {proj} {i} -nb' for i in range(
            1, project_bug_nums[proj]+1)]
        # print(cmdlines)
        processesnum = 1
        with Pool(processes=processesnum) as pool:
            pool.map(os.system, cmdlines)
            pool.close()
            pool.join()
        time_end = time.time()
        totaltime = time_end-time_start
        print(f'total time: {totaltime/60}min')
    else:
        rund4j(proj,id)

def testproj(proj: str):
    time_start = time.time()
    os.system('mvn package -DskipTests')
    # cmdlines = [(proj, str(i))for i in range(1, project_bug_nums[proj]+1)]
    cmdlines = [f'python3 s.py fl {proj} {i}' for i in range(
        1, project_bug_nums[proj]+1)]
    # print(cmdlines)
    if use_simple_filter:
        processesnum = 64
    else:
        processesnum = 1
    with Pool(processes=processesnum) as pool:
        pool.map(os.system, cmdlines)
        pool.close()
        pool.join()

    # for i in range(1, d4j.project_bug_nums[name]+1):
    #     fl_wrap(name, i)
    time_end = time.time()
    totaltime = time_end-time_start
    print(f'total time: {totaltime/60}min')
    evalproj(proj)


def testprojw(proj: str):
    time_start = time.time()
    os.system('mvn package -DskipTests')
    # cmdlines = [(proj, str(i))for i in range(1, project_bug_nums[proj]+1)]
    cmdlines = [f'python3 s.py flw {proj} {i}' for i in range(
        1, project_bug_nums[proj]+1)]
    # print(cmdlines)
    with Pool(processes=16) as pool:
        pool.map(os.system, cmdlines)
        pool.close()
        pool.join()

    # for i in range(1, d4j.project_bug_nums[name]+1):
    #     fl_wrap(name, i)
    time_end = time.time()
    totaltime = time_end-time_start
    print(f'total time: {totaltime/60}min')
    evalproj(proj)


def evalproj_method(proj: str):
    no_oracle = 0
    no_result = 0
    not_listed = 0
    top = []
    for i in range(11):
        top.append(0)
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = eval_method(proj, str(i))
        if(result > 0 and result <= 10):
            for j in range(result, 11):
                top[j] += 1
        if result == -3:
            no_result += 1
    print(f'top1={top[1]},top3={top[3]},top5={top[5]},top10={top[10]},failed={no_result}')


def zevalproj(proj: str):
    no_oracle = 0
    no_result = 0
    not_listed = 0
    crashed = 0
    top = []
    for i in range(11):
        top.append(0)
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = zeval(proj, str(i))
        if(result > 0 and result <= 10):
            for j in range(result, 11):
                top[j] += 1
        if result == -3:
            no_result += 1
        if result == -2:
            crashed += 1
    print(
        f'top1={top[1]},top3={top[3]},top5={top[5]},top10={top[10]},failed={no_result+crashed}')


def evalproj(proj: str):
    no_oracle = 0
    no_result = 0
    not_listed = 0
    crashed = 0
    top = []
    for i in range(11):
        top.append(0)
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = eval(proj, str(i))
        if(result > 0 and result <= 10):
            for j in range(result, 11):
                top[j] += 1
        if result == -3:
            no_result += 1
        if result == -2:
            crashed += 1
    print(
        f'top1={top[1]},top3={top[3]},top5={top[5]},top10={top[10]},failed={no_result+crashed}')

def pevalproj(proj: str):
    no_oracle = 0
    no_result = 0
    not_listed = 0
    crashed = 0
    top = []
    for i in range(11):
        top.append(0)
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = eval(proj, str(i))
        if(result > 0 and result <= 10):
            for j in range(result, 11):
                top[j] += 1
        if result == -3:
            no_result += 1
        if result == -2:
            crashed += 1
    score = top[1]*10+top[3]*7+top[5]*5+top[10]
    return score

def eval_method(proj: str, id: str):
    try:
        oraclefile = utf8open(f'oracle/ActualFaultStatement/{proj}/{id}')
    except IOError:
        print(f'{proj}{id} has no oracle')
        return -1
    oracle_lines = set()
    for line in oraclefile.readlines():
        sp = line.split('||')
        for oracle in sp:
            oracle_lines.add(oracle.strip().split(':')[0])
    try:
        resultfile = utf8open(
            f'trace/logs/mytrace/InfResult-{proj}{id}.log')
    except IOError:
        print(f'{proj}{id} has failed.')
        return -2
    i = 0
    ret = -3
    lines = set()
    for line in resultfile.readlines():
        if(line.strip() == '' or line.startswith('Probabilities:') or line.startswith('Vars:') or line.startswith('Stmts:') or line.startswith('Belief propagation time')):
            continue
        sp = line.split(':')
        if(sp.__len__() < 3):
            print(line)
        classname = sp[0]
        methodname = sp[1]
        sp = sp[2].split('#')
        linenumber = sp[1]
        fullname = f'{classname}.{methodname}'
        if fullname in lines:
            continue
        lines.add(fullname)
        i += 1
        if fullname in oracle_lines:
            ret = i
            break
    print(f'{proj}{id} result ranking: {ret}')
    return ret


def eval(proj: str, id: str):
    try:
        oraclefile = utf8open(f'oracle/ActualFaultStatement/{proj}/{id}')
    except IOError:
        print(f'{proj}{id} has no oracle')
        return -1
    oracle_lines = set()
    for line in oraclefile.readlines():
        sp = line.split('||')
        for oracle in sp:
            oracle = oracle.strip()
            oracle = re.sub(pattern=r"\$[^\.]*\.", repl=".", string= oracle)
            oracle = re.sub(pattern=r"\.[^.]*:", repl=":", string= oracle)
            oracle_lines.add(oracle)
    try:
        resultfile = utf8open(
            f'trace/logs/mytrace/InfResult-{proj}{id}.log')
    except IOError:
        print(f'{proj}{id} has failed.')
        return -2
    i = 0
    ret = -3
    prob = 0
    lines = set()
    for line in resultfile.readlines():
        if(line.strip() == '' or line.startswith('Probabilities:') or line.startswith('Vars:') or line.startswith('Stmts:') or line.startswith('Belief propagation time')):
            continue
        if(line.startswith("out scale")):
            continue
        sp = line.split(':')
        if(sp.__len__() < 3):
            print(line)
        classname = sp[0]
        methodname = sp[1]
        if methodname == '<clinit>':
            methodname = '<init>'
        sp = sp[2].split('#')
        linenumber = sp[1]
        fullname = f'{classname}:{linenumber}'
        if fullname in lines:
            continue
        if classname.lower().startswith('test') or classname.lower().endswith('test'):
            continue
        lines.add(fullname)
        i += 1
        if fullname in oracle_lines:
            ret = i
            prob = float(line.split("=")[1])
            break
    print(f'{proj}{id} result ranking: {ret}')
    # print(f'{proj}{id} result ranking: {ret}, prob = {prob}')
    return ret


def zeval(proj: str, id: str):
    try:
        oraclefile = utf8open(f'oracle/ActualFaultStatement/{proj}/{id}')
    except IOError:
        print(f'{proj}{id} has no oracle')
        return -1
    oracle_lines = set()
    for line in oraclefile.readlines():
        sp = line.split('||')
        for oracle in sp:
            oraclesp = oracle.rindex('.')
            neworacle = oracle[:oraclesp]+':'+oracle[oraclesp+1:].split(':')[1]
            oracle_lines.add(neworacle.strip())
    # print(oracle_lines)
    try:
        resultfile = utf8open(
            f'TSE19_result/{proj.lower()}{id}')
    except IOError:
        print(f'{proj}{id} has failed.')
        return -2
    i = 0
    ret = -3
    for line in resultfile.readlines():
        sp = line.split(' ')
        fullname = sp[0].strip()
        # print(fullname)
        i += 1
        if fullname in oracle_lines:
            ret = i
            break
    print(f'{proj}{id} result ranking: {ret}')
    return ret

def zcompare(proj: str):
    better =0
    worse = 0
    equal = 0
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = eval(proj, str(i))
        zresult = zeval(proj, str(i))
        if(result > 0 and zresult >0):
            if(result<zresult):
                better += 1
            elif(result==zresult):
                equal += 1
            else:
                worse += 1
        elif(result<=0 and zresult >0):
            worse +=1
        elif(result>0 and zresult <=0):
            better +=1
        else:
            equal +=1
    print(
        f'better={better},equal={equal},worse={worse}')


def gentrigger(proj: str):
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        try:
            # cleanupcheckout(proj, i)
            # clearcache(proj, i)
            # checkout(proj, i)
            metadata = getmetainfo(proj, i)
        except FileNotFoundError:
            print(f'{proj}{i} has no trigger.')
            continue
            # cleanupcheckout(proj, i)
            # clearcache(proj, i)
            # checkout(proj, i)
            # try:
            #     metadata = getmetainfo(proj, i)
            # except FileNotFoundError:
            #     print(f'{proj}{i} has no trigger.')
        trigger_tests = metadata['tests.trigger'].strip()
        utf8open_w(f'./triggertest/{proj}/{i}').write(trigger_tests)
        # print(f'{proj}{i} triggertest: {trigger_tests}')


def match(proj: str, id: str):
    oraclepath = f'./oracle/ActualFaultStatement/{proj}/{id}'
    try:
        oraclefile = utf8open(oraclepath)
    except IOError:
        print(f'{proj}{id} has no oracle')
        return -1
    oracle_lines = set()
    for line in oraclefile.readlines():
        sp = line.split('||')
        for oracle in sp:
            oracle_lines.add(oracle.strip())

    triggerpath = f'./triggertest/{proj}/{id}'
    try:
        triggertests = utf8open(triggerpath).read().strip().split(';')
    except IOError:
        print(f'{proj}{id} has no trigger')
        return -2
    for testlog in triggertests:
        testlog = testlog.replace('::', '.')
        logpath = f'{checkoutbase}/{proj}/{id}/trace/logs/run/{testlog}.log'
        try:
            logfile = utf8open(logpath)
        except IOError:
            print(f'{proj}{id} misses log of triggertest    {testlog}')
            return -3
        for line in logfile.readlines():
            sp = line.split(',')
            for instinfo in sp:
                spinfo = instinfo.split('=')
                if spinfo[0] == 'lineinfo':
                    sporacle = spinfo[1].split('#')
                    compare_oracle = sporacle[0] + \
                        '.'+sporacle[1]+':'+sporacle[3]
                    if compare_oracle in oracle_lines:
                        print(
                            f'{proj}{id} log has oracle,              in {testlog}')
                        return 1
    print(f'{proj}{id} has no oracle in trigger log')
    return 0


def matchproj(proj: str):
    no_oracle = 0
    no_trigger = 0
    in_log = 0
    not_in_log = 0
    no_trigger_log = 0
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = match(proj, str(i))
        if result == 1:
            in_log += 1
        elif result == 0:
            not_in_log += 1
        elif result == -1:
            no_oracle += 1
        elif result == -2:
            no_trigger += 1
        else:
            no_trigger_log += 1

    print(f'in_log = {in_log}, not_in_log = {not_in_log}, no_oracle = {no_oracle}, no trigger = {no_trigger}, no_trigger_log = {no_trigger_log}')

def extract(proj: str, id: str):
    extract_dir = os.path.abspath(f'./extract_message/{proj}/{id}')
    if not os.path.exists(extract_dir):
        os.mkdir(extract_dir)
    triggerpath = f'./triggertest/{proj}/{id}'
    try:
        triggertests = utf8open(triggerpath).read().strip().split(';')
    except IOError:
        print(f'{proj}{id} has no trigger')
        return -2
    for testlog in triggertests:
        testlog = testlog.replace('::', '.')
        logpath = f'{checkoutbase}/{proj}/{id}/trace/logs/run/{testlog}.log'
        try:
            each_line = utf8open(logpath).readlines()
        except IOError:
            print(f'{proj}{id} misses log of triggertest    {testlog}')
            continue
            # return -3
        wf = utf8open_w(f'{extract_dir}/{testlog}')
        wf.write(f'begin: {testlog}\n')
        wf.write(f'log lengh: {each_line.__len__()}\n\n')
        line_number = 0
        last_number = 0
        method = ''
        stack_tab = 0
        total_number = each_line.__len__()
        for i in range(total_number):
            line = each_line[i]
            if(not line.startswith("opcode")):
                continue
            # print(line)
            split = line.split(",")
            tracemap = {}
            for instinfo in split:
                splitinstinfo = instinfo.split("=")
                infotype = splitinstinfo[0]
                infovalue = splitinstinfo[1]
                tracemap[infotype] = infovalue
            opcode_infos = tracemap["opcode"].split("(")
            opcode_number = int(opcode_infos[0])
            if opcode_number >= 182 and opcode_number <= 186:
                next_line = each_line[i+1]
                if(not line.startswith("opcode")):
                    pass

            lineinfos = tracemap["lineinfo"].split("#")
            this_method = lineinfos[0]+"#"+lineinfos[1]
            if this_method != method:
                wf.write(f'{line_number}:{this_method}   {line_number-last_number}\n')
                method = this_method
                last_number = line_number
            line_number += 1
    return 0 

def extractproj(proj: str):
    allbugs = project_bug_nums[proj]
    for i in range(1, allbugs+1):
        result = extract(proj, str(i))

def decoder():
    cmdline = f'mvn compile -q && mvn exec:java "-Dexec.mainClass=ppfl.instrumentation.TraceDecoder"'
    os.system(cmdline)
