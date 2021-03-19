#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
    TFD_HOME=~/TFD_HOME
fi

if [ -z "$1" ]; then
echo "Please enter a collection, which is a required parameter."
  # Terminate our shell script with success message
  exit 0
else

  echo "starting processes with collection = $1 ..."
  for tag in NoStem KStem Snowball Hunspell; do
    ./run.sh Indexer -collection "$1" -tag "$tag" -silent
  done
  ./run.sh Optimize -collection "$1"
  ./run.sh Doclen -collection "$1"
  ./run.sh Stats -collection "$1"
  ./run.sh Stats -collection "$1" -task query
  ./run.sh TFDistribution -collection "$1" -task query
  ./run.sh TFDistribution -collection "$1" -task term

fi