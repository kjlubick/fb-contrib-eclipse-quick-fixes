import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class IsEmptyBugs {

    public Set<String> thisSet = new HashSet<String>();
    
    public boolean checkField() {
        return thisSet.size() == 0;
    }
    
    public void checkTwoParams(List<String> list, List<Integer> otherList) {
        while(list.size() != 0 && thisSet != null && 0 != otherList.size()) {
            list.remove(0);
            otherList.remove(0);
        }
        
        if (list.size() == 0 && 
                otherList.size() != 0) {
            System.out.println("FOO");
        }
    }
}
