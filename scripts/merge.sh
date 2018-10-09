#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

set=CW09B
# Merges multiple *.features files (for different Web Tracks) into one single file.
for model in BM25 DFIC DLH13 PL2 LGD Dirichlet DFRee DPH; do

 out="$TFD_HOME/${set}/features/${model}.features"
 touch ${out}
 truncate -s 0 ${out}

 for WT in 09 10 11 12;  do
  path="$TFD_HOME/${set}/features/KStem/WT${WT}/${model}.features"
  printf "%s\n" ${path}
  cat ${path} >> ${out}
 done

done