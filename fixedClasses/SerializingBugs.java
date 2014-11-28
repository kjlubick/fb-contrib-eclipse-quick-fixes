import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class SerializingBugs implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private transient BigDecimalStringBugs
    obj1 = new BigDecimalStringBugs();
    
    private List<String> things = new ArrayList<>();
    
    transient List<IsNANBugs> isNanThingsBugs = new ArrayList<>();
    
    public void print() {
        System.out.println("Things: "+things + obj1 + isNanThingsBugs);
    }

}
