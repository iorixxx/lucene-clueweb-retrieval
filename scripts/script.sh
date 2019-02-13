#!/usr/bin/env bash

if [ -z "$TFD_HOME" ]; then
 TFD_HOME=~/TFD_HOME
fi


for set in GOV2 CW09A CW12A; do
mkdir -p "$set"
for script in Jpan Cyrillic Greek Arabic Hangul Thai Armenian Devanagari Hebrew Georgian; do
./run.sh High  -collection "$set" -tag Script -field "$script" > "${set}/${script}.txt"
done
done