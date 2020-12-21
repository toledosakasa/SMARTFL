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
    str = "defects4j query -p " + proj + \
        " -q \"bug.id,classes.relevant.src,classes.relevant.test,tests.trigger\"  -o ./d4j_resources/" + proj + ".csv"
    os.system(str)


def loadinstclass(proj, id):
    infofile = utf8open("./d4j_resources"+proj)
    lines = infofile.readlines()
    for line in lines:
        splits = line.split(',')
        if splits[0] == str(id):
            return splits[1].strip('\"') + ';' + splits[2].strip('\"')
    return ""


def getd4jcmdline(proj, id, testname):
    instclass = loadinstclass(proj, id)
    ret = ""
    ret += "defects4j test -t "
    ret += testname  # org.apache.commons.lang3.math.NumberUtilsTest::testStringCreateNumberEnsureNoPrecisionLoss3
    ret += "-a \"-Djvmargs=-noverify -Djvmargs="
    jarpath = os.path.abspath(
        "./target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar")
    # /mnt/e/My_files/eclipse-workspace/ppfl/target/ppfl-0.0.1-SNAPSHOT-jar-with-dependencies.jar
    ret = ret + "-javaagent:" + jarpath + "="
    # org.apache.commons.lang3.math.NumberUtilsTest.testStringCreateNumberEnsureNoPrecisionLoss3,
    ret = ret + "logfile=" + testname.replace("::", ".") + ","
    # org.apache.commons.lang3.StringUtils:org.apache.commons.lang3.math.NumberUtils:org.apache.commons.lang3.math.NumberUtilsTest"
    ret = ret + "instrumentingclass=" + instclass
    return ret


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
