#!/usr/bin/env bash

for i in {5..95..5}
  do
     echo "calling eval.sh with spam_$i argument"
     ./eval.sh "spam_$i"
     ./mq.sh "spam_$i"
     ./mqe1.sh "spam_$i"
 done