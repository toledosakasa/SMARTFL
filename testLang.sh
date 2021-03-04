#!/bin/bash  
  
for((i=3;i<=65;i++));  
do   
python3 s.py clearcache Lang $i
timeout 1200 python3 s.py rund4j Lang $i > trace/runtimelog/Lang$i.rund4j.log
timeout 1200 python3 s.py parsed4j Lang $i > trace/runtimelog/Lang$i.parsed4j.log
done  