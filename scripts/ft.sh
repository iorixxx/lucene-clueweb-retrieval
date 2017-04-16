#!/usr/bin/env bash
for i in $(find FT -type f -name '*.Z');
do
    gzip -d "$i" 
done