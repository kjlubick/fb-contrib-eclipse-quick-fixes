import java.util.List;


public class SingleLengthStringBugs {

    public String makeString(int i, SingleLengthStringBugs sb, List<Double> list) {
        return "This String: [" + i + " sb = " + sb +
                "list = " + list + "]";
    }
    
    public void bug20(boolean b, String mName, StringBuilder methodSig) {
        // see issue #20
        String thisMethodInfo = b ? (mName + ":" + methodSig) : "0";
    
        System.out.println(thisMethodInfo);
    }
    
    public String testList(List<SingleLengthStringBugs> list) {
        return this + ":" + list;
    }
    
    public String testInts(int i, int j) {
        return i + "+" + j;
    }
    
    public String testSum(int i, int j) {
        return "The sum of " + i + " and " + j + ":" + (i + j);
    }
    
    public String testFirstIsToString(List<SingleLengthStringBugs> list, Object other) {
        return list.toString() + ":" + other;
    }
    
    private static final String SEPERATOR_STRING = ":";
    
    public String testInts2(int i, int j) {
        return i + SEPERATOR_STRING + j;
    }
    
    public String testReplace(String str) {
        return str.replace("f", "g");
    }
    
    public int testIndexOf(String str) {
        return str.indexOf("q");
    }
}
