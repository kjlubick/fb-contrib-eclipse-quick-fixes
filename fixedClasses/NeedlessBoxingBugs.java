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
        boolean bo = Boolean.valueOf(data).booleanValue();
        byte b = Byte.valueOf(data).byteValue();
        short s = Short.valueOf(data).shortValue();
        int i = Integer.valueOf(data).intValue();
        long l = Long.valueOf(data).longValue();
        float f = Float.valueOf(data).floatValue();
        double d = Double.valueOf(data).doubleValue();
        
        System.out.println("" + bo + b + s + i + l + f + d);
    }

    public void testBooleanConsts(String s) {
        boolean b = false;
        boolean b1 = true;
        Boolean bb2 = Boolean.FALSE;
        Boolean bb = Boolean.TRUE;

        System.out.println("" + b + b1 + bb2 + bb);
    }
    
}
