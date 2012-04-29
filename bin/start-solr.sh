#!/bin/sh

#
# start-solr.sh: start a solr instance providing a solr instance name
#  
DIR=$(dirname "$0")
cd ${DIR}/../apache-solr/example

INSTANCE=$1;

if [ "x$INSTANCE" == "x" ]; then
  echo "Usage: start-solr.sh (instance), where instance is one of:"
  echo "    solr-qa, solr-clustering, solr-tagging"
  exit
fi

IDIR=$(readlink "../$INSTANCE");

if [ ! -d $IDIR ]; then
  echo "Instance $INSTANCE does not exist in $IDIR"
  exit
fi

# expects symlinks to opennlp-models and WordNet-3.0 in the dist directory
# or tamingText-src directory depending on where this is running from

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044"

exec java \
  -Xmx1024M $DEBUG_OPTS \
  -Dsolr.solr.home=../${INSTANCE} \
  -Dsolr.data.dir=../${INSTANCE}/data \
  -Dmodel.dir=../../opennlp-models \
  -Dwordnet.dir=../../WordNet-3.0 \
  -jar start.jar
