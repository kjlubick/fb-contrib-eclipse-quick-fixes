import java.util.List;
import java.util.Set;


public class InefficiantArrayBugs {

    
    public String getOne(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return names.toArray(new String[0])[0];
    }
    
    public String[] convertToArray(List<String> names) {
        if (names == null) {
            return new String[0];
        }
        return names.toArray(new String[0]);
    }
}
