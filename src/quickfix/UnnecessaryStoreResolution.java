package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class UnnecessaryStoreResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        USBRVisitor visitor = new USBRVisitor();
        node.accept(visitor);
        
        
        ReturnStatement newReturnStatement;
        
        
        rewrite.replace(visitor.originalReturn, newReturnStatement, null);
        
    }
    
    private static class USBRVisitor extends ASTVisitor {
        
        public ReturnStatement originalReturn;

        @Override
        public boolean visit(ReturnStatement node) {
            // TODO Auto-generated method stub
            return super.visit(node);
        }
        
    }

}
