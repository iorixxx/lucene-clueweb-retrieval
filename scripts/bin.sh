#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi

for set in ROB04 GOV2 MQ09 CW09A CW09B CW12B; do

    if [ -d  "$TFD_HOME"/${set}/binary ]; then
        rm -rf  "$TFD_HOME"/${set}/binary
    fi

    mkdir -p "$TFD_HOME"/${set}/binary
done

for test in ROB04 GOV2 MQ09 CW09A CW09B CW12B; do
    for models in BM25_DFIC BM25_DLH13 BM25_PL2 BM25_LGD BM25_Dirichlet BM25_DPH BM25_DFRee DFIC_DLH13 DFIC_PL2 DFIC_LGD DFIC_Dirichlet DFIC_DPH DFIC_DFRee DLH13_PL2 DLH13_LGD DLH13_Dirichlet DLH13_DPH DLH13_DFRee PL2_LGD PL2_Dirichlet PL2_DPH PL2_DFRee LGD_Dirichlet LGD_DPH LGD_DFRee Dirichlet_DPH Dirichlet_DFRee DPH_DFRee; do
        ./run.sh Binary -test "$test" -train "$test" -models "$models" -optimize NDCG100 -report NDCG100
    done
done