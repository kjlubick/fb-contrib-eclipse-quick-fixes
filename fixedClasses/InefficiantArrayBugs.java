import java.util.List;
import java.util.Set;


public class InefficiantArrayBugs {

    
    public String getOne(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return names.toArray(new String[names.size()])[0];
    }
    
    public String[] convertToArray(List<Integer> names) {
        if (names == null) {
            return new Integer[0];
        }
        return names.toArray(new Integer[names.size()]);
    }
}
