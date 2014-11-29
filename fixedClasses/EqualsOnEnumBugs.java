
public class EqualsOnEnumBugs {
    
    private Thing thing = Thing.ONE;
    
    private enum Thing {
        ONE,TWO,THREE,FOUR
    }
    
    public boolean checkThing() {
        return thing == Thing.TWO;
    }
    
    public boolean checkOtherThing(Thing t) {
        return t == thing;
    }

}
