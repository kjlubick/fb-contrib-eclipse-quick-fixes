
public class IsNANBugs {
    
    public void testNAN(float f) {
        if (!Float.isNaN(f)) {
            System.out.println("It's not a nan");
        }
    }
    
    public void testNotNAN(float f) {
        if (Float.isNaN(f)) {
            System.out.println("It's a nan");
        }
    }
    
    public void testNAN(Double d) {
        if (d.isNaN()) {
            System.out.println("It's a nan");
        } 
    }
    
    public void testNotNAN(double d) {
        if (!Double.isNaN(d)) {
            System.out.println("It's not a nan");
        }
    }
    
    public boolean testNAN2(Double doub) {
        return !doub.isNaN();
    }

}
