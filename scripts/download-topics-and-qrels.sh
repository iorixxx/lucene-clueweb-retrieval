#!/usr/bin/env bash


# Tasks Track 2015
curl -o 2015-qrels-docs.txt https://trec.nist.gov/data/tasks/qrels-docs.txt
curl -o tasks_track_queries_2015.xml https://trec.nist.gov/data/tasks/final_tasks.xml

# Tasks Track 2016
curl -o 2016-qrels-docs.txt https://trec.nist.gov/data/tasks/2016-qrels-docs.txt
curl -o tasks_track_queries_2016.xml https://trec.nist.gov/data/tasks/tasks_track_queries_2016.xml