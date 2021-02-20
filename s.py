import os
import sys
import pylib.defects4j as d4j


def runtesttrace(cmdarg):
    bindir = os.path.abspath("./target")
    cmdargs = cmdarg.split('#')
    cmdstr = " mvn jar:jar && mvn test -Dtest={classname}#{testname} -DargLine=\"-noverify -javaagent:{jardir}/ppfl-0.0.1-SNAPSHOT.jar=logfile={classname}.{testname},instrumentingclass=trace.{classname}\"".format(
        classname=cmdargs[0], testname=cmdargs[1], jardir=bindir)
    print(cmdstr)
    os.system(cmdstr)


def runtest(cmdarg):
    cmdstr = "mvn test -Dtest={testname}".format(testname=cmdarg)
    print(cmdstr)
    os.system(cmdstr)


def makedirs():
    dirs2make = ["./configs", "./test/trace/patterns", "./test/trace/logs",
                 "./test/trace/logs/btrace", "./test/trace/logs/mytrace", "./d4j_resources/metadata_cached/"]
    for dir in dirs2make:
        if not(os.path.exists(dir)):
            os.makedirs(dir)


if __name__ == '__main__':
    makedirs()
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
        d4j.getd4jprojinfo()

    if args[1] == 'rund4j':
        projname = args[2]
        bugid = args[3]
        cmdlines = d4j.getd4jcmdline(projname, bugid)
        checkoutdir = f'tmp_checkout/{projname}{bugid}'
        # cleanup previous log
        os.system(f'rm {checkoutdir}/trace/logs/mytrace/all.log')
        cdcmd = f'cd {checkoutdir} && '
        for cmdline in cmdlines:
            print(cmdline)
            # input()
            os.system(cdcmd + cmdline)
    if args[1] == 'clearcache':
        d4j.clearcache()
