package quickfix;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class IsNANResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    
    
    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        // TODO Auto-generated method stub
        System.out.println("Repairing");
    }
    
    @Override
    public String getLabel() {
        System.out.println(getMarker());
        return super.getLabel();
    }

}
