import com.google.common.base.Joiner;
import org.apache.lucene.index.*;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.log4j.Logger;
import java.util.*;
/*
This class is the main handler and where all the scoring is done.
The tasks of making the initial DocumentFrequency map and TermFrequencyMap is delegated to NGramHelper Class.
findTopParsing method--->creates all the partitions by calling ParsingGenerator class and sends each of them to scoreParsing method.
returns the top scored parsing.

scoreParsing method---> takes all the parsings and passes phrases among the parsings to scorePhrase method.

scorePhrase method--->Returns a score from each phrase passed to it. Uses the scoreSubPhrase method and getMinFreqNGram as a helper.
 */

public class ProbabilisticQueryParserRequestHandler extends RequestHandlerBase{
  private final static Logger logger = Logger.getLogger(ProbabilisticQueryParserRequestHandler.class);
  private Map<String,Map<String,Integer>> allDocFreqMaps;
  private Map<String,Map<String,Long>> allNGramFreqMaps;
  private String[] fields;
  private double multiplier=2.5;
  private double bigramThreshold = 0.4;
  private double trigramThreshold=0.37;
  private double trigramTitleThreshold = 0.3;
  private double bigramTitleThreshold = 0.9;
  public void handleRequestBody(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse) throws Exception {
    SolrParams params=solrQueryRequest.getParams();
    String query_text=params.get("q");
    /*These can be used for testing
    multiplier=Double.parseDouble(params.get("multiplier"));
    bigramThreshold=Double.parseDouble(params.get("bigramContent"));
    trigramThreshold=Double.parseDouble(params.get("trigramContent"));
    bigramTitleThreshold= Double.parseDouble(params.get("bigramTitle"));
    trigramTitleThreshold=Double.parseDouble(params.get("trigramTitle"));
    */
    allDocFreqMaps=new HashMap<>();
    allNGramFreqMaps=new HashMap<>();
    LeafReader reader;
    SolrIndexSearcher searcher;
    searcher = solrQueryRequest.getSearcher();
    reader = searcher.getLeafReader();
    fields=new String[2];
    fields[0]="content";
    fields[1]="title";
    for(String field:fields) {
      NgramHelper helper=new NgramHelper(reader,searcher);
      allDocFreqMaps.put(field,helper.getDocFreqMap(query_text,field));
      allNGramFreqMaps.put(field,helper.getNgramMap(query_text,field));
    }
    String finalQuery=findTopParsing(query_text);
    solrQueryResponse.add("ParsedQuery",finalQuery);
  }

  private String findTopParsing(String query_text) {
    double top_score=-1;
    List<String> top_parsing=new ArrayList<>();
    ParsingGenerator gen=new ParsingGenerator();
    List<List<String>> parsings=gen.generateParsings(query_text,allNGramFreqMaps.get(fields[0]));
    if(parsings.size()==0) {
      return query_text;
    }
    for(List<String> parsing: parsings) {
      double currentScore=scoreParsing(parsing);
      if(currentScore > top_score) {
        top_parsing=parsing;
        top_score=currentScore;
      }
    }
    StringBuilder listString = new StringBuilder();
    for (String s : top_parsing) {
      listString.append("\"" + s + "\"");
    }
    return listString.toString();
  }

  private double scoreParsing(List<String> parsing) {
    double score=0;
    try {
      for (String phrase : parsing) {
        double currentScore=scorePhrase(phrase);
        if(currentScore==-1) {
          return -1;
        }
        score += currentScore;
      }
    }
    catch(Exception e){
      logger.warn(e);
    }
    return score;
  }

  private double scorePhrase(String phrase) {
    double score = 0;
    List<String> parsedList=Arrays.asList(phrase.split("[,\\s\\.]+"));
    try {
      if(parsedList.size()<=1){
        return score;
      }
      else if (parsedList.size() == 2) {
        for(String field:fields) {
          score += scoreSubPhrase(phrase, field);
          if(field.equals("content")) {
            score = Math.max(score - bigramThreshold, 0);
          }
          else {
            score = Math.max(score - bigramTitleThreshold, 0);
          }
        }
      } else {
        for (int i = 0; i < parsedList.size()-2; i++) {
          String currentTrigram=Joiner.on(" ").join(parsedList.subList(i,i+3));
          for(String field:fields) {
            score += scoreSubPhrase(currentTrigram, field);
            if(field.equals("content")) {
              score = Math.max(score - trigramThreshold, 0);
            }
            else {
              score = Math.max(score - trigramTitleThreshold, 0);
            }
          }
          score /= (double)(parsedList.size() - 2);
        }
      }
    }
    catch (Exception e) {
      logger.warn(e);
    }
    return score;
  }
  private double scoreSubPhrase(String phrase,String field){
    double score = 0.0;
    try {
      double termFreq = allNGramFreqMaps.get(field).get(phrase);
      if (termFreq == 0) {
        return -1;
      }
      long docFreq = allDocFreqMaps.get(field).get(phrase);
      if (docFreq != 0) {
        score += termFreq / (double) (docFreq * 2);
      }
      else {
        score += termFreq;
      }
      score += termFreq / getMinFreqNGram(phrase, allNGramFreqMaps.get(field));
      if(field.equals("title")) {
        score = score * multiplier;
      }
    }
    catch(Exception e){
      logger.warn(e);
    }
    return score;
  }

  public double getMinFreqNGram(String ngramString,Map<String,Long> ngramMap){
    long minFreq=Long.MAX_VALUE;
    try{
      for (String onegram : ngramString.split("[,\\s\\.]+")) {
        long onegramFreq=ngramMap.get(onegram);
        minFreq = Math.min(onegramFreq, minFreq);
      }
    }
    catch(Exception e){
      logger.warn(e);
    }
    return minFreq==0? 1:minFreq;
  }

  public String getDescription() {
    return null;
  }
}
