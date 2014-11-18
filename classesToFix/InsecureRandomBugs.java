import java.util.Random;


public class InsecureRandomBugs {

    private Random rand = new Random();
    
    private Random otherRandom = new Random();

    public Random getRand() {
        return rand;
    }

    public Random getOtherRandom() {
        return otherRandom;
    }
    
}
