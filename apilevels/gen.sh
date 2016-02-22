#!/usr/bin/bash
echo "Input the path to the android SDK"
read SDK
for d in $xSDK/*; do dx --dex --core-library --output=$(basename "$d").dex $d/android.jar; done
echo "aLL api levels:"
ls -l *.dex

