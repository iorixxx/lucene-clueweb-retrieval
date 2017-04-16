#!/usr/bin/env bash
for i in $(find FR94 -type f -name '*.?Z');
do
	filename="${i%.*}"
    echo "$filename"
    gzip -dc "$i" >> "$filename"
    rm "$i"
done