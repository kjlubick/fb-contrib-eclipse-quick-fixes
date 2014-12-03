import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class IsEmptyBugs {

    public Set<String> thisSet = new HashSet<String>();
    
    public boolean checkField() {
        return thisSet.isEmpty();
    }
    
    public void checkTwoParams(List<String> list, List<Integer> otherList) {
        while(!list.isEmpty() && thisSet != null && !otherList.isEmpty()) {
            list.remove(0);
            otherList.remove(0);
        }
        
        if (list.isEmpty() && 
                !otherList.isEmpty()) {
            System.out.println("FOO");
        }
    }
}
