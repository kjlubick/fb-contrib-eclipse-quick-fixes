package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class CompareFloatResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new CompareToVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);
        CompareToVisitor visitor = new CompareToVisitor();
        node.accept(visitor);
        
        if (visitor.expressionToReplace != null) {
            
        }
    }
    
    private static class CompareToVisitor extends ASTVisitor implements ApplicabilityVisitor {
        
        ConditionalExpression expressionToReplace;
        VariableDeclarationStatement optionalTempVariableToDelete;
        
        @Override
        public boolean visit(ConditionalExpression node) {
            // TODO Auto-generated method stub
            return super.visit(node);
        }

        @Override
        public boolean isApplicable() {
            return expressionToReplace != null;
        }
    }

}
