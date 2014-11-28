import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class InefficiantArrayBugs {

    private List<Integer> someInts = new ArrayList<Integer>();
    
    public String getOne(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return names.toArray(new String[names.size()])[0];
    }
    
    public Integer[] convertToArray() {
        if (someInts == null) {
            return new Integer[0];
        }
        return this.someInts.toArray(new Integer[this.someInts.size()]);
    }
}
