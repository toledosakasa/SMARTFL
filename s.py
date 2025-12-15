import os
from re import template
import sys
import pylib.defects4j as d4j
import time
import func_timeout
import random
import numpy as np


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
                 "./trace/runtimelog/", "./test/trace/logs/mytrace", "./d4j_resources/metadata_cached/"]
    for dir in dirs2make:
        if not(os.path.exists(dir)):
            os.makedirs(dir)

def modify(decay,site):
    changes = 0.5*(0.9**(decay))#0.25 0.5
    data = np.loadtxt('infer.txt',delimiter=',')
    f = open('infer.txt', 'w+', encoding='utf-8', errors='ignore')
    s = ''
    a = []

    ori = data[site]
    #data[site] = data[site] + changes*(random.random()*2-1)
    #if data[site] > 1:
    #    data[site] = 1.0
    #if data[site] < 0:
    #    data[site] = 0.0
    while ori == data[site]:
        data[site] = data[site] + changes*(random.random()*2-1)
        if data[site] > 1:
            data[site] = 1.0
        if data[site] < 0:
            data[site] = 0.0

    #data[site+4] = data[site+4] + changes*(random.random()*2-1)
    #if data[site+4] > 1:
    #    data[site+4] = 1.0
    #if data[site+4] < 0:
    #    data[site+4] = 0.0
    #part_a1 = sorted(data[0:4])
    #part_a2 = sorted(data[4:8])
    #a[0:4]=part_a1
    #a[4:8]=part_a2
    a = data
    for i in range(len(a)):
        if i<len(a)-1:
            s = s + str(a[i])+','
        else:
            s = s + str(a[i])
    f.write(s)
    f.close()

def modifyback(data):
    f = open('infer.txt', 'w+', encoding='utf-8', errors='ignore')
    s = ''
    for i in range (len(data)):
        if i<len(data)-1:
            s = s + str(data[i])+','
        else:
            s = s + str(data[i])
    f.write(s)
    f.close()

def train(proj, looptime):
    d4j.parseproj(proj)
    score = d4j.pevalproj(proj)
    assert(score>0)
    bestscore = score
    bestprob = np.loadtxt('infer.txt',delimiter=',')

    iters = int(looptime)
    for i in range(iters):
        for j in range(len(bestprob)):#para
            data = np.loadtxt('infer.txt',delimiter=',')
            modify(i,j)
            d4j.parseproj(proj)
            tmpscore = d4j.pevalproj(proj)
            tmpdata = np.loadtxt('infer.txt',delimiter=',')

            delta_T = tmpscore - score

            f = open('spylog.txt', 'a+', encoding='utf-8', errors='ignore')
            s = ''
            s = s + "score:" + str(score) + ", tmpscore:" + str(tmpscore) + "\n" + "tmpprob:"
            for i in range (len(tmpdata)):
                    s = s + str(tmpdata[i]) + ','
            s = s + "\n"
            f.write(s)
            f.close()

            if delta_T > 0:
                score = tmpscore
                if score > bestscore:
                    bestscore = score
                    bestprob = np.loadtxt('infer.txt',delimiter=',')
            else:
                prob = min(tmpscore/(score+2**int(i)),0.05)
                if prob > random.random():
                    score = tmpscore
                    f = open('spylog.txt', 'a+', encoding='utf-8', errors='ignore')
                    f.write("prob:"+ str(prob)+"\n")
                    f.close()
                else:
                    modifyback(data)

    f = open('spylog.txt', 'a+', encoding='utf-8', errors='ignore')
    s = ''
    s = s + "score:" + str(score) + ", bestscore:" + str(bestscore) + "\n" 
    f.write(s)
    f.close()

    if score < bestscore:
        modifyback(bestprob)
            

def resetp():
    f = open('infer.txt', 'w+', encoding='utf-8', errors='ignore')
    #data = [0.32799097751396117,0.5838037049393161,0.5838037049393161,1.0,0.02932532074716611,0.4982062243886685,0.4982062243886685,1.0]
    data = [0.01,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4,0.4]
    s = ''
    for i in range (len(data)):
        if i<len(data)-1:
            s = s + str(data[i])+','
        else:
            s = s + str(data[i])
    f.write(s)
    f.close()

if __name__ == '__main__':
    makedirs()
    args = sys.argv
    if len(args) <= 1:
        print('''usage:
        s.py btrace preprocessing for btrace tracing
        s.py mytrace preprocessing for built-in tracing''')
        exit()

    if args[1] == 'rebuild':
        os.system('mvn package -DskipTests')

    if args[1] == 'trace':
        runtesttrace(args[2])

    if args[1] == 'test':
        runtest(args[2])

    if args[1] == 'd4jinit':
        d4j.getd4jprojinfo()

    if args[1] == 'fl':
        d4j.fl(args[2], args[3])
        d4j.eval(args[2], args[3])
        #d4j.rund4j(args[2], args[3])
        #d4j.parse(args[2], args[3])

    if args[1] == 'flw':
        d4j.fl_wrap(args[2], args[3])

    if args[1] == 'rund4j':
        if (len(args) == 5):
            if args[4] == '-nb':
                d4j.traceproj(args[2], args[3],False)
            else:
                d4j.traceproj(args[2], args[3])
        if(len(args) == 4):
            d4j.traceproj(args[2], args[3])
        if(len(args) == 3):
            d4j.traceproj(args[2],'all')
    
    if args[1] == 'rund4jtest':
        if (len(args) == 5):
            d4j.rund4jtest(args[2], args[3], args[4])
        else:
            print(f"python3 s.py rund4jtest proj id TestClass#testmethod")
    if args[1] == 'runfailtest':
        if (len(args) == 4):
            d4j.runfailtest(args[2], args[3])
        else:
            print(f"python3 s.py runfailtest proj id")

    if args[1] == 'parsed4j':
        if(len(args) == 4):
            d4j.parse(args[2], args[3])
            d4j.eval(args[2], args[3])
        if(len(args) == 3):
            d4j.parseproj(args[2])
    if args[1] == 'clearcache':
        d4j.clearcache(args[2], args[3])
        d4j.cleanupcheckout(args[2], args[3])
    if args[1] == 'rerun':
        d4j.rerun(args[2], args[3])
    if args[1] == 'eval':
        if(len(args) == 3):
            d4j.evalproj(args[2])
        if(len(args) == 4):
            d4j.eval(args[2], args[3])
    # if args[1] == 'zeval':
    #     if(len(args) == 3):
    #         d4j.zevalproj(args[2])
    #     if(len(args) == 4):
    #         d4j.zeval(args[2], args[3])
    # if args[1] == 'zcompare':
    #     if(len(args) == 3):
    #         d4j.zcompare(args[2])
    if args[1] == 'meval':
        if(len(args) == 3):
            d4j.evalproj_method(args[2])
        if(len(args) == 4):
            d4j.eval_method(args[2], args[3])
    if args[1] == 'testproj':
        d4j.testproj(args[2])
    if args[1] == 'testprojw':
        d4j.testprojw(args[2])
    if args[1] == 'gentrigger':
        d4j.gentrigger(args[2])
    if args[1] == 'match':
        if(len(args) == 3):
            d4j.matchproj(args[2])
        if(len(args) == 4):
            d4j.match(args[2], args[3])
    if args[1] == 'extract':
        if(len(args) == 3):
            d4j.extractproj(args[2])
        if(len(args) == 4):
            d4j.extract(args[2], args[3])
    if args[1] == 'peval':
        if(len(args) == 4):
            train(args[2],args[3])
    if args[1] == 'resetp':
        resetp()
    if args[1] == 'decoder':
        if(len(args) == 4):
            d4j.decoder(True, args[2])
        elif(len(args) == 3):
            d4j.decoder(False, args[2])
        else:
            d4j.decoder(False)
    if args[1] == 'runall':
        d4j.runall()
    if args[1] == 'parseall':
        d4j.parseall()
    if args[1] == 'evalall':
        d4j.evalall()
    