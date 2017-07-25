#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
  TFD_HOME=~/TFD_HOME
fi

if [ "$1" = "parameter" ]; then
	RUNS=parameter_runs
    EVALS=parameter_evals
else
	RUNS=runs
    EVALS=evals
fi

echo "Starting Milliyet Collection evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

qrels[4]=mcqrels.txt

for set in MC; do
if [ ! -d "${TFD_HOME}/${set}/${RUNS}" ]; then
       continue
fi
for tag in ${TFD_HOME}/${set}/${RUNS}/*; do
if [[ ! -d ${tag} ]]; then
    continue
fi
tag=$(basename "${tag}")
mkdir -p "${TFD_HOME}/$set/${EVALS}/$tag"

if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/MC" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/MC does not exist!"
        continue
    fi

     mkdir -p "${TFD_HOME}/${set}/${EVALS}/$tag/MC"
     mkdir -p "${TFD_HOME}/${set}/${EVALS}/$tag/MC/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/MC/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/mcqrels.txt ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/MC/trec_eval/${f##/*/}" &
        for k in 20 100 1000; do
          mkdir -p "${TFD_HOME}/${set}/${EVALS}/$tag/MC/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k $k ${TFD_HOME}/topics-and-qrels/mcqrels.txt $f > "${TFD_HOME}/${set}/${EVALS}/${tag}/MC/$k/${f##/*/}" &
        done
    done
     wait

done
done