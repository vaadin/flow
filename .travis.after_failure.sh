#!/bin/bash
FILES=hummingbird-tests/*/error-screenshots/*.png
for file in $FILES
do
  echo $file
  base64 < $file
  echo
done
