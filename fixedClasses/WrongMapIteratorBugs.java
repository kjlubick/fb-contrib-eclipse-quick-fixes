import java.util.List;
import java.util.Map;
import java.util.Set;


public class WrongMapIteratorBugs {
  
    public void testIterator(Map<String, Integer> map){
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer integer = entry.getValue();
            System.out.println(integer);
        }
    }
    
    public void testIteratorExplicitVar(Map<String, Integer> map){
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer i = entry.getValue();
            System.out.println(key + ':'+i);
        }
    }
    
    public void testIteratorExplicitList(Map<String, List<Integer>> map) {
        for (Map.Entry<String, List<Integer>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<Integer> someVal = entry.getValue();
            System.out.println(someVal);
        }
    }
    
    public void testIteratorImplicitSet(Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            Set<String> mapValue = entry.getValue();
            System.out.println(mapValue);
        }
    }
}
