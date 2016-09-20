import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.util.NamedList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
/*
This class is mainly used to add a feedback loop and test on queries to find the accuracy of the parser.
This is not a UNIT test for the requestHandler.
 */

public class TestQueries {
  private static List<String> allQueries;
  private static List<String> groundTruths;
  @Test
  public void testQueries(){
    try {
      String urlString = "http://localhost:8983/solr/newsData";
      HttpSolrClient solr = new HttpSolrClient.Builder(urlString).build();
      String uri="/config?wt=json";
      HttpPost httpPost=new HttpPost(urlString+uri);
      String payload = "{\n" +
              "'add-requesthandler' : { 'name' : '/probablisticParser', 'class': 'ProbabilisticQueryParserRequestHandler'}}\n" +
              "}";
      StringEntity input = new StringEntity(payload);
      input.setContentType("application/json");
      httpPost.setEntity(input);
      solr.getHttpClient().execute(httpPost);
      TestQueries t = new TestQueries();
      double result;
      t.loadQueries();
      for(double j = 1.5; j <= 3.0; j+=0.1) {
        result = 0;
        for (int i = 0; i < allQueries.size(); i++) {
          SolrQuery sq = new SolrQuery();
          sq.setRequestHandler("/probablisticParser");
          sq.setParam("multiplier", Double.toString(j));
          sq.setParam("bigramContent", Double.toString(0.4));
          sq.setParam("trigramContent", Double.toString(0.37));
          sq.setParam("bigramTitle", Double.toString(0.9));
          sq.setParam("trigramTitle", Double.toString(0.3));
          sq.setQuery(allQueries.get(i));
          sq.set("wt", "json");
          QueryResponse rsp = solr.query(sq);
          NamedList nl = rsp.getResponse();
          result += percentCorrect((String) nl.get("ParsedQuery"), groundTruths.get(i));
        }
        System.out.println(result / allQueries.size());
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public static double percentCorrect(String parserQuery,String groundTruth){
    Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(parserQuery);
    List<String> parsings = new ArrayList<>();
    while (m.find()) {
      parsings.add(m.group(1).replaceAll("\"", ""));
    }
    ArrayList<String> desiredParsings= new ArrayList<>(Arrays.asList(groundTruth.split("\n")));
    int maxLength=Math.min(parsings.size(),desiredParsings.size());
    double score=0;
    for(int i=0;i<maxLength;i++){
      if(parsings.get(i).replaceAll("\\s+","").equals(desiredParsings.get(i).replaceAll("\\s+",""))){
        score+=1/(double) parsings.size();
      }
    }
    return score;
  }

  public void loadQueries(){
    try {
      File file=new File("src/test/resources/SampleQueries.txt");
      BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
      allQueries=new ArrayList<>();
      groundTruths=new ArrayList<>();
      String line = reader.readLine();
      String patternToMatch="Query";
      int currentQuery=0;
      while (line != null) {
        if (line.indexOf(patternToMatch) == 0) {
          allQueries.add(line.substring(line.lastIndexOf(":") + 1));
          currentQuery++;
        } else {
          if (groundTruths.size() < currentQuery) {
            groundTruths.add(line);
            line = reader.readLine();
            if (line.indexOf(patternToMatch) == 0) {
              continue;
            }
          }
          groundTruths.set(currentQuery-1, groundTruths.get(currentQuery - 1) + "\n" + line);
          }
        line = reader.readLine();
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
  }
}
