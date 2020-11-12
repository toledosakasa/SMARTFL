# ppfl

## Run Tracing
```
python s.py trace MergeTest#fail
```

Trace log will be generated at trace/logs/

## Run GraphTest
```
python s.py test GraphTest#mergetest_bc
```

## Add new testcase
New Junit testcase at test/trace/

run tracing on all methods.

Add new test method in GraphTest (basically the same as existing ones.)

## Run on defects4j
Modify defects4j with the following:

```
git fetch https://github.com/Ultimanecat/defects4j
```

Look at relevant information:

```
defects4j query -p Lang -q "bug.id,classes.relevant.src,classes.relevant.test,tests.relevant,tests.trigger"  -o langtest.csv
```

The instrumented classes should be "classes.relevant.src"+"tests.relevant"

Test methods to run should be all test methods in "tests.relevant"

Commandline tracing example:

```
defects4j checkout -p Lang -v 3b -w ./lang3b

cd lang3b

defects4j compile

defects4j test -t org.apache.commons.lang3.math.NumberUtilsTest::testStringCreateNumberEnsureNoPrecisionLoss -a "-Djvmargs=-noverify -Djvmargs=-javaagent:[pathToAssembledTracer.jar]=logfile=[logfilename],instrumentingclass=org.apache.commons.lang3.math.NumberUtils:org.apache.commons.lang3.StringUtils:org.apache.commons.lang3.math.NumberUtilsTest"
```

for debugging use, copy the ant commandline (you should see that by running the instructions above) and run it.


