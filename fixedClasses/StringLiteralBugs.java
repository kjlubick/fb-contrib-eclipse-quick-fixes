import java.util.Locale;


public class StringLiteralBugs {
    
    public static void main(String[] args) {
        if ("hello world".equals(args[0])) {
            System.out.println("WASSUP");
        }
        
        args[1] = "kiens' kasdf ksdfie  ad";
        
    }
    
    public String getString1() {
        return "foo";
    }
    
    public String getString2() {
        return "FOO";
    }
    
    public String getString3() {
        return "foo".toUpperCase(Locale.getDefault());
    }
    
    public String getString4() {
        return "FOO";
    }
    
    public String getString5() {
        return "foo".toUpperCase(Locale.forLanguageTag("eng"));
    }

}
