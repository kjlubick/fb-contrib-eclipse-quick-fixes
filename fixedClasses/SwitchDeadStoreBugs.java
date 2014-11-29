
public class SwitchDeadStoreBugs {
    
    //break;  return local variable; return field?
    private String thing;
    
    public String localSameAsReturn(int i) {
        String retVal = "";
        switch (i) {
        case 0:
            retVal = "foo";
            break;
        case 1:
            retVal = "foo";
            break;
        default:
            retVal = "fizzbuzz";
        }
        return retVal;
    }
    
    public String fieldSameAsReturn(int i) {
        switch (i) {
        case 0:
            thing = "foo";
            return thing;
        case 1:
            thing = "foo";
            return thing;
        default:
            thing = "fizzbuzz";
        }
        return thing;
    }
    
    public StringBuilder localDiffAsReturn(int i) {
        String retVal = "";
        switch (i) {
        case 0:
            retVal = "foo";
            break;
        case 1:
            retVal = "foo";
            break;
        default:
            retVal = "fizzbuzz";
        }
        return new StringBuilder(retVal);
    }
    
    public StringBuilder fieldDiffAsReturn(int i) {
        switch (i) {
        case 0:
            thing = "foo";
            break;
        case 1:
            thing = "foo";
            break;
        default:
            thing = "fizzbuzz";
        }
        return new StringBuilder(thing);
    }
    
    @Override
    public String toString() {
        return thing;
    }

}
