import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import com.google.common.base.Joiner;
import java.util.Map;
/*
The recursive method partition creates all the partitions from a given number.
Example for number 3--->{111},{1,2},{2,1},{3}.
We also take a map as an input so that if any of the phrase in the parsing has zero ocurance we ignore all the parsing made from that phrase.
 */

public class Partition {
  public List<String> ls;
  public Partition(int n, Map<String,Long> map,String query) {
    ls=new ArrayList<>();
    String[] queryArray=query.split(" ");
    partition(n,1,"",ls,map,queryArray);
  }

  private void partition(int n, int max, String prefix,List<String> partitions,Map<String,Long> map,String[] query) {
    if (n == 0) {
      prefix=prefix.replaceFirst("^,", "");
      partitions.add(prefix);
      return;
    }
    for (int i = Math.min(max, n); i <= n; i++) {
      String current=Joiner.on(" ").join(Arrays.copyOfRange(query,query.length-n,query.length-n+i));
      if(i<=3 && map.get(current)==0)
        break;
      partition(n-i, max, prefix + "," + i,partitions,map,query);
    }
  }
}