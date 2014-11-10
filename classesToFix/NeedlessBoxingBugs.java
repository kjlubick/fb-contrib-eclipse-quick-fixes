import java.util.concurrent.Callable;

@SuppressWarnings("unused")
public class NeedlessBoxingBugs {

    public Boolean testBooleanBox() throws Exception {
        Callable<Boolean> getThing = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return true;
            }
        };
        return getThing.call();
    }
    
    public void testDupCtor() {
        Boolean bo = new Boolean(false);
        Byte b = new Byte((byte) 0);
        Character c = new Character('a');
        Short s = new Short((short) 0);
        Integer i = new Integer(0);
        Long l = new Long(0);
        Float f = new Float(0.0f);
        Double d = new Double(0.0);
        
        System.out.println("" + bo + b + c + s + i + l + f + d);
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

    public Boolean testBooleanConsts(String s) {
        boolean b = Boolean.FALSE;
        b = Boolean.TRUE;
        Boolean bb = false;
        bb = true;

        return Boolean.valueOf("true".equals(s) && bb.booleanValue());
    }
    
}
