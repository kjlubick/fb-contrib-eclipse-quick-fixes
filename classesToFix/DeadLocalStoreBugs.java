import java.util.Collections;
import java.util.List;


public class DeadLocalStoreBugs {
    
    private int count;
    
    public void initializeCount(int count) {
        System.out.println(count);      // removes "dead store to parameter" 
        
        count *= 0;
    }
    
    public int getCount() {
        return count;
    }
    
    private String className;
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    class InnerClass {
        private String name;
        
        public InnerClass(String name) {
           this.name = name;
        }
        
        void setString(String name) {
            System.out.println(name);
            name = "foo" + className;
        }
        
        void setClassName(String className, List<Integer> otherArg) {
            System.out.println(className);
            className = "foo" + name;       //not a DLS shadow bug detected (rank 15)
            
            System.out.println(otherArg);
            otherArg = Collections.emptyList();
        }
        
        @Override
        public String toString() {
            return name;        //to avoid "unused parameter bugs"
        }
    }

}
