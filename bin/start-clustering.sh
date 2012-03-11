#!/bin/sh

DIR=$(dirname "$0")

cd ${DIR}/../apache-solr/example

exec java \
  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 \
  -Dsolr.solr.home=../../solr-clustering \
  -Dsolr.data.dir=../../solr-clustering/data \
  -Dmodel.dir=../../opennlp-models \
  -jar start.jar
