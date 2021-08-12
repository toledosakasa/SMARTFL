# ppfl

## Requiring
pip install func_timeout

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

Localize a single bug:
```
python s.py fl Lang 1
```

Test a project:
```
python s.py testproj Lang
```


