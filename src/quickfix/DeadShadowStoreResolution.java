package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class DeadShadowStoreResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return false; // types don't matter, we just look for an assignment
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);

        DeadStoreVisitor visitor = new DeadStoreVisitor();
        node.accept(visitor);

        SimpleName leftSide = visitor.badLeftSideName;

        if (leftSide != null) {
            AST ast = rewrite.getAST();

            FieldAccess newField = ast.newFieldAccess();
            newField.setExpression(ast.newThisExpression());
            newField.setName((SimpleName) rewrite.createMoveTarget(leftSide));

            rewrite.replace(leftSide, newField, null);
        } else {
            System.err.println("Could not find a local assignment to replace.");
        }
    }

    @Override
    public String getDescription() {
        return "Turns the assignment to a local shadow variable into a field assignment "
                + "by adding a this. to the variable";
    }

    private static class DeadStoreVisitor extends ASTVisitor {

        public SimpleName badLeftSideName;

        @Override
        public boolean visit(Assignment node) {
            if (badLeftSideName != null) {
                return false;
            }

            Expression left = node.getLeftHandSide();
            if (left instanceof SimpleName) {
                badLeftSideName = (SimpleName) left;
                return false;
            }
            return true;
        }
    }
}
