#!/usr/bin/env bash

# index ClueWeb09 collection, removing spam documents during indexing.

for i in {0..90..10}
  do
     echo "calling SpamRemove with spam_$i argument"
   ./run.sh SpamRemove -collection CW09B -spam "$i"
 done

