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
    
    
    //TODO I think I can make this a util class, (something similar is used in charSetIssues)
    //Subclasses can take different types into the constructor.  
    private static class QMethodAndArgs {
        //TODO make these final
        public String qualifiedTypeString;      //the type that this method is invoked on (dot seperated)
        public String invokedMethodString;      //the name of the method invoked
        public List<String> argumentTypes;      //dot seperated argument types
        
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
