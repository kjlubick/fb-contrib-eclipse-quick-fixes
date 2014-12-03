
public class EqualsOnEnumBugs {
    
    private Thing thing = Thing.ONE;
    
    private enum Thing {
        ONE,TWO,THREE,FOUR
    }
    
    public boolean checkThing() {
        return thing.equals(Thing.TWO);
    }
    
    public boolean checkOtherThing(Thing t, String str) {
        return t.equals(thing) && "Foo".equals(str)
                && !thing.equals(Thing.FOUR);
    }

}
