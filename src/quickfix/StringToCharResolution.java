package quickfix;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

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
    
    
    StringBuffer sb;
    
    private void test(String foo) {
        sb = new StringBuffer();
        sb.append("f");
        sb.append('f');
        StringBuilder sb2 = new StringBuilder();
        sb2.append("g");
        sb2.append('g');
        
        System.out.println(foo.replace(".", ","));
        System.out.println(foo.replace('.', ','));
        
        System.out.println(foo.lastIndexOf("."));
        System.out.println(foo.lastIndexOf('.'));
            
    }

}
