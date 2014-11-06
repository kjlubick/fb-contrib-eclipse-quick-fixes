public class LiteralStringComparisonBugs {

    private static final String CONSTANT_STRING = "catmouse";

    public static void main(String[] args) {
        if (args[0].equals("foo") && args[1].equals("bar")) {
            System.out.println("Success!");
        }
    }

    public void testWeirdFormatting(String str1, String str2, String str3) {
        if (str1.equals("foo") &&
                str2.equals("bar")
                && !str2.equals(str3) && str3.
                        equals(CONSTANT_STRING)) {
            System.out.println("Strings conform");
        }
        System.out.println("done");
    }

}
