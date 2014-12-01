import java.util.Arrays;
import java.util.EnumSet;
import java.util.EnumMap;
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

    private Map<Suit, List<String>> badMap = new EnumMap<>(Suit.class);
    
    private Set<Suit> badSet;
    
    public UseEnumCollectionBugs() {
        badSet = EnumSet.noneOf(Suit.class);
    }
    
    public void addToMap(Suit suit, String... strings) {
        badMap.put(suit, Arrays.asList(strings));
    }

    public void addToSet(Suit s) {
        badSet.add(s);
    }
    
    public Set<Suit> getHand() {
        Set<Suit> badLocalSet = EnumSet.noneOf(Suit.class);
        badLocalSet.add(Suit.Hearts);
        badLocalSet.add(Suit.Clubs);
        return badLocalSet;
    }
    
    public Map<Suit, String> getHandMap() {
        Map<Suit, String> badLocalMap = new EnumMap<>(Suit.class);
        badLocalMap.put(Suit.Spades, "Foo");
        badLocalMap.put(Suit.Clubs, "Bar");
        return badLocalMap;
    }

    @Override
    public String toString() {
        return "UseEnumCollectionBugs [badMap=" + badMap + ", badSet=" + badSet + ']';
    }
    
}
