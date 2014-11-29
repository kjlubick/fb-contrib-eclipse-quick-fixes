
public class NeedsDefaultCaseBugs {

    private int thing = 0;
    
    public String getString(int i) {
        switch (i) {
        case 0:
            thing++;
            break;
        case 1:
            thing += 7;
            break;
        case 2:
            thing--;
            break;
        }
       
        return "Thing: "+(thing + i);
    }
    
}
