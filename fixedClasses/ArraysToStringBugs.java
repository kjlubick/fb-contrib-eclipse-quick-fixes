import java.math.BigDecimal;
import java.util.Arrays;


public class ArraysToStringBugs {

    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));
    }
    
    StringBuffer sBuffer = new StringBuffer();
    
    public String toString(Integer i, ArraysToStringBugs[] arr, BigDecimal bd) {
        sBuffer.append(i);
        sBuffer.append(Arrays.toString(arr));
        sBuffer.append(bd);
        return sBuffer.toString();
    }
    
    public String twoArrays(Object[] objs, int[] ints) {        //must be last one, due to double fix
        return Arrays.toString(objs) + " : " + Arrays.toString(ints);
    }
}
