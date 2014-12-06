import java.util.Locale;


public class StringLiteralBugs {
    
    public static void main(String[] args) {
        if ("HelLo WoRld".toLowerCase().equals(args[0])) {
            System.out.println("WassUp".toUpperCase(Locale.CHINA));
        }
        
        args[1] = " kiens' kasdf ksdfie  ad         ".trim();
        
    }
    
    public String getString1() {
        return "FOO ".toLowerCase().trim();
    }
    
    public String getString2() {
        return " foo".trim().toUpperCase(Locale.UK);
    }
    
    public String getString3() {
        return "foo".toUpperCase(Locale.getDefault());
    }
    
    public String getString4() {
        return "foo".toUpperCase(Locale.JAPAN);
    }
    
    public String getString5() {
        return "foo".trim().toUpperCase(Locale.forLanguageTag("eng"));
    }

}
