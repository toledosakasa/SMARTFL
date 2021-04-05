#!/bin/bash  
start_time=$(date +%s)

for((i=1;i<=65;i++));  
do   
echo running Lang $i
timeout 1200 python3 s.py rerun Lang $i > trace/runtimelog/Lang$i.rund4j.log
timeout 1200 python3 s.py parsed4j Lang $i > trace/runtimelog/Lang$i.parsed4j.log
done  

end_time=$(date +%s)
cost_time=$[ $end_time-$start_time ]
echo "build kernel time is $(($cost_time/60))min $(($cost_time%60))s"
