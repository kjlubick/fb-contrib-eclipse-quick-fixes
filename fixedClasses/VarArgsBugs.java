
public class VarArgsBugs {
    
    public void testNormalUVA(int foo...) {
        System.out.println(foo.length);
    }
    
    public void testLowUVA1(int boo, String hoo...) {
        System.out.println(hoo.length);
    }

}
