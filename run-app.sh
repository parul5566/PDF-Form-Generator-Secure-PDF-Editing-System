#!/bin/bash
set -e
cd /workspace
if command -v mysql >/dev/null 2>&1; then :; fi
mvn -q -DskipTests package 2>&1 | tail -5
java -jar target/pdf-form-generator-1.0.0.jar
