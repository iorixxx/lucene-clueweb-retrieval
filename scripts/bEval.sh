#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
    TFD_HOME=/home/iorixxx/TFD_HOME
fi

export LC_ALL=en_US.UTF-8
export LANGUAGE=en_US.UTF-8

if [ -z "$1" ]; then
	RUNS=runs
    EVALS=catb_evals
else
	RUNS="$1_runs"
    EVALS="$1_catb_evals"
fi

echo "starting evaluator with RUNS = $RUNS and EVALS = $EVALS ..."

 qrels[9]=09.qrels.adhoc.catB
 qrels[10]=10.qrels.adhoc.catB
 qrels[11]=11.qrels.adhoc.catB
 qrels[12]=12.qrels.adhoc.catB
 qrels[13]=13.qrels.adhoc.catB
 qrels[14]=14.qrels.adhoc.catB

for WT in 09 10 11 12 13 14; do
 printf "%s\n" ${qrels[${WT#0}]}
done


for set in CW09B; do
for tag in KStemAnalyzer KStemAnalyzerAnchor; do
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
for WT in 09 10 11 12; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT} does not exist!"
        continue
    fi

    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT"
    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${WT#0}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/trec_eval/${f##/*/}" &
        for k in 20 100 1000; do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${WT#0}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/${k}/${f##/*/}" &
        done
    done
    wait

done
done
done

set=CW12B
for tag in KStemAnalyzer KStemAnalyzerAnchor; do
mkdir -p "$TFD_HOME/$set/${EVALS}/$tag"
for WT in 13 14; do

    if [ ! -d "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}" ]; then
        # Control will enter here if $DIRECTORY does not exist.
        echo "${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT} does not exist!"
        continue
    fi

    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT"
    mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/trec_eval"

    for f in ${TFD_HOME}/${set}/${RUNS}/${tag}/WT${WT}/*.txt; do
        ${TFD_HOME}/scripts/trec_eval -M1000 -q ${TFD_HOME}/topics-and-qrels/${qrels[${WT}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/trec_eval/${f##/*/}" &
        for k in 20 100 1000; do
          mkdir -p "$TFD_HOME/$set/${EVALS}/$tag/WT$WT/$k"
          ${TFD_HOME}/scripts/gdeval.pl -k ${k} ${TFD_HOME}/topics-and-qrels/${qrels[${WT}]} ${f} > "${TFD_HOME}/${set}/${EVALS}/${tag}/WT${WT}/${k}/${f##/*/}" &
        done
    done
    wait

done
done
wait

