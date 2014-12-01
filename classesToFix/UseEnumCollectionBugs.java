import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;


public class UseEnumCollectionBugs {
    public enum Suit {
        Spades, Hearts, Clubs, Diamonds
    };

    private Map<Suit, List<String>> badMap = new HashMap<>();
    
    private Set<Suit> badSet;
    
    public UseEnumCollectionBugs() {
        badSet = new TreeSet<>();
    }
    
    public void addToMap(Suit suit, String... strings) {
        badMap.put(suit, Arrays.asList(strings));
    }

    public void addToSet(Suit s) {
        badSet.add(s);
    }
    
    public Set<Suit> getHand() {
        Set<Suit> badLocalSet = new HashSet<>();
        badLocalSet.add(Suit.Hearts);
        badLocalSet.add(Suit.Clubs);
        return badLocalSet;
    }
    
    public Map<Suit, String> getHandMap() {
        Map<Suit, String> badLocalMap = new TreeMap<>();
        badLocalMap.put(Suit.Spades, "Foo");
        badLocalMap.put(Suit.Clubs, "Bar");
        return badLocalMap;
    }

    @Override
    public String toString() {
        return "UseEnumCollectionBugs [badMap=" + badMap + ", badSet=" + badSet + ']';
    }
    
}
