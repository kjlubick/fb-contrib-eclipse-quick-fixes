
public class DeadLocalStoreBugs {
    
    private int count;
    
    public void initializeCount(int count) {
        System.out.println(count);
        
        this.count *= 0;
    }
    
    public int getCount() {
        return count;
    }

}
