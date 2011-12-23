#!/bin/sh

java \
  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 \
  -Dsolr.solr.home=../../src/main/resources/solr-qa \
  -Dsolr.data.dir=../../src/main/resources/solr-qa/data/ \
  -Dmodel.dir=../../../opennlp/models-1.4 \
  -jar start.jar
