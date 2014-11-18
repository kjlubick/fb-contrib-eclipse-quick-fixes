public class LiteralStringComparisonBugs {

    private static final String CONSTANT_STRING = "catmouse";

    public static void main(String[] args) {
        if ("foo".equals(args[0]) && "bar".equals(args[1])) {
            System.out.println("Success!");
        }
    }

    public void testWeirdFormatting(String str1, String str2, String str3) {
        if ("foo".equals(str1) &&
                "bar".equals(str2)
                && !str2.equals(str3) && CONSTANT_STRING.equals(str3)) {
            System.out.println("Strings conform");
        }
        System.out.println("done");
    }

}
