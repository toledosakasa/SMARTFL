import os
btrace_home=os.path.abspath("./lib/btrace")
testdir = os.path.abspath("./simpletests")
srcname = "foo"
tracedir = os.path.abspath("./test_traces")
tmp_tracefile = tracedir+"/tmp.txt"

# os.system("export BTRACE_HOME=%s" % (btrace_home))

jvmargs="-javaagent:%s/build/btrace-agent.jar=noserver,debug=true,scriptOutputFile=%s,script=%s/scripts/AllLines.class" % (btrace_home, tmp_tracefile, btrace_home)

f=open("%s/scripts/AllLines_pattern.java"%(btrace_home))
s=f.read()
f.close()
s=s.replace('__CLASS__NAME__',srcname)
f=open("%s/scripts/AllLines.java"%(btrace_home),'w')
f.write(s)
f.close()
os.system("cd %s/scripts && ../bin/btracec AllLines.java"%(btrace_home))


os.system("cd %s && javac "%(testdir) + srcname + ".java")

os.system("cd %s && java "%(testdir) + jvmargs+ " "+ " -noverify "+ srcname)
