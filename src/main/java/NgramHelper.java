import org.apache.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
This class is used to make the Document Frequency and Term Frequency maps.
These maps would be made at the beginning and would be passed to Partition class for removing partitions which contain phrase with zero doc occurance.
These maps wil also be used for scoring.
 */

public class NgramHelper {
  final static Logger logger = Logger.getLogger(ProbabilisticQueryParserRequestHandler.class);
  private IndexReader reader;
  private IndexSearcher searcher;

  public NgramHelper(IndexReader reader, IndexSearcher searcher){
    this.reader=reader;
    this.searcher=searcher;
  }

  private List<String> buildNgrams(String query){
    List<String> ngrams=new ArrayList<String>();
    try {
      StandardTokenizer st = new StandardTokenizer();
      st.setReader(new StringReader(query));
      TokenStream tokenStream = new StandardFilter(st);
      ShingleFilter sf = new ShingleFilter(tokenStream,2,3);
      sf.setOutputUnigrams(true);
      CharTermAttribute charTermAttribute = sf.addAttribute(CharTermAttribute.class);
      sf.reset();
      while (sf.incrementToken()) {
        ngrams.add(charTermAttribute.toString());
      }
    }
    catch(Exception e){
      logger.warn(e);
    }
    return ngrams;
  }


  private Map<String,Long> buildNgramFrequencyMap(String query,String field){
    HashMap<String,Long> ngramMap=new HashMap<>();
    List<String> ngrams=this.buildNgrams(query);
    try {
      for (String ngram : ngrams) {
        if (!ngramMap.containsKey(ngram)) {
          if(ngram.split(" ").length==1) {
            ngramMap.put(ngram, reader.totalTermFreq(new Term("unigrams"+field, ngram)));
          }
          else if(ngram.split(" ").length==2){
            ngramMap.put(ngram, reader.totalTermFreq(new Term("bigrams"+field, ngram)));
          }
          else {
            ngramMap.put(ngram, reader.totalTermFreq(new Term("trigrams" + field, ngram)));
          }
        }
      }
    }
    catch(Exception e){
      logger.warn(e);
    }
    return ngramMap;
  }

  private Map<String,Integer> buildDocFrequencyMap(String query,String field){
    HashMap<String,Integer> docMap=new HashMap<>();
    List<String> ngrams=buildNgrams(query);
    try {
      for (String ngram : ngrams) {
        if (!docMap.containsKey(ngram)) {
          if(ngram.split(" ").length==1) {
            docMap.put(ngram, reader.docFreq(new Term("unigrams"+field, ngram)));
          }
          else{
            BooleanQuery.Builder qb=new BooleanQuery.Builder();
            for(String onegram:ngram.split(" ")){
              qb.add(new TermQuery(new Term("unigrams"+field, onegram.toLowerCase())), BooleanClause.Occur.MUST);
            }
            TotalHitCountCollector collector = new TotalHitCountCollector();
            searcher.search(qb.build(), collector);
            docMap.put(ngram,this.searcher.count(qb.build()));
          }
        }
      }
    }
    catch(Exception e){
      logger.warn(e);
    }
    return docMap;
  }

  public Map<String, Long> getNgramMap(String query,String field) {
    return buildNgramFrequencyMap(query,"_"+field);
  }

  public Map<String, Integer> getDocFreqMap(String query,String field) {
    return buildDocFrequencyMap(query,"_"+field);
  }
}
