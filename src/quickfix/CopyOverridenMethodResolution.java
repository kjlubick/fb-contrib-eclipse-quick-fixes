package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class CopyOverridenMethodResolution extends BugResolution{

    @Override
    protected boolean resolveBindings() {
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        MethodDeclaration node = findEnclosingMethod(workingUnit, bug.getPrimarySourceLineAnnotation());
        
        if (node != null) {
            rewrite.remove(node, null);
        }
    }

    private MethodDeclaration findEnclosingMethod(CompilationUnit workingUnit, SourceLineAnnotation primarySourceLineAnnotation) {

        MethodFinder mf = new MethodFinder(workingUnit, primarySourceLineAnnotation.getStartLine());
        workingUnit.accept(mf);
        
        return mf.enclosingMethod;
    }

    
    private static class MethodFinder extends ASTVisitor {
        
        MethodDeclaration enclosingMethod;
        private int lineToLookFor;
        private CompilationUnit compilationUnit;

        public MethodFinder(CompilationUnit workingUnit, int startLine) {
            this.lineToLookFor = startLine;
            this.compilationUnit = workingUnit;
        }
        
        @Override
        public boolean visit(MethodDeclaration node) {
            int startingLineNumber = compilationUnit.getLineNumber(node.getStartPosition());
            int endingLineNumber = compilationUnit.getLineNumber(node.getLength() + node.getStartPosition());
            if (lineToLookFor >= startingLineNumber && lineToLookFor <= endingLineNumber) {
                this.enclosingMethod = node;
            }
            return false;
        }
        
    }
}
