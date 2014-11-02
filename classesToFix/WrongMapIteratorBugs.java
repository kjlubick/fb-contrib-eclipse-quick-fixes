import java.util.Map;


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
}
