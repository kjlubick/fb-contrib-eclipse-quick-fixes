import java.util.concurrent.Callable;


public class NeedlessBoxingBugs {

    public Boolean testBooleanBox() throws Exception {
        Callable<Boolean> getThing = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return Boolean.TRUE;
            }
        };
        return getThing.call();
    }

    public void testNeedsParse(String data) {
        // The first one is a false positive for < 1.5
        boolean bo = Boolean.parseBoolean(data);
        byte b = Byte.parseByte(data);
        short s = Short.parseShort(data);
        int i = Integer.parseInt(data);
        long l = Long.parseLong(data);
        float f = Float.parseFloat(data);
        double d = Double.parseDouble(data);
        
        System.out.println("" + bo + b + s + i + l + f + d);
    }

    public void testBooleanConsts(String s) {
        boolean b = false;
        boolean b1 = true;
        Boolean bb2 = Boolean.FALSE;
        Boolean bb = Boolean.TRUE;

        System.out.println("" + b + b1 + bb2 + bb);
    }
    
    public int needsParse2(String num) {
        return Integer.parseInt(num);
    }
    
}
