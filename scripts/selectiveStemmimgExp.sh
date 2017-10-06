#!/usr/bin/env bash

collection=MQ09
tags=NoStem_KStem
metrics=(ERR20 ERR100 ERR100 MAP NDCG20 NDCG100)
selectionMethods=(MSTTF MSTDF TFOrder DFOrder KendallTauTFOrder KendallTauDFOrder MSTTFBinning MSTDFBinning TFOrderBinning DFOrderBinning KendallTauTFOrderBinning KendallTauDFOrderBinning)
kendallTauThreshold=(55 65 75 85 95)

for m in ${metrics[*]}; do
	for s in ${selectionMethods[*]}; do
		if [[ ${s} == KendallTau* ]]; then
			for th in ${kendallTauThreshold[*]}; do
				./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -metric ${m} -selection ${s} -KTT ${th} > ${collection}_${m}_${s}_${th}.txt
			done
		else
			./run.sh SelectiveStemming -collection ${collection} -tags ${tags} -metric ${m} -selection ${s} > ${collection}_${m}_${s}.txt
		fi
		
	done
	
done
