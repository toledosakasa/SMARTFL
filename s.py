import os
import sys

dirs2make = ["./configs","./test/trace/patterns","./test/trace/logs","./test/trace/logs/btrace","./test/trace/logs/mytrace"]
bindir = os.path.abspath("./target")
for dir in dirs2make:
	if not(os.path.exists(dir)):
		os.makedirs(dir)

def utf8open(filename):
	return open(filename, encoding='utf-8',errors='ignore')

def runtesttrace(cmdarg):
	cmdargs = cmdarg.split('#')
	cmdstr = " mvn jar:jar && mvn test -Dtest={classname}#{testname} -DargLine=\"-noverify -javaagent:{jardir}/ppfl-0.0.1-SNAPSHOT.jar=logfile={classname}.{testname},instrumentingclass=trace.{classname}\"".format(classname = cmdargs[0],testname=cmdargs[1],jardir=bindir)
	print(cmdstr)
	os.system(cmdstr)
	
def runtest(cmdarg):
	cmdstr = "mvn test -Dtest={testname}".format(testname=cmdarg)
	print(cmdstr)
	os.system(cmdstr)	

args = sys.argv
if len(args)<=2:
	print('''usage:
	s.py btrace preprocessing for btrace tracing
	s.py mytrace preprocessing for built-in tracing''')
	exit()

if args[1] == 'trace':
	runtesttrace(args[2])
	
if args[1] == 'test':
	runtest(args[2])

