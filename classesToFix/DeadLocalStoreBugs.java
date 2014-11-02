
public class DeadLocalStoreBugs {
    
    private int count;
    
    public void initializeCount(int count) {
        System.out.println(count);
        
        count *= 0;
    }
    
    public int getCount() {
        return count;
    }

}
