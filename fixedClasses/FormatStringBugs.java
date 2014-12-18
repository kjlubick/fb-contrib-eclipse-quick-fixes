import java.util.Arrays;



public class FormatStringBugs {

    public String stuff(String[] args, String thing) {
        System.out.printf("%s%n", Arrays.toString(new int[] { 1, 2 }));
        System.out.printf("%s%n", thing);
        System.out.printf("%d %s %s %n", 5, Boolean.FALSE, thing);
        System.out.printf("%s%n", Arrays.toString(args));
        
        return String.format("%nSize: %d contents: %s %n", args.length, Arrays.asList(args));
    }

}
