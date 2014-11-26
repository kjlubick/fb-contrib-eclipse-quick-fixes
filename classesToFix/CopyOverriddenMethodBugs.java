

public class CopyOverriddenMethodBugs extends Visitor {
    
    @Override
    public boolean methodA(String s) {
        return true;
    }
    
    @Override
    public boolean methodB(String s) {
        return false;
    }
    
    public boolean methodC(String s) {
        return true;
    }
    
    @Override
    public void methodD() {
        // EMPTY
    }

}

abstract class Visitor {
    public boolean methodA(String s) {
        return true;
    }
    
    public boolean methodB(String s) {
        return true;
    }
    
    public boolean methodC(String s) {
        return true;
    }
    
    public void methodD() {
        
    }
}
