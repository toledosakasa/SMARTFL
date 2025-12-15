import os
import sys
from os.path import join, getsize
import re
import json

project_bug_nums = {"Lang": 65, "Math": 106,
                    "Time": 27, "Closure": 176, "Chart": 26}

def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')

def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')

def utf8open_a(filename):
    return open(filename, 'a+', encoding='utf-8', errors='ignore')


def find(proj:str):
    path = f"./metadata_cached/{proj}"
    for i in range(project_bug_nums[proj]):
        id = i + 1
        trigger_klassset = set()
        relevant_klassset = set()
        try:
            logfile = utf8open(f'{path}/{id}.log')
        except IOError:
            print(f'no {proj} {id} log')
            continue
        triggercnt = 0
        relevantcnt = 0
        triggertestcnt = 0
        relevanttestcnt = 0
        for line in logfile.readlines():
            if line.startswith("tests.trigger"):
                tests = line.split('=')[1]
                tests = tests.split(';')
                for test in tests:
                    klass = test.split('::')[0]
                    trigger_klassset.add(klass)
                    triggercnt += 1
            if line.startswith("tests.relevant"):
                klasses = line.split('=')[1]
                klasses = klasses.split(';')
                for klass in klasses:
                    relevant_klassset.add(klass)
                    relevantcnt += 1
            
            if line.startswith("methods.test.all"):
                tests = line.split('=')[1]
                tests = tests.split(';')
                for test in tests:
                    if(len(test.split("::")) == 1):
                        continue
                    klass = test.split("::")[0]
                    methods = test.split("::")[1]
                    methods = methods.split(",")
                    if klass in relevant_klassset:
                        if(methods[0] != ""):
                            relevanttestcnt += len(methods)
                    if klass in trigger_klassset:
                        if(methods[0] != ""):
                            triggertestcnt += len(methods)
        logfile.close()
        try:
            profile = utf8open(f'{path}/{id}.profile.log').read()
        except IOError:
            print(f'no {proj} {id} profile log')
            continue
        relevant_testmethods = json.loads(profile)
        totalcnt = 0
        intriggercnt = 0
        inrelevantcnt = 0
        for (cname, mname) in relevant_testmethods:
            totalcnt += 1
            if cname in trigger_klassset:
                intriggercnt += 1
            if cname in relevant_klassset:
                inrelevantcnt += 1
        print(f'{proj} {id}: intrigger = {intriggercnt}, inrelevant = {inrelevantcnt}, total = {totalcnt}, trigger_test = {triggercnt}, triggerTesttest = {triggertestcnt} relevantclass = {relevantcnt}, relevanttest = {relevanttestcnt}')



if __name__ == '__main__':
    args = sys.argv
    if len(args) <= 1:
        print('need parameters')

    proj = args[1]
    find(proj)