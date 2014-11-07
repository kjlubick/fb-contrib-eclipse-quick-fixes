import java.security.SecureRandom;
import java.util.Random;


public class InsecureRandomBugs {

    private Random rand = new Random(new SecureRandom().nextLong());
    
    private Random otherRandom = new SecureRandom();

    public Random getRand() {
        return rand;
    }

    public Random getOtherRandom() {
        return otherRandom;
    }
    
}
