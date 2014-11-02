import java.util.Map;


public class WrongMapIteratorBugs {
    
    public void testIterator(Map<String, Integer> map){
        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey()
            System.out.println(key);
        }
    }
    
    public void testIteratorExplicitVar(Map<String, Integer> map){
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer i = entry.getValue();
            System.out.println(key + ':'+i);
        }
    }

}
