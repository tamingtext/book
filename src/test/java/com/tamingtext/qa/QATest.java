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

package com.tamingtext.qa;


import com.tamingtext.TTTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * http://www.manning.com/ingersoll
 *
 **/
public class QATest extends TTTestCaseJ4 {
  @BeforeClass
  public static void beforeClass() throws Exception {

    initCore("solrconfig-qa.xml",
            "schema-qa.xml");


  }

  public static final String[] DOCS = {
          "Basketball is a sport played with a ball and two hoops.  There are 5 players on a team. Michael Jordan is the greatest basketball player of all time.  " +
                  "Basketball is played all around the world, but is most popular in the United States.",
          "Hockey is a sport played on ice with a puck, 6 players on a side and two nets.  Each team has a goalie that defends their net, trying to prevent the other team from scoring. " +
                  "Wayne Gretzky is the greatest hockey player of all time.  Mario Lemieux is the second best player of all time.  " +
                  "Hockey is one of Canada's most popular sports, but it is also popular in many northern areas of the United States as well as Europe.",
          "Golf is an individual sport where the goal is to hit a little ball into a hole that is usually somewhere between 100 and 600 yards away.  " +
                  "Jack Nicklaus is the greatest golfer of all time. Tiger Woods and Arnold Palmer are close seconds.  Golf is played all around the world.  It is especially popular in the United States and the United Kingdom.",
          "Animals are a lot of fun to watch, especially young animals that play.  Cats are mammals.  Dogs are also mammals.  Cats and dogs don't usually get along, but sometimes they do.  Lizards are not mammals.",
          "Ostriches cannot fly.  Owls, on the other hand, can fly."
  };

  @Test
  public void testComponent() throws Exception {

    for (int i = 0; i < DOCS.length; i++) {
      String doc = DOCS[i];
      assertU("Add a doc to be ranked",
              adoc("id", String.valueOf(i),
                      "details", doc));
    }

    assertU(commit());

    assertQ("Couldn't find query",
            req("q", "Who is the greatest hockey player of all time?"
                    ), "//result[@numFound=1]",
            "//str[@name='id'][.='1']");

        assertQ("Couldn't find query",
            req("q", "Who is the greatest basketball player of all time?",
                    QAParams.PRIMARY_WINDOW_SIZE, "10"
                    ), "//result[@numFound=1]",
            "//str[@name='id'][.='0']",
                "//str[@name='field'][.='details']"

                );

        assertQ("Couldn't find query",
            req("q", "Are cats mammals?",
                    QAParams.PRIMARY_WINDOW_SIZE, "5"
                    ), "//result[@numFound=1]",
            "//str[@name='id'][.='3']",
                "//str[@name='field'][.='details']"
        );




  }
}
