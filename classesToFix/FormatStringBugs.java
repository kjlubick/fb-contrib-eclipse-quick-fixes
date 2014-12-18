import java.util.Arrays;



public class FormatStringBugs {

    public String stuff(String[] args, String thing) {
        System.out.printf("%s%n", new int[] { 1, 2 });
        System.out.printf("%b%n", thing);
        System.out.printf("%d %d %d %n", 5, Boolean.FALSE, thing);
        System.out.printf("%x%n", Arrays.toString(args));
        
        return String.format("\nSize: %d contents: %s \n", args.length, Arrays.asList(args));
    }

}
