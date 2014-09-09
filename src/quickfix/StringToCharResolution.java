package quickfix;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class StringToCharResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        // TODO Auto-generated method stub

    }
    
    
    private static class StringToCharVisitor extends ASTVisitor {
        //private static 
    }
   
    
    //for testing
    private StringBuffer sb;
    
    public void test(String foo) {
        sb = new StringBuffer();
        sb.append("f").append("o");
        sb.append('f').append("o");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("g");
        sb2.append('g');
        
        System.out.println(foo.replace(".", ","));
        System.out.println(foo.replace('.', ','));
        
        System.out.println(foo.lastIndexOf("."));
        System.out.println(foo.lastIndexOf('.'));
            
    }

}
