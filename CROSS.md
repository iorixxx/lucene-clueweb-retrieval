# Retrieval experiments in which hyper parameters of models (BM25,PL2,LGD,DLM) are set to default (Terrier's) or custom provided.

Useful when you need to bypass training phase of hyper parameters. Instead supply manually.

# Indexer

./run.sh Indexer -collection GOV2 -tag ICU
./run.sh Indexer -collection GOV2 -tag Latin

./run.sh Indexer -collection MQ09 -tag ICU
./run.sh Indexer -collection MQ09 -tag Latin

# Searcher

./run.sh Custom -collection MQ09 -tag KStem -task search