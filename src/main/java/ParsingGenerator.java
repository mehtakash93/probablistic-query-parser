import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
/*
This class is mainly calls the Partition class which would return integer partitions.
Example for number 3--->{111},{1,2},{2,1},{3}.
In this class we take in those partitions and map it to the query text.
The output of this class would be all the partitions.
i love solr--->{i,love,solr},{i,love solr},{i love,solr},{i love solr}.
 */

public class ParsingGenerator {
  final static Logger logger = Logger.getLogger(ParsingGenerator.class);
  public List<List<String>> generateParsings(String query, Map<String,Long> map) {
    String[] initialQuery=query.split("[,\\s\\.]+");
    List<List<String>> mainList=new ArrayList<>();
    try {
      Partition p=new Partition(initialQuery.length,map,query);
      List<String> allPartitions=p.ls;
      mainList=getListForPartitions(allPartitions,initialQuery);
    }
    catch(Exception e) {
      logger.warn(e);
    }
    return mainList;
  }
  private List<List<String>> getListForPartitions(List<String> allPartitons,String[] query) {
    List<List<String>> finalList=new ArrayList<>();
    for(String oneList:allPartitons) {
      List<String> oneParsing=new ArrayList<>();
      List<String> partitions= Arrays.asList(oneList.split("[,\\s\\.]+"));
      int startIndex=0;
      for (String partitionString:partitions) {
        int partitionNumber=Integer.parseInt(partitionString);
        oneParsing.add(String.join(" ",Arrays.copyOfRange(query, startIndex, startIndex+partitionNumber)));
        startIndex+=partitionNumber;
      }
      finalList.add(oneParsing);
    }
    return finalList;
  }
}
