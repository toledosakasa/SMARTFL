import os
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
# Lang changes name from org.apache.commons.lang3 to org.apache.commons.lang after 40
classprefix = {'Lang': 'org.apache.commons.lang', 'Math': 'org.apache.commons.math', 'Chart': 'org.jfree', 'Time': 'org.joda.time', 'Closure': 'com.google.javascript', 'Mockito': 'org.mockito', 'Cli' : 'org.apache.commons.cli', 'Codec': 'org.apache.commons.codec', 'Csv' : 'org.apache.commons.csv', 'Gson' : 'com.google.gson', 'JacksonXml' : 'com.fasterxml.jackson.dataformat.xml'}
def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')


def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')


def utf8open_a(filename):
    return open(filename, 'a+', encoding='utf-8', errors='ignore')


checkoutbase = utf8open('checkout.config').readline().strip()
projectbase = os.path.abspath(".")


def genExceptionObserve(proj: str, id: int):
    obspath = f'{projectbase}/observe/{proj}'
    if not(os.path.exists(obspath)):
        os.makedirs(obspath)
    obsfile = utf8open_w(f'{obspath}/Exception-{id}.log')
    logspath = f'{checkoutbase}/{proj}/{id}/trace/logs/observe/'
    for root, dirs, files in os.walk(logspath):
        for name in files:
            if (re.match(r".*-obs\.log", name)):
                try:
                    logfile = utf8open(join(root, name))
                except IOError:
                    continue
                prefix = '    [junit] 	at '
                find = False
                for line in logfile.readlines():
                    if (line.startswith(prefix)):
                        line = line[len(prefix):]
                        if (line.startswith(classprefix[proj])):
                            methodname = line.split('(')[0].split('.')[-1]
                            classname = line.split('(')[0][:-(len(methodname) + 1)]
                            if(line.find(':') == -1):
                                continue
                            lineno = line.split(':')[1].strip()[:-1]
                            find = True
                            break
                if find:
                    name = name.split('-')[0]
                    testmethod = name.split('.')[-1]
                    name = name[:-(len(testmethod)+1)] + '::' + testmethod
                    obsfile.write(f'Fail-{name}-{classname}-{lineno}\n')




def genAssertObserve(proj: str, id: int):
    obspath = f'{projectbase}/observe/{proj}'
    if not(os.path.exists(obspath)):
        os.makedirs(obspath)
    obsfile = utf8open_w(f'{obspath}/Assert-{id}.log')
    logspath = f'{checkoutbase}/{proj}/{id}/trace/logs/mytrace/'
    for root, dirs, files in os.walk(logspath):
        for name in files:
            if (re.match(r".*\.source\.log", name)):
                try:
                    logfile = utf8open(join(root, name))
                except IOError:
                    continue
                assertset = {'assertTrue', 'assertFalse', 'assertEquals', 'assertNotEquals', 'assertNotNull', 'assertNull', 'assertSame', 'assertNotSame', 'assertArrayEquals'}
                for line in logfile.readlines():
                    sps = line.split(',')
                    add = False
                    for sp in sps:
                        if(sp.split('=')[0] == 'callname' and sp.split('=')[1] in assertset):
                            add  =True
                        if(sp.split('=')[0] == 'lineinfo'):
                            lineinfo = sp.split('=')[1]
                            break
                    if(add):
                        infos = lineinfo.split('#')
                        classname = infos[0]
                        methodname = infos[1]
                        lineno = infos[3]
                        obsfile.write(f'{classname}-{lineno}\n')