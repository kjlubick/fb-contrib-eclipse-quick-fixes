package fb.contrib;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

public class LiteralStringComparisonResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        // TODO Auto-generated method stub
        //rewrite.createStringPlaceholder("FOO BAR", nodeType)
        Debug.println("Do work here");

        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        Debug.println(node);
        LSCVisitor visitor = new LSCVisitor();
        node.accept(visitor);

        //rewrite.replace(node, replacement, editGroup);
    }

    private static class LSCVisitor extends ASTVisitor {

        private MethodInvocation LSCMethodInvocation;

        @Override
        public boolean visit(MethodInvocation node) {
            Debug.println();
            Debug.println(node);
            if (this.LSCMethodInvocation != null) {
                return false;
            }
            Debug.println(node.getName());
            Debug.println(node.getExpression());
            Debug.println(node.arguments());
            if ("equals".equals(node.getName().toString())) {
                Debug.println("was equals");

                List<Expression> arguments = node.arguments();
                if (arguments.size() == 1) {
                    Debug.println(arguments.get(0).resolveConstantExpressionValue());
                    //if this was a constant string, resolveConstantExpressionValue() will be nonnull
                    if (null != arguments.get(0).resolveConstantExpressionValue()) {
                        this.LSCMethodInvocation = node;
                        return false;
                    }
                }

            }
            return true;
        }
    }

}
