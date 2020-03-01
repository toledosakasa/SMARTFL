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
Note that these configs will be automatically deleted after import. You can re-generate by running s.py again.

## Run Tracing
(in eclipse)Run -> Run configurations -> DomainTest.test -> run
Trace log will be generated at test/trace/logs/

## Run GraphTest

