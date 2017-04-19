#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

for tag in KStemAnalyzer KStemAnalyzerAnchor; do
for test in MQ09 CW09A CW09B CW12B; do

    for train in MQ09 CW09A CW09B CW12B; do
        ./run.sh X -tag "$tag" -test "$test" -train "$train" -optimize NDCG100 -report NDCG100 &
    done
    wait

#    for train in MQ09 CW09A CW09B CW12B; do
#        ./run.sh X -tag "$tag" -test "$test" -train "$train" -optimize NDCG20 -report NDCG20 &
#    done
#    wait

    for train in MQ09 CW09A CW09B CW12B; do
        ./run.sh X -tag "$tag" -test "$test" -train "$train" -optimize MAP -report MAP &
    done
    wait

done
done

./run.sh IP -task short
./run.sh IP -task summary
./y.sh