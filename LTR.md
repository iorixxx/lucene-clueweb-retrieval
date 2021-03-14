# Learning to Rank Experiments

This library can produce a features file that is compatible with learning to rank libraries such as RankLib and RankSVM.

## Extraction of Type-Q

Not all learning to rank algorithms can handle this type of features.
Pre-retrieval query performance predictors.
Running following tools is a pre-requisite.

```
./run.sh Indexer -collection CW09B
./run.sh Optimize -collection CW09B
./run.sh Doclen -collection CW09B
./run.sh Stats -collection CW09B
./run.sh Stats -collection CW09B -task query
./run.sh TFDistribution -collection CW09B -task query
./run.sh TFDistribution -collection CW09B -task term
```

Following command dumps query features in the format: qid:1   0.6     0.4715      0.0896

```
./run.sh Feature -collection CW09B
```

## Extraction of Type-QD

```
./run.sh Indexer -collection CW09B -tag KStem
./run.sh Indexer -collection CW09B -tag KStem -field
```
To extract Weighting Model Whole Document (WMWD) features
```
./run.sh Searcher -collection CW09B -task feature -tag KStem
```
To extract Weighting Model Single Field (WMSF) features
```
./run.sh Searcher -collection CW09B -task field -tag KStem
```
The eight models are: BM25, LGD 
Fields: ULR, title, body, anchor

To be implemented WMFB

WMWD: Multiple standard weighting models computed on whole documents
WMSF: A standard weighting model calculated individually on each “single field” of the document. 
WMFB: A weighting model that is field-based, where term frequencies are combined rather than the weighting model scores.

The above notations are borrowed from "About Learning Models with Multiple Query Dependent Features"

This step appends (row-based) different files into a single file.
manually copy *.features files of individual tracks in to TFD_HOME/MQ09/ModelName.features or
` ./merge.sh `
This step merges WMWD features and WMSF features in a column oriented way.
```
./run.sh Sample -collection CW09B -task merge
```
This step concludes the extraction of Type-QD features.

## Extraction of Type-D

This family of features are document priors.
The document prior is the probability that the document is relevant to *any* query.
At the moment we have PageRank and SpamRankings.
We store them in Apache Solr for fast lookup.
```
./run.sh Sample -collection CW09B -task spam
```
The last command will produce TFD_HOME/MQ09/X.ModelName.features files.