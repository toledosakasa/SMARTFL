# ppfl

## Pre-processing

generate scripts for btrace, config for eclipse, etc.

```
python s.py
```

config file for eclipse at configs/

btrace pattern files at test/trace/patterns/

## Eclipse import
import config file at configs/


(in eclipse)File -> Import -> Run/debug -> (select configs/)


Note that these configs will be automatically deleted after import. 


You can re-generate by running s.py again.


## Run Tracing
(in eclipse)Run -> Run configurations -> DomainTest.test -> run


Trace log will be generated at test/trace/logs/

## Run GraphTest
Run As -> junit test

## Add new testcase
New Junit testcase at test/trace/

python s.py to pre-process.

import config files.

run tracing and get trace log.

Add new test method in GraphTest (basically the same as existing ones.)

## Run on defects4j
Modify defects4j with the following:

git fetch https://github.com/Ultimanecat/defects4j

(cd to your working directory)

defects4j checkout -w ./lang3b

defects4j test -t org.apache.commons.lang3.math.NumberUtilsTest::testStringCreateNumberEnsureNoPrecisionLoss -w ./lang3b -a -Djvmargs=\"-javaagent:[pathtotracer.jar]=logfile=[pathtologfile],instrumentingclass=org.apache.commons.lang3.math.NumberUtils:org.apache.commons.lang3.StringUtils\"

Note: log file name can be added with .1 as java.util.logging performs badly(FIXME)

