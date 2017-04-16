#!/usr/bin/env bash

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
  else
    JAVACMD="`which java`"
  fi
fi

$JAVACMD -cp "repo/*:*" -server -Xms10g -Xmx20g edu.anadolu.cmdline.CLI $@
