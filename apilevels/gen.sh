#!/bin/bash
if [ $# -ne 1 ]; then
  echo "Usage: gen.sh path/to/android/sdk"
  exit
fi
for d in $1/platforms/*; do
  p=$(basename "$d")
  echo "converting $p"
  dx --dex --core-library --output=$p.dex $d/android.jar
done
echo "Done"

