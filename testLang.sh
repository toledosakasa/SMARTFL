#!/bin/bash  
  
for((i=1;i<=42;i++));  
do   
echo running Lang $i
timeout 600 python3 s.py rerun Lang $i > trace/runtimelog/Lang$i.rund4j.log
timeout 600 python3 s.py parsed4j Lang $i > trace/runtimelog/Lang$i.parsed4j.log
done  

for((i=44;i<=65;i++));  
do   
echo running Lang $i
timeout 600 python3 s.py rerun Lang $i > trace/runtimelog/Lang$i.rund4j.log
timeout 600 python3 s.py parsed4j Lang $i > trace/runtimelog/Lang$i.parsed4j.log
done  