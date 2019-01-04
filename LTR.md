# Learning to Rank Experiments

This library can produce a features file that is acceptable by learning to rank libraries such as RankLib and RankSVM.


## Feature extraction

` ./run.sh Searcher -collection MQ09 -task feature -tag KStem `
` ./run.sh Searcher -collection MQ09 -task field -tag KStem `
manually copy *. features files of individual tracks in to TFD_HOME/MQ09/ModelName.features or
` ./merge.sh `
` ./run.sh Sample -collection MQ09 -task merge `
` ./run.sh Sample -collection MQ09 -task spam `
The last command will produce TFD_HOME/MQ09/X.ModelName.features files.