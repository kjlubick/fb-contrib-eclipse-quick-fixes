package util;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.SourceLineAnnotation;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class TraversalUtil {

    private TraversalUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> T findClosestAncestor(@Nonnull ASTNode node, @Nonnull Class<T> parentClass) {
        ASTNode parent = node.getParent();
        while (parent != null) {
            if (parentClass.isAssignableFrom(parent.getClass())) { // allows parentClass to be something generic like Statement.class
                return (T) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public static ASTNode backtrackToBlock(ASTNode node) {
        // finds top-most expression that is not a block
        while (!(node.getParent() == null || node.getParent() instanceof Block)) {
            node = node.getParent();
        }
        return node;
    }
    
    public static MethodDeclaration findEnclosingMethod(CompilationUnit workingUnit, SourceLineAnnotation primarySourceLineAnnotation) {

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
