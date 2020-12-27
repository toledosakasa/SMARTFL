import os
import sys

dirs2make = ["./configs", "./test/trace/patterns", "./test/trace/logs",
             "./test/trace/logs/btrace", "./test/trace/logs/mytrace"]
bindir = os.path.abspath("./target")
alld4jprojs = ["Chart", "Cli", "Closure", "Codec", "Collections", "Compress", "Csv", "Gson",
               "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Lang", "Math", "Mockito", "Time"]
for dir in dirs2make:
    if not(os.path.exists(dir)):
        os.makedirs(dir)


def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')


def runtesttrace(cmdarg):
    cmdargs = cmdarg.split('#')
    cmdstr = " mvn jar:jar && mvn test -Dtest={classname}#{testname} -DargLine=\"-noverify -javaagent:{jardir}/ppfl-0.0.1-SNAPSHOT.jar=logfile={classname}.{testname},instrumentingclass=trace.{classname}\"".format(
        classname=cmdargs[0], testname=cmdargs[1], jardir=bindir)
    print(cmdstr)
    os.system(cmdstr)


def runtest(cmdarg):
    cmdstr = "mvn test -Dtest={testname}".format(testname=cmdarg)
    print(cmdstr)
    os.system(cmdstr)


def getd4jprojinfo():
    for proj in alld4jprojs:
        getinstclassinfo(proj)


def getinstclassinfo(proj):
    cmdline = "defects4j query -p {projname} -q \"bug.id,classes.relevant.src,classes.relevant.test,tests.trigger\"  -o ./d4j_resources/{projname}.csv".format(
        projname=proj)
    os.system(cmdline)


def getmetainfo(proj, id):
    ret = {}
    # caching
    cachepath = os.path.abspath(
        './d4j_resources/metadata_cached/{proj}{id}.log'.format(proj=proj, id=id))
    if os.path.exists(cachepath):
        lines = utf8open(cachepath).readlines()
        for line in lines:
            splits = line.split('=')
            ret[splits[0]] = splits[1]
        return ret

    # if not cached, generate metadata
    workdir = os.path.abspath(
        './tmp_checkout/{proj}{id}'.format(proj=proj, id=id))
    if not os.path.exists(workdir):
        checkout(proj, id)
    fields = ['tests.all', 'classes.relevant.src', 'tests.trigger']
    for field in fields:
        ret[field] = os.popen(
            'defects4j export -p {field} -w {workdir}'.format(field=field, workdir=workdir))
    cmdline_getallmethods = 'mvn -q exec:java "-Dexec.mainClass=ppfl.defects4j.Instrumenter" "-Dexec.args={proj}{id}"'.format(
        proj=proj, id=id)
    os.system(cmdline_getallmethods)
    # FIXME
    ret['methods.test.all'] = utf8open(
        './d4j_resources/metadata_cached/{proj}{id}.alltests.log'.format(proj=proj, id=id)).readlines

    # write to cache
    cachedir = os.path.abspath('./d4j_resources/metadata_cached')
    if not os.path.exists(cachedir):
        os.mkdir(cachedir)
    cachefile = utf8open(cachepath)
    for k in ret:
        cachefile.write('{key}={value}'.format(key=k, value=ret[k]))
    # cleanup
    os.system('rm -rf {workdir}'.format(workdir=workdir))
    return ret


def getd4jcmdline(proj, id, testname):
    metadata = getmetainfo(proj, id)
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    instclasses = metadata['classes.relevant.src'] + \
        ';' + metadata['tests.all']
    instclasses = instclasses.replace(";", ":")
    testnames = metadata['methods']
    ret = "defects4j test -t {testname} \
        -a \"-Djvmargs=-noverify \
            -Djvmargs=-javaagent:{jarpath}=logfile={dottestname},\
                instrumentingclass={instclass}\"".format(testname=testname, jarpath=jarpath, dottestname=testname.replace("::", "."), instclass=instclasses)
    return ret


def checkout(proj, id):
    os.system(
        'defects4j checkout -p {proj} -v {id}b -w ./tmp_checkout/{proj}{id}'.format(proj=proj, id=id))


args = sys.argv
if len(args) <= 1:
    print('''usage:
	s.py btrace preprocessing for btrace tracing
	s.py mytrace preprocessing for built-in tracing''')
    exit()

if args[1] == 'trace':
    runtesttrace(args[2])

if args[1] == 'test':
    runtest(args[2])

if args[1] == 'd4jinit':
    getd4jprojinfo()
