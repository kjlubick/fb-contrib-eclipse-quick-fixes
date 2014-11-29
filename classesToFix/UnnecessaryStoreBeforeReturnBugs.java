
public class UnnecessaryStoreBeforeReturnBugs {

    private Object argumentTypes = new Object();
    private Object qualifiedType = new Object();
    private boolean wasConstructor = true;
    
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UnnecessaryStoreBeforeReturnBugs [argumentTypes=");
        builder.append(argumentTypes);
        builder.append(", qualifiedType=");
        builder.append(qualifiedType);
        builder.append(", wasConstructor=");
        builder.append(wasConstructor);
        builder.append(']');
        String string = builder.toString();
        return string;
    }

    public int doMath(int i, int j) {
        if (wasConstructor) {
            int ave = (i + j + 1) / 2;
            return ave;
        }
        return 0;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argumentTypes == null) ? 0 : argumentTypes.hashCode());
        result = prime * result + ((qualifiedType == null) ? 0 : qualifiedType.hashCode());
        result = prime * result + (wasConstructor ? 1231 : 1237);
        return result;
    }
    
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
        retVal += i;     
        return retVal;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UnnecessaryStoreBeforeReturnBugs other = (UnnecessaryStoreBeforeReturnBugs) obj;
        if (argumentTypes == null) {
            if (other.argumentTypes != null)
                return false;
        } else if (!argumentTypes.equals(other.argumentTypes))
            return false;
        if (qualifiedType == null) {
            if (other.qualifiedType != null)
                return false;
        } else if (!qualifiedType.equals(other.qualifiedType))
            return false;
        if (wasConstructor != other.wasConstructor)
            return false;
        return true;
    }
}
