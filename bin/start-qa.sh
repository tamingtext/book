#!/bin/sh

DIR=$(dirname "$0")

cd ${DIR}/../apache-solr/example

# expects symlinks to opennlp-models and WordNet-3.0 in the dist directory

exec java \
  -Xmx1024M -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 \
  -Dsolr.solr.home=../../solr-qa \
  -Dsolr.data.dir=../../solr-qa/data \
  -Dmodel.dir=../../opennlp-models \
  -Dwordnet.dir=../../WordNet-3.0 \
  -jar start.jar
