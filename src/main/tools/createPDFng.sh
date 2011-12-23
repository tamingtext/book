#!/bin/sh

# Get the fully qualified path to the script
case $0 in
    /*) SCRIPT="$0" ;;
     *) SCRIPT="`pwd`/$0" ;;
esac

SCRIPT_PATH=$(dirname $SCRIPT)/

INPUT="$1"; shift;
OUTPUT="$1"; shift;
LIB="$1";

if [ "$LIB" = "" ]; then
  LIBDIR=$SCRIPT_PATH/lib
else
  LIBDIR=$LIB/lib
fi

java -Djava.ext.dirs=$LIBDIR AAPDFMaker $INPUT $OUTPUT $SCRIPT_PATH
