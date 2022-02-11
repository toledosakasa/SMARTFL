# SmartFL

## Requiring
defects4j from https://github.com/rjust/defects4j

maven >= 3.6.0

python >= 3.6.9

pip install func_timeout numpy

## Run on defects4j
Modify defects4j with the following:

```
cd "Path2D4j"
git patch "Path2ThisRepo"/defects4j-mod/diff.patch
```

Localize a single bug:
```
python3 s.py fl {proj} {id}

e.g.
python3 s.py fl Lang 1
```

Test a project:
```
python3 s.py testproj {proj}

e.g.
python3 s.py testproj Lang
```

Evaluate a project on statement-level:
```
python3 s.py eval {proj}
```

Evaluate a project on method-level:
```
python3 s.py meval {proj}
```


## Add SmartFL to CombineFL
Generate add-in data file of SmartFL
```
tar zxvf CombineFLwithSmartFL.tar.gz
cd combinefl
python3 gendata.py
```

Run [CombineFL](https://damingz.github.io/combinefl/index.html) (Notice that CombineFL needs python2)
```
python 1-combine.py -f add_in
./2-split.sh
./3-crossvalidation.sh
python 4-calc-metric.py
```
