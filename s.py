import os
import sys
import platform
btrace_home=os.path.abspath("./lib/btrace")
btrace_bin = btrace_home + "/bin"
btracec = btrace_bin + '/btracec'
btracer = btrace_bin + '/btracer'
if(platform.system() == 'Windows'):
	btracec = btracec + '.bat'
	btracer = btracer + '.bat'

dirs2make = ["./configs","./test/trace/patterns","./test/trace/logs","./test/trace/logs/btrace","./test/trace/logs/mytrace"]
testdir = os.path.abspath("./test/trace")
for dir in dirs2make:
	if not(os.path.exists(dir)):
		os.makedirs(dir)
# os.system("export BTRACE_HOME=%s" % (btrace_home))

def utf8open(filename):
	return open(filename, encoding='utf-8',errors='ignore')

def btrace():
	f=utf8open("%s/scripts/AllLines_pattern.java"%(btrace_home))
	patternstr=f.read()
	f.close()

	#generate pattern file for btrace
	files = os.listdir(testdir)
	scriptroot = testdir + '/patterns'
	for filename in files:
		if not(os.path.isdir(filename)):
			if filename.endswith('.java'):
				classname = filename[0:-5]
				pattern_name = classname + "_pattern"
				writestr = patternstr.replace('__CLASS__NAME__','trace.'+classname)
				writestr = writestr.replace('AllLines',pattern_name)
				tempf=utf8open("%s/%s.java"%(scriptroot,pattern_name),'w+')
				tempf.write(writestr)
				tempf.close()
				
				#generate launch template for eclipse.
				file = utf8open(testdir+'/'+filename)
				lines = file.readlines()
				flag = False
				testnames = []
				for line in lines:
					if flag == True:
						l = line.find("void")
						r = line.find("(")
						testnames.append(line[l+5:r])
						flag = False
					if line.find("@Test")!=-1:
						flag = True
					else:
						flag = False
				for testname in testnames:
					f = utf8open("templates/btrace_template.launch")
					s = f.read()
					f.close()
					s = s.replace("#CLASSNAME#",classname)
					s = s.replace("%TESTNAME%",testname)
					f = utf8open("configs/btrace/%s.%s.launch"%(classname,testname),'w+')
					f.write(s)
					f.close()

	#invoke btracec. please set $JAVA_HOME$ first.
	files = os.listdir(scriptroot)
	for filename in files:
		if(filename.endswith('.java')):
			os.system("cd %s && %s %s"%(scriptroot,btracec,filename))
		
def mytrace():
	files = os.listdir(testdir)
	for filename in files:
		if not(os.path.isdir(filename)):
			if filename.endswith('.java'):
				classname = filename[0:-5]
				
				#generate launch template for eclipse.
				file = utf8open(testdir+'/'+filename)
				lines = file.readlines()
				flag = False
				testnames = []
				for line in lines:
					if flag == True:
						l = line.find("void")
						r = line.find("(")
						testnames.append(line[l+5:r])
						flag = False
					if line.find("@Test")!=-1:
						flag = True
					else:
						flag = False
				for testname in testnames:
					f = utf8open("templates/mytrace_template.launch")
					s = f.read()
					f.close()
					s = s.replace("#CLASSNAME#",classname)
					s = s.replace("%TESTNAME%",testname)
					f = utf8open("configs/mytrace/%s.%s.launch"%(classname,testname),'w+')
					f.write(s)
					f.close()

args = sys.argv
if len(args)!=2:
	print('''usage:
	s.py btrace preprocessing for btrace tracing
	s.py mytrace preprocessing for built-in tracing''')
	exit()

if args[1] == 'mytrace':
	mytrace()
	
if args[1] == 'btrace':
	btrace()

#tracedir = os.path.abspath("./test_traces")			
#simpletestdir = os.path.abspath("./simpletests")

# jvmargs="-javaagent:%s/build/btrace-agent.jar=noserver,debug=true,scriptOutputFile=%s,script=%s/scripts/AllLines.class" % (btrace_home, tracefile, btrace_home)

# f=open("%s/scripts/AllLines_pattern.java"%(btrace_home))
# s=f.read()
# f.close()
# s=s.replace('__CLASS__NAME__',srcname)
# f=open("%s/scripts/AllLines.java"%(btrace_home),'w')
# f.write(s)
# f.close()
# os.system("cd %s/scripts && %s AllLines.java"%(btrace_home,btracec))

# os.system("cd %s && javac "%(simpletestdir) + srcname + ".java")

# os.system("cd %s && java "%(simpletestdir) + jvmargs+ " "+ " -noverify "+ srcname)
# print("cd %s && %s %s/scripts/AllLines.class %s -o %s.log" %(simpletestdir,btracer,btrace_home,srcname,tracefile))
# os.system("cd %s && %s %s/scripts/AllLines.class %s -o %s.log" %(simpletestdir,btracer,btrace_home,srcname,tracefile))
