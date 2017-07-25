#!/usr/bin/env bash
mvn clean package dependency:copy-dependencies assembly:single
