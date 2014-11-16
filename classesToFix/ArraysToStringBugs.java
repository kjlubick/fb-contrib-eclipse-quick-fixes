import java.math.BigDecimal;


public class ArraysToStringBugs {

    public static void main(String[] args) {
        System.out.println(args);
    }
    
    StringBuffer sBuffer = new StringBuffer();
    
    public String toString(Integer i, ArraysToStringBugs[] arr, BigDecimal bd) {
        sBuffer.append(i);
        sBuffer.append(arr);
        sBuffer.append(bd);
        return sBuffer.toString();
    }
    
    public String twoArrays(Object[] objs, int[] ints) {        //must be last one, due to double fix
        return objs + " : " + ints;
    }
}
