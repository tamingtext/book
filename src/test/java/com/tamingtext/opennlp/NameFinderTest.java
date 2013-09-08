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

package com.tamingtext.opennlp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.NameSample;
import opennlp.tools.namefind.NameSampleDataStream;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

import org.junit.Test;

import com.tamingtext.TamingTextTestJ4;
import com.tamingtext.util.MemoryStatus;

public class NameFinderTest extends TamingTextTestJ4 {


//<start id="ne-display1"/>
  private void displayNames(Span[] names, String[] tokens) {
    for (int si = 0; si < names.length; si++) { //<co id="co.opennlp.name.eachname"/>
      StringBuilder cb = new StringBuilder();
      for (int ti = names[si].getStart(); ti < names[si].getEnd(); ti++) {
        cb.append(tokens[ti]).append(" "); //<co id="co.opennlp.name.eachtoken"/>
      }
      System.out.println(cb.substring(0, cb.length() - 1)); //<co id="co.opennlp.name.extra"/>
      System.out.println("\ttype: " + names[si].getType());
    }
  }
/*<calloutlist>
<callout arearefs="co.opennlp.name.eachname"><para>Iterate over each name.</para></callout>
<callout arearefs="co.opennlp.name.eachtoken"><para>Iterate over each token in the name.</para></callout>
<callout arearefs="co.opennlp.name.extra"><para>Remove the extra space at the end of the name and print.</para></callout>
</calloutlist>*/
//<end id="ne-display1"/>

  //private Span[] mergeSpans(Span[][] spans) {
  //  return null;
  //}

  //<start id="ne-remove-conflicts"/>
  private void removeConflicts(List<Annotation> allAnnotations) {
    if (allAnnotations.size() < 2) return; //<co id="co.opennlp.name.earlyreturn"/>
    java.util.Collections.sort(allAnnotations); //<co id="co.opennlp.name.sort"/>
    List<Annotation> stack = new ArrayList<Annotation>(); //<co id="co.opennlp.name.stack"/>
    stack.add(allAnnotations.get(0));
    for (int ai = 1; ai < allAnnotations.size(); ai++) { //<co id="co.opennlp.name.eachname2"/>
      Annotation curr = (Annotation) allAnnotations.get(ai);
      boolean deleteCurr = false;
      for (int ki = stack.size() - 1; ki >= 0; ki--) { //<co id="co.opennlp.name.eachstack"/>
        Annotation prev = (Annotation) stack.get(ki);
        if (prev.getSpan().equals(curr.getSpan())) { //<co id="co.opennlp.name.isequal"/>
          if (prev.getProb() > curr.getProb()) {
            deleteCurr = true;
            break;
          } else {
            allAnnotations.remove(stack.remove(ki));
            ai--;  //<co id="co.opennlp.name.change4delete"/> 
          }
        } else if (prev.getSpan().intersects(curr.getSpan())) { //<co id="co.opennlp.name.iscrossing"/>
          if (prev.getProb() > curr.getProb()) {
            deleteCurr = true;
            break;
          } else {
            allAnnotations.remove(stack.remove(ki));
            ai--;  //<co id="co.opennlp.name.change4delete2"/> 
          }
        } else if (prev.getSpan().contains(curr.getSpan())) { //<co id="co.opennlp.name.issubsumed"/>
          break;
        } else { //<co id="co.opennlp.name.ispast"/>
          stack.remove(ki);
        }
      }
      if (deleteCurr) {
        allAnnotations.remove(ai);
        ai--; //<co id="co.opennlp.name.change4delete3"/> 
        deleteCurr = false;
      } else {
        stack.add(curr);
      }
    }
  }

  /*
  <calloutlist>
  <callout arearefs="co.opennlp.name.earlyreturn"><para>Exit early if there will be no conflicts.</para></callout>
  <callout arearefs="co.opennlp.name.sort"><para>Sort the names based on their span's start index ascending then end index decending.</para></callout>
  <callout arearefs="co.opennlp.name.stack"><para>Initialize a stack to keep track of previous names.</para></callout>
  <callout arearefs="co.opennlp.name.eachname2"><para>Iterate over each name.</para></callout>
  <callout arearefs="co.opennlp.name.eachstack"><para>Iterate over each item in the stack.</para></callout>
  <callout arearefs="co.opennlp.name.isequal"><para>Test if a name span is identical to another name span, and if so remove the less probable one.</para></callout>
  <callout arearefs="co.opennlp.name.change4delete co.opennlp.name.change4delete2 co.opennlp.name.change4delete3"><para>Update index of name after deletion to negate ai++ at end of for-loop.</para></callout>
  <callout arearefs="co.opennlp.name.iscrossing"><para>Test if a name span is over-lapping another name span, and if so remove the less probable one.</para></callout>
  <callout arearefs="co.opennlp.name.issubsumed"><para>Test if a name span is subsumed by another name span, and if so exit loop.</para></callout>
  <callout arearefs="co.opennlp.name.ispast"><para>Test if a name span is past another name span, and if so remove previous name from the stack.</para></callout>
  </calloutlist>
  */
  //<end id="ne-remove-conflicts"/>
  @Test
  public void testRemoveConflicts() {
    List<Annotation> annotations = new ArrayList<Annotation>();
    annotations.add(new Annotation("person", new Span(1, 5), 0.75));
    annotations.add(new Annotation("person", new Span(7, 10), 0.95));
    annotations.add(new Annotation("location", new Span(11, 15), 0.85));
    removeConflicts(annotations);
    assertTrue(annotations.size() == 3);
    annotations.add(new Annotation("location", new Span(2, 7), 0.85));
    removeConflicts(annotations);
    assertTrue(annotations.size() == 3);
    assertTrue(((Annotation) annotations.get(0)).getSpan().getStart() == 2);
    annotations.clear();
    annotations.add(new Annotation("person", new Span(1, 5), 0.75));
    annotations.add(new Annotation("person", new Span(7, 10), 0.95));
    annotations.add(new Annotation("location", new Span(11, 15), 0.85));
    annotations.add(new Annotation("person", new Span(3, 8), 0.85));
    removeConflicts(annotations);
    assertTrue(annotations.size() == 2);
    assertTrue(((Annotation) annotations.get(0)).getSpan().getStart() == 7);
  }

  public void multiModel() throws IOException {

    File modelDir = getModelDir();
    //<start id="ne-multi"/>    
    String[] sentences = {
      "Former first lady Nancy Reagan was taken to a " +
              "suburban Los Angeles " +
      "hospital \"as a precaution\" Sunday after a fall at " +
              "her home, an " +
      "aide said. ",
      "The 86-year-old Reagan will remain overnight for " +
      "observation at a hospital in Santa Monica, California, " +
              "said Joanne " +
      "Drake, chief of staff for the Reagan Foundation."};
    NameFinderME[] finders = new NameFinderME[3];
    String[] names = {"person", "location", "date"};
    for (int mi = 0; mi < names.length; mi++) {  //<co id="co.opennlp.name.1"/>
      finders[mi] = new NameFinderME(new TokenNameFinderModel(
          new FileInputStream(
              new File(modelDir, "en-ner-" + names[mi] + ".bin")
          )));
    }

    Tokenizer tokenizer = SimpleTokenizer.INSTANCE; //<co id="co.opennlp.name.2"/>
    for (int si = 0; si < sentences.length; si++) { //<co id="co.opennlp.name.3"/>
      List<Annotation> allAnnotations = new ArrayList<Annotation>();
      String[] tokens = tokenizer.tokenize(sentences[si]);//<co id="co.opennlp.name.4"/>
      for (int fi = 0; fi < finders.length; fi++) { //<co id="co.opennlp.name.5"/>
        Span[] spans = finders[fi].find(tokens); //<co id="co.opennlp.name.6"/>
        double[] probs = finders[fi].probs(spans); //<co id="co.opennlp.name.7"/>
        for (int ni = 0; ni < spans.length; ni++) {
          allAnnotations.add( //<co id="co.opennlp.name.8"/>
              new Annotation(names[fi], spans[ni], probs[ni])
          ); 
        }
      }
      removeConflicts(allAnnotations); //<co id="co.opennlp.name.9"/>
    }
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.1">
      <para>Initialize a new model for identifying people, locations, and dates
        based on the binary compressed model in the files "en-ner-person.bin",
        "en-ner-location.bin", "en-ner-date.bin".
      </para>
    </callout>
    <callout arearefs="co.opennlp.name.2">
      <para>Obtain a reference to a tokenizer to split the sentence into 
        individual words and symbols.
      </para>
    </callout>
    <callout arearefs="co.opennlp.name.3">
      <para>Iterate over each sentence.</para>
    </callout>
    <callout arearefs="co.opennlp.name.4">
      <para>Split the sentence into an array of tokens.</para>
    </callout>
    <callout arearefs="co.opennlp.name.5">
      <para>Iterate over each of the name finders (person, location, date).</para>
    </callout>
    <callout arearefs="co.opennlp.name.6">
      <para>Identify the names in the sentence and return token-based offsets
         to these names.</para>
    </callout>
    <callout arearefs="co.opennlp.name.7">
      <para>Get the probabilities associated with the associated matches.</para>
    </callout>
    <callout arearefs="co.opennlp.name.8">
      <para>Collect each of the identified names from each of the name 
        finders.</para></callout>
    <callout arearefs="co.opennlp.name.9">
      <para>Resolve any cases of overlapping names in favor of the more 
        probable name.</para></callout>
    </calloutlist>*/
    //<end id="ne-multi"/>

  }

  @Test
  public void testMultiNameSamples() throws IOException {
    File destDir = new File("target");
    
    //<start id="ne-namesample-type"/>
    String taggedSent = 
      "<START:person> Britney Spears <END> was reunited " +
      "with her sons <START:date> Saturday <END> ";
    ObjectStream<NameSample> nss = new NameSampleDataStream(
        new PlainTextByLineStream(new StringReader(taggedSent)));
    TokenNameFinderModel model = NameFinderME.train( 
        "en", 
        "default" ,
        nss, 
        (AdaptiveFeatureGenerator) null,
        Collections.<String,Object>emptyMap(),
        70 , 1 );
    
    File outFile = new File(destDir,"multi-custom.bin");
    FileOutputStream outFileStream = new FileOutputStream(outFile);
    model.serialize(outFileStream); 
    
    NameFinderME nameFinder = new NameFinderME(model);
    
    String[] tokens = 
        (" Britney Spears was reunited with her sons Saturday .")
        .split("\\s+");
    Span[] names = nameFinder.find(tokens);
    displayNames(names, tokens);
    //<end id="ne-namesample-type"/>
    
    assertEquals("person", names[0].getType());
    assertEquals("date", names[1].getType());
  }

  @Test
  public void testMemoryUsageNonPooled() throws IOException {
    File modelDir = getModelDir();
    MemoryStatus memStatus = new MemoryStatus();
    memStatus.dumpMemory("before non-pooled model load");
    //String[] names = {"person"};
    //String[] names = {"date","location","money","organization","percentage","person","time"};
    String[] names = {"person","location","date"};
    NameFinderME[] finders = new NameFinderME[names.length];
    for (int mi = 0; mi < names.length; mi++) {
      finders[mi] = new NameFinderME(new TokenNameFinderModel(
          new FileInputStream(
              new File(modelDir, "en-ner-" + names[mi] + ".bin")
              )));
    }
    memStatus.dumpMemory("after non-pooled model load of " + Arrays.toString(names));
    
    //    ----------before non-pooled model load----------
    //    Code Cache 511.88 KBytes
    //    Par Eden Space 6.32 MBytes
    //    Par Survivor Space 0.00 Bytes
    //    CMS Old Gen 0.00 Bytes
    //    CMS Perm Gen 5.88 MBytes
    //    Total 12.70 MBytes
    //    ---------------------------------
    //    ----------after non-pooled model load of person, money, date----------
    //    Code Cache 622.19 KBytes
    //    Par Eden Space 4.29 MBytes
    //    Par Survivor Space 3.19 MBytes
    //    CMS Old Gen 142.21 MBytes
    //    CMS Perm Gen 6.22 MBytes
    //    Total 156.51 MBytes
    //    ---------------------------------
  }
  
  @Test
  public void testMemoryUsagePooled() throws IOException {
    File modelDir = getModelDir();
    MemoryStatus memStatus = new MemoryStatus();
    memStatus.dumpMemory("before pooled model load");
    //String[] names = {"person"};
    //String[] names = {"date","location","money","organization","percentage","person","time"};
    //<start id="ne-pool"/>
    String[] names = {"person","location","date"};
    NameFinderME[] finders = new NameFinderME[names.length];
    for (int mi = 0; mi < names.length; mi++) { //<co id="co.opennlp.name.init4"/>
      finders[mi] = new NameFinderME(
        new PooledTokenNameFinderModel( //<co id="co.opennlp.name.pool"/>
          new FileInputStream(
              new File(modelDir, "en-ner-"
                      + names[mi] + ".bin"))));
    }
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.init4"><para>Initialize name finders for identifying people, locations, and dates</para></callout>
    <callout arearefs="co.opennlp.name.pool"><para>Use the string-pooling model to reduce memory footprint.</para></callout>
    </calloutlist>*/
    //<end id="ne-pool"/>
    memStatus.dumpMemory("after pooled model load of " + Arrays.toString(names));
    
    //    ----------before pooled model load----------
    //    Code Cache 514.13 KBytes
    //    Par Eden Space 6.18 MBytes
    //    Par Survivor Space 0.00 Bytes
    //    CMS Old Gen 0.00 Bytes
    //    CMS Perm Gen 5.88 MBytes
    //    Total 12.57 MBytes
    //    ---------------------------------
    //    ----------after pooled model load----------
    //    Code Cache 626.75 KBytes
    //    Par Eden Space 7.16 MBytes
    //    Par Survivor Space 2.06 MBytes
    //    CMS Old Gen 61.59 MBytes
    //    CMS Perm Gen 32.95 MBytes
    //    Total 104.37 MBytes
    //    ---------------------------------
  }

  @Test
  public void trainNameFinder() throws IOException {
    File baseDir = new File("src/test/resources");
    File destDir = new File("target");
    //<start id="ne-train"/>
    File inFile = new File(baseDir,"person.train");
    NameSampleDataStream nss = new NameSampleDataStream( //<co id="co.opennlp.name.initnamestream"/>
      new PlainTextByLineStream(
        new java.io.FileReader(inFile)));

    int iterations = 100;
    int cutoff = 5;
    TokenNameFinderModel model = NameFinderME.train( //<co id="co.opennlp.name.train"/>
        "en", // language
        "person", // type
        nss, 
        (AdaptiveFeatureGenerator) null,
        Collections.<String,Object>emptyMap(),
        iterations,
        cutoff);
    
    File outFile = new File(destDir, "person-custom.bin");
    FileOutputStream outFileStream = new FileOutputStream(outFile);
    model.serialize(outFileStream); //<co id="co.opennlp.name.persist3"/>
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.initnamestream"><para>Create a stream of name samples based on annotated data in the "person.train" file.</para></callout>
    <callout arearefs="co.opennlp.name.train"><para>Train the model.</para></callout>
    <callout arearefs="co.opennlp.name.persist3"><para>Save the model to a file.</para></callout>
    </calloutlist>*/

    //<end id="ne-train"/>
  }

  @Test
  @SuppressWarnings("unused")
  public void trainNameFinderWithCustomFeatures() throws IOException {
    File baseDir = new File("src/test/resources");
    File destDir = new File("target");
    
    //<start id="ne-features"/>    
    AggregatedFeatureGenerator featureGenerators = 
      new AggregatedFeatureGenerator( //<co id="co.opennlp.name.createfeat"/>
        new WindowFeatureGenerator(
          new TokenFeatureGenerator(), 2, 2), //<co id="co.opennlp.name.tokenfeat"/>
        new WindowFeatureGenerator(
          new TokenClassFeatureGenerator(), 2, 2), //<co id="co.opennlp.name.tokenclassfeat"/>
        new PreviousMapFeatureGenerator() //<co id="co.opennlp.name.prevfeat"/>
      );  
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.createfeat"><para>Creates an aggregated feature generator containing the 3 generators defined below.</para></callout>
    <callout arearefs="co.opennlp.name.tokenfeat"><para>Creates a feature generator corresponding to the tokens in a 5-token widow (2 to the left, and 2 to the right).</para></callout>
    <callout arearefs="co.opennlp.name.tokenclassfeat"><para>Creates a feature generator corresponding to the token classes of the tokens in a 5-token widow (2 to the left, and 2 to the right).</para></callout>
    <callout arearefs="co.opennlp.name.prevfeat"><para>Creates a feature generator which specifies how this token was previously tagged.</para></callout>     
    </calloutlist>*/
    //<end id="ne-features"/>

    //<start id="ne-features-train"/>
    File inFile = new File(baseDir,"person.train");
    NameSampleDataStream nss = new NameSampleDataStream( //<co id="co.opennlp.name.initfeat"/>
      new PlainTextByLineStream(
        new java.io.FileReader(inFile)));

    int iterations = 100;
    int cutoff = 5;
    TokenNameFinderModel model = NameFinderME.train( //<co id="co.opennlp.name.train2"/>
        "en", // language
        "person", // type
        nss, 
        featureGenerators, 
        Collections.<String,Object>emptyMap(),
        iterations, 
        cutoff);

    File outFile = new File(destDir,"person-custom2.bin");
    FileOutputStream outFileStream = new FileOutputStream(outFile);
    model.serialize(outFileStream); //<co id="co.opennlp.name.persist2"/>
    /*<calloutlist>
   <callout arearefs="co.opennlp.name.initfeat"><para>Create the sample stream..</para></callout>
   <callout arearefs="co.opennlp.name.train2"><para>Train the model with a custom feature generator.</para></callout>
   <callout arearefs="co.opennlp.name.persist2"><para>Save the model to a file.</para></callout>
   </calloutlist>*/
    //<end id="ne-features-train"/>
    
    //<start id="ne-features-test"/>
    NameFinderME finder = new NameFinderME(
        new TokenNameFinderModel(
            new FileInputStream(
                new File(destDir, "person-custom2.bin")
                )), featureGenerators, NameFinderME.DEFAULT_BEAM_SIZE);
    //<end id="ne-features-test"/>
  }

  @SuppressWarnings("unused")
  @Test
  public void test() throws IOException {
    
    //<start id="ne-setup"/>
    String[] sentences = {
      "Former first lady Nancy Reagan was taken to a " +
              "suburban Los Angeles " +
      "hospital \"as a precaution\" Sunday after a " +
              "fall at her home, an " +
      "aide said. ",
      
      "The 86-year-old Reagan will remain overnight for " +
      "observation at a hospital in Santa Monica, California, " +
              "said Joanne " +
      "Drake, chief of staff for the Reagan Foundation."};
    
    NameFinderME finder = new NameFinderME(  //<co id="co.opennlp.name.initmodel"/>
      new TokenNameFinderModel(new FileInputStream(getPersonModel()))
    );
    
    Tokenizer tokenizer = SimpleTokenizer.INSTANCE; //<co id="co.opennlp.name.inittokenizer2"/>
    
    for (int si = 0; si < sentences.length; si++) {
      String[] tokens = tokenizer.tokenize(sentences[si]); //<co id="co.opennlp.name.tokenize2"/>
      Span[] names = finder.find(tokens); //<co id="co.opennlp.name.findnames3"/>
      displayNames(names, tokens);
    }
    
    finder.clearAdaptiveData(); //<co id="co.opennlp.name.clear"/>
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.initmodel">
      <para>Initialize a new model for identifying people names based on the 
        binary compressed model in the file "en-ner-person.bin".</para>
    </callout>
    <callout arearefs="co.opennlp.name.inittokenizer2">
      <para>Initialize a tokenizer to split the sentence into individual words 
        and symbols.</para>
    </callout>
    <callout arearefs="co.opennlp.name.tokenize2">
      <para>Split the sentence into an array of tokens.</para>
    </callout>
    <callout arearefs="co.opennlp.name.findnames3">
      <para>Identify the names in the sentence and return token-based offsets
      to these names.</para>
    </callout>
    <callout arearefs="co.opennlp.name.clear">
      <para>Clear data structures that store which words have been seen 
      previously in the document and whether these words were considered part 
      of a person's name.</para>
    </callout>    
    </calloutlist>*/
    //<end id="ne-setup"/>

    //<start id="ne-display2"/>
    for (int si = 0; si < sentences.length; si++) { //<co id="co.opennlp.name.eachsent2"/>
      Span[] tokenSpans = tokenizer.tokenizePos(sentences[si]); //<co id="co.opennlp.name.tokenizepos"/>
      String[] tokens = Span.spansToStrings(tokenSpans, sentences[si]); //<co id="co.opennlp.name.convert2strings"/>
      Span[] names = finder.find(tokens); //<co id="co.opennlp.name.findnames4"/>

      for (int ni = 0; ni < names.length; ni++) {
        Span startSpan = tokenSpans[names[ni].getStart()]; //<co id="co.opennlp.name.computestart"/>
        int nameStart  = startSpan.getStart(); 
        
        Span endSpan   = tokenSpans[names[ni].getEnd() - 1]; //<co id="co.opennlp.name.computeend"/>
        int nameEnd    = endSpan.getEnd();
        
        String name = sentences[si].substring(nameStart, nameEnd); //<co id="co.opennlp.name.namestring"/>
        System.out.println(name);
      }
    }
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.eachsent2">
      <para>Iterate over each sentence.</para>
    </callout>
    <callout arearefs="co.opennlp.name.tokenizepos">
      <para>Split the sentence into an array of tokens and return the 
        character offsets (spans) of those tokens.</para>
    </callout>
    <callout arearefs="co.opennlp.name.findnames4">
      <para>
      Identify the names in the sentence and return token-based offsets to these names.
      </para>
    </callout>
    <callout arearefs="co.opennlp.name.computestart">
      <para>
      Compute the start character index of the name.
      </para>
    </callout>    
    <callout arearefs="co.opennlp.name.computeend">
      <para>
      Compute the end character index (last character +1) of the name.
      </para>
    </callout>
    <callout arearefs="co.opennlp.name.computeend">
      <para>
      Compute the string which represents the name.
      </para>
    </callout>
    </calloutlist>*/
    //<end id="ne-display2"/>
    //<start id="ne-prob"/>
    for (int si = 0; si < sentences.length; si++) {//<co id="co.opennlp.name.eachsent3"/>
      String[] tokens = tokenizer.tokenize(sentences[si]); //<co id="co.opennlp.name.tokenize3"/>
      Span[] names = finder.find(tokens); //<co id="co.opennlp.name.findnames1"/>
      double[] spanProbs = finder.probs(names); //<co id="co.opennlp.name.probs"/>
    }
    /*<calloutlist>
    <callout arearefs="co.opennlp.name.eachsent3"><para>Iterate over each sentence.</para></callout>
    <callout arearefs="co.opennlp.name.tokenize3"><para>Split the sentence into an array of tokens.</para></callout>
    <callout arearefs="co.opennlp.name.findnames1"><para>Identify the names in the sentence and return token-based offsets to these names.</para></callout>
    <callout arearefs="co.opennlp.name.probs"><para>Return the probability associated with each name.</para></callout>
    </calloutlist>*/
    //<end id="ne-prob"/>
  }
}

class Annotation implements Comparable<Annotation> {
  private Span span;
  private String type;
  private double prob;

  public Annotation(String type, Span span, double prob) {
    this.span = span;
    this.type = type;
    this.prob = prob;
  }

  public Span getSpan() {
    return span;
  }

  public String getType() {
    return type;
  }

  public double getProb() {
    return prob;
  }

  public int compareTo(Annotation a) {
    int c = span.compareTo(a.span);
    if (c == 0) {
      c = Double.compare(prob, a.prob);
      if (c == 0) {
        c = type.compareTo(a.type);
      }
    }
    return c;
  }

  public String toString() {
    return type + " " + span + " " + prob;
  }
}