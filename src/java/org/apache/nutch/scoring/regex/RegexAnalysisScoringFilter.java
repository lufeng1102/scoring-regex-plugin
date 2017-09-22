/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nutch.scoring.regex;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.nutch.crawl.CrawlDatum;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.indexer.NutchDocument;
import org.apache.nutch.metadata.Nutch;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseData;
import org.apache.nutch.protocol.Content;
import org.apache.nutch.scoring.ScoringFilter;
import org.apache.nutch.scoring.ScoringFilterException;
import org.apache.nutch.util.NutchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class RegexAnalysisScoringFilter
  implements ScoringFilter {

  private Configuration conf;
  private Map<String,Float> regexScoreMap;
  private final static Logger LOG = LoggerFactory.getLogger(RegexAnalysisScoringFilter.class);

  public RegexAnalysisScoringFilter() {

  }

  public Configuration getConf() {
    return conf;
  }

  public void setConf(Configuration conf) {
    this.conf = conf;
    String fileRules = conf.get("scoring.regex.file");
    Reader reader =  conf.getConfResourceAsReader(fileRules);
    try {
      regexScoreMap = readRules(reader);
    } catch (IOException e) {
      if (LOG.isErrorEnabled()) { LOG.error(e.getMessage()); }
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private Map<String, Float> readRules(Reader reader) throws IOException {

    BufferedReader in = new BufferedReader(reader);
    String line;
    Map<String,Float> regexScoreMap = new TreeMap<String, Float>();

    while((line=in.readLine())!=null) {
      if (line.length() == 0) {
        continue;
      }
      char first=line.charAt(0);
      switch (first) {
        case ' ' : case '\n' : case '#' :           // skip blank & comment lines
          continue;
        default :
          break;
      }

      String[] urlRegex = line.split(" ");
      //LOG.debug("Line:"+line + ":"+urlRegex.length);
      if(urlRegex.length ==2) {
        Float inc_rate = Float.parseFloat(urlRegex[1]);
        String patternTxt = urlRegex[0];
        regexScoreMap.put(patternTxt,inc_rate);
        if (LOG.isTraceEnabled()) { LOG.trace("Adding rule [" + patternTxt + "]"); }
      }
    }
    return regexScoreMap;
  }

  public CrawlDatum distributeScoreToOutlinks(Text fromUrl,
    ParseData parseData, Collection<Entry<Text, CrawlDatum>> targets,
    CrawlDatum adjust, int allCount)
    throws ScoringFilterException {
    return adjust;
  }

  public float generatorSortValue(Text url, CrawlDatum datum, float initSort)
    throws ScoringFilterException {
	Pattern pattern;
	Float scoreBoost;
    for(String patternTxt: regexScoreMap.keySet()){
    	pattern =  Pattern.compile(patternTxt);
    	if(pattern.matcher(url.toString()).find()){
    		scoreBoost = regexScoreMap.get(patternTxt);
    		LOG.debug("Applying scoring to document | " + url.toString() + " | " + scoreBoost);
    		if (datum.getScore() == 0){
    			datum.setScore(1.0f);
    		}
    		return datum.getScore() * initSort * scoreBoost;
    	}
    }
    return datum.getScore() * initSort;
  }

  public float indexerScore(Text url, NutchDocument doc, CrawlDatum dbDatum,
    CrawlDatum fetchDatum, Parse parse, Inlinks inlinks, float initScore)
    throws ScoringFilterException {
    return dbDatum.getScore();
  }

  public void initialScore(Text url, CrawlDatum datum)
    throws ScoringFilterException {
    datum.setScore(0.0f);
  }

  public void injectedScore(Text url, CrawlDatum datum)
    throws ScoringFilterException {
  }

  public void passScoreAfterParsing(Text url, Content content, Parse parse)
    throws ScoringFilterException {
    parse.getData().getContentMeta().set(Nutch.SCORE_KEY,
      content.getMetadata().get(Nutch.SCORE_KEY));
  }

  public void passScoreBeforeParsing(Text url, CrawlDatum datum, Content content)
    throws ScoringFilterException {
    content.getMetadata().set(Nutch.SCORE_KEY, "" + datum.getScore());
  }

  public void updateDbScore(Text url, CrawlDatum old, CrawlDatum datum,
    List<CrawlDatum> inlinked)
    throws ScoringFilterException {
    // nothing to do
  }



  public static void main(String args[]) throws IOException {
    RegexAnalysisScoringFilter regex = new RegexAnalysisScoringFilter();
    regex.setConf(NutchConfiguration.create());
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String line;
    float newScore = 0f;
    while((line=in.readLine())!=null) {
      CrawlDatum crawlDatum = new CrawlDatum();
      crawlDatum.setScore(1.0f);
      try {
    	  newScore = regex.generatorSortValue(new Text(line),crawlDatum,1.0F);
      } catch (ScoringFilterException e) {
        e.printStackTrace();
      }
      System.out.println(crawlDatum);
      System.out.println("New Score: " + newScore);
      System.out.println("-------------");
    }
  }
}
