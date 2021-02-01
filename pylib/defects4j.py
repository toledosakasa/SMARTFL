import os
alld4jprojs = ["Chart", "Cli", "Closure", "Codec", "Collections", "Compress", "Csv", "Gson",
               "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Lang", "Math", "Mockito", "Time"]


def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')


def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')


def getd4jprojinfo():
    for proj in alld4jprojs:
        getinstclassinfo(proj)


def getinstclassinfo(proj):
    cmdline = f"defects4j query -p {proj} -q \"bug.id,classes.relevant.src,classes.relevant.test,tests.trigger,tests.relevant\"  -o ./d4j_resources/{proj}.csv"
    os.system(cmdline)


def getmetainfo(proj, id):
    ret = {}
    # caching
    print('Checking for cache...')
    cachepath = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}{id}.log')
    if os.path.exists(cachepath):
        print('cache found')
        lines = utf8open(cachepath).readlines()
        for line in lines:
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


def getd4jcmdline(proj, id):
    print('getting metainfo')
    metadata = getmetainfo(proj, id)
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    tests_relevant = metadata['tests.relevant']
    instclasses = metadata['classes.relevant'].strip() + \
        ';' + tests_relevant.strip()
    instclasses = instclasses.replace(";", ":")
    testmethods = metadata['methods.test.all'].split(';')
    relevant_classes = tests_relevant.split(';')
    #l = len(testnames)
    #print(f'Test methods: {l}')
    #" defects4j test -t org.apache.commons.lang3.math.NumberUtilsTest::testStringCreateNumberEnsureNoPrecisionLoss2,testStringCreateNumberEnsureNoPrecisionLoss1,testStringCreateNumberEnsureNoPrecisionLoss3"
    ret = []
    d4jdatafile = os.path.abspath(
        f'./d4j_resources/metadata_cached/{proj}{id}.log')
    for testmethod in testmethods:
        testclassname = testmethod.split('::')[0]
        if testclassname in relevant_classes:
            app = f"defects4j test -t {testmethod} \
            -a \"-Djvmargs=-noverify \
                -Djvmargs=-javaagent:{jarpath}=instrumentingclass={instclasses},d4jdatafile={d4jdatafile}\""
            ret.append(app)
    return ret


def checkout(proj, id):
    checkoutpath = './tmp_checkout'
    if not(os.path.exists(checkoutpath)):
        os.makedirs(checkoutpath)
    os.system(
        f'defects4j checkout -p {proj} -v {id}b -w ./tmp_checkout/{proj}{id}')


def clearcache():
    cachepath = os.path.abspath('./d4j_resources/metadata_cached/')
    os.system('rm -rf '+cachepath)
