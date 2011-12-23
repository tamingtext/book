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

package com.tamingtext.snowball;

import com.tamingtext.TamingTextTestJ4;
import junit.framework.TestCase;
import org.junit.*;
import org.tartarus.snowball.ext.EnglishStemmer;

public class SnowballStemmerTest extends TamingTextTestJ4 {

  @Test
  public void test() throws NoSuchMethodException {
    //<start id="stemmer"/>
    EnglishStemmer english = new EnglishStemmer();

    String[] test = {"bank", "banks", "banking", "banker", "banked", "bankers"};//<co id="stemmer.co.test"/>
    String[] gold = {"bank", "bank", "bank", "banker", "bank", "banker"};//<co id="stemmer.co.gold"/>
    for (int i = 0; i < test.length; i++) {
      english.setCurrent(test[i]);//<co id="stemmer.co.set"/>
      english.stem();//<co id="stemmer.co.stem"/>
      System.out.println("English: " + english.getCurrent());
      assertTrue(english.getCurrent() + " is not equal to " + gold[i], english.getCurrent().equals(gold[i]) == true);
    }
    /*
<calloutlist>
<callout arearefs="stemmer.co.test"><para>Setup some tokens to be stemmed</para></callout>
<callout arearefs="stemmer.co.gold"><para>Define our expectations for results</para></callout>
<callout arearefs="stemmer.co.set"><para>Tell the english what to stem</para></callout>
<callout arearefs="stemmer.co.stem"><para>Do the stemming</para></callout>
</calloutlist>
    */
    //<end id="stemmer"/>

  }


}