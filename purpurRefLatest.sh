#!/bin/bash
set -e

# https://gist.github.com/kongchen/6748525
setProperty(){
  awk -v pat="^$1=" -v value="$1=$2" '{ if ($0 ~ pat) print value; else print $0; }' $3 > $3.tmp
  mv $3.tmp $3
}

SHA=$(curl https://api.github.com/repos/PurpurMC/Purpur/commits/ver/1.21.11 | jq -r .sha)
setProperty purpurRef $SHA gradle.properties

echo $SHA