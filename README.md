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
python s.py trace MergeTest#fail


Trace log will be generated at trace/logs/

## Run GraphTest
python s.py test GraphTest#mergetest_bc

## Add new testcase
New Junit testcase at test/trace/

run tracing on all methods.

Add new test method in GraphTest (basically the same as existing ones.)

## Run on defects4j
Modify defects4j with the following:

git fetch https://github.com/Ultimanecat/defects4j

(cd to your working directory)

defects4j checkout -w ./lang3b

Look at relevant information:

defects4j query -p Lang -q "bug.id,classes.relevant.src,classes.relevant.test,tests.relevant,tests.trigger"  -o langtest.csv

The instrumented classes should be "classes.modified"+"tests.relevant"

Test methods to run should be all test methods in "tests.relevant"

From commandline run the following to trace d4j:

defects4j test -t org.apache.commons.lang3.math.NumberUtilsTest::testStringCreateNumberEnsureNoPrecisionLoss -w ./lang3b -a -Djvmargs=\"-javaagent:[pathtotracer.jar]=logfile=[pathtologfile],instrumentingclass=org.apache.commons.lang3.math.NumberUtils:org.apache.commons.lang3.StringUtils\"

for debugging use, copy the ant commandline (you should see that by running the instructions above) and run it.


