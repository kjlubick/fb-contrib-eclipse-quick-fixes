
public class IsNANBugs {
    
    public void testNAN(float f) {
        if (f != Float.NaN) {
            System.out.println("It's not a nan");
        }
    }
    
    public void testNotNAN(float f) {
        if (Float.NaN == f) {
            System.out.println("It's a nan");
        }
    }
    
    public void testNAN(Double d) {
        if (d == Double.NaN) {
            System.out.println("It's a nan");
        } 
    }
    
    public void testNotNAN(double d) {
        if (d != Double.NaN) {
            System.out.println("It's not a nan");
        }
    }
    
    public boolean testNAN2(Double doub) {
        return doub != Double.NaN;
    }

}
