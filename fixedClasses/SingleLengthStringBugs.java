import java.util.List;


public class SingleLengthStringBugs {

    public String makeString(int i, SingleLengthStringBugs sb, List<Double> list) {
        return "This String: [" + i + " sb = " + sb +
                "list = " + list + ']';
    }
    
    public void bug20(boolean b, String mName, StringBuilder methodSig) {
        // see issue #20
        String thisMethodInfo = b ? (mName + ':' + methodSig) : "0";
    
        System.out.println(thisMethodInfo);
    }
    
    public String testList(List<SingleLengthStringBugs> list) {
        return new StringBuilder().append(this).append(':').append(list).toString();
    }
    
    public String testInts(int i, int j) {
        return new StringBuilder().append(i).append('+').append(j)
                .toString();
    }
    
    public String testSum(int i, int j) {
        return "The sum of " + i + " and " + j + ':' + (i + j);
    }
    
    public String testFirstIsToString(List<SingleLengthStringBugs> list, Object other) {
        return new StringBuilder().append(list.toString()).append(':').append(other)
                .toString();
    }
    
    public String testReplace(String str) {
        return str.replace('f', 'g');
    }
    
    public int testIndexOf(String str) {
        return str.indexOf('q');
    }
}
