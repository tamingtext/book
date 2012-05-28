/*
 * Copyright 2008-2011 Grant Ingersoll, Thomas Morton and Drew Farris
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * -------------------
 * To purchase or learn more about Taming Text, by Grant Ingersoll, Thomas Morton and Drew Farris, visit
 * http://www.manning.com/ingersoll
 */

package com.tamingtext.texttamer.solr;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.solr.analysis.BaseTokenFilterFactory;

import com.tamingtext.util.NameFinderFactory;

public class NameFilterFactory extends BaseTokenFilterFactory {
  private NameFinderFactory factory;

  public void init(Map<String, String> args) {
    super.init(args);

    try {
      factory = new NameFinderFactory(args);
    }
    catch (IOException e) {
      throw (RuntimeException) new RuntimeException().initCause(e);
    }
  }

  public NameFilter create(TokenStream ts) {
    return new NameFilter(ts,
        factory.getModelNames(), factory.getNameFinders());
  }
}
