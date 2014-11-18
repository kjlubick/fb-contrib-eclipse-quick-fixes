import java.util.List;
import java.util.Map;
import java.util.Set;


public class WrongMapIteratorBugs {
  
    public void testIterator(Map<String, Integer> map){
        for(String key: map.keySet()) {
            System.out.println(map.get(key));
        }
    }
    
    public void testIteratorExplicitVar(Map<String, Integer> map){
        for(String key: map.keySet()) {
            Integer i = map.get(key);
            System.out.println(key + ':'+i);
        }
    }
    
    public void testIteratorExplicitList(Map<String, List<Integer>> map) {
        for (String key : map.keySet()) {
            List<Integer> someVal = map.get(key);
            System.out.println(someVal);
        }
    }
    
    public void testIteratorImplicitSet(Map<String, Set<String>> map) {
        for (String key : map.keySet()) {
            System.out.println(map.get(key));
        }
    }
}
