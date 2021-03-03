#!/bin/bash  
  
for((i=1;i<=1;i++));  
do   
timeout 600 python3 s.py fl Lang $i > trace/runtimelog/$i.screen.log
done  