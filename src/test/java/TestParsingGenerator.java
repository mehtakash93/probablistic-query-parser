import org.junit.Test;
import java.util.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestParsingGenerator {
  @Test
  public void testParsingGenerator(){
    String query="one two three";
    Map<String,Long> map=new HashMap<>();
    map.put("one",10L);
    map.put("two",100L);
    map.put("three",50L);
    map.put("one two",10L);
    map.put("two three",10L);
    map.put("one two three",10L);
    ParsingGenerator gen=new ParsingGenerator();
    List<List<String>> parsings=gen.generateParsings(query,map);
    List<String> expected1 = Arrays.asList("one","two","three");
    List<String> expected2 = Arrays.asList("one", "two three");
    List<String> expected3 = Arrays.asList("one two", "three");
    List<String> expected4 = Arrays.asList("one two three");
    List<List<String>> expected=new ArrayList<>();
    expected.add(expected1);
    expected.add(expected2);
    expected.add(expected3);
    expected.add(expected4);
    assertThat(expected, is(parsings));
  }
}
