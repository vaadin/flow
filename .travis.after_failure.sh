#!/usr/bin/env bash

FILES=flow-tests/*/error-screenshots/*
for file in $FILES
do
  echo $file
  base64 < $file
  echo
done
