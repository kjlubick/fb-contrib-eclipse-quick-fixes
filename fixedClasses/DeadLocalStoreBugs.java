
public class DeadLocalStoreBugs {
    
    private int count;
    
    public void initializeCount(int count) {
        System.out.println(count);      // removes "dead store to parameter" 
        
        this.count *= 0;
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
            this.name = "foo" + className;
        }
        
        void setClassName(String className) {
            System.out.println(className);
            className = "foo" + name;       //not a DLS shadow bug detected (rank 15)
        }
        
        @Override
        public String toString() {
            return name;        //to avoid "unused parameter bugs"
        }
    }

}
