import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPartition {
  @Test
  public void testPartition(){
    String query="one two three";
    Map<String,Long> map=new HashMap<>();
    map.put("one",10L);
    map.put("two",100L);
    map.put("three",50L);
    map.put("one two",10L);
    map.put("two three",10L);
    map.put("one two three",10L);
    Partition partition=new Partition(query.split(" ").length,map,query);
    List<String> expected = Arrays.asList("1,1,1", "1,2", "2,1","3");
    assertThat(partition.ls, is(expected));
  }
}
