package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

public class LiteralStringComparisonResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        LSCVisitor lscFinder = findLSCOccurrence(workingUnit, bug);

        MethodInvocation badMethodInvocation = lscFinder.lscMethodInvocation;

        MethodInvocation fixedMethodInvocation = createFixedMethodInvocation(rewrite, lscFinder);

        rewrite.replace(badMethodInvocation, fixedMethodInvocation, null);
    }

    private LSCVisitor findLSCOccurrence(CompilationUnit workingUnit, BugInstance bug) throws ASTNodeNotFoundException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        LSCVisitor lscFinder = new LSCVisitor();
        node.accept(lscFinder);
        return lscFinder;
    }

    @SuppressWarnings("unchecked")
    private MethodInvocation createFixedMethodInvocation(ASTRewrite rewrite, LSCVisitor lscFinder) {
        AST ast = rewrite.getAST();
        MethodInvocation fixedMethodInvocation = ast.newMethodInvocation();
        String invokedMethodName = lscFinder.lscMethodInvocation.getName().toString();
        fixedMethodInvocation.setName(ast.newSimpleName(invokedMethodName));
        // can't simply use visitor.stringLiteralExpression because an IllegalArgumentException
        // will be thrown because it belongs to another AST. So, we use a moveTarget to eventually
        // move the literal into the right place
        fixedMethodInvocation.setExpression((Expression) rewrite.createMoveTarget(lscFinder.stringLiteralExpression)); // thing the method is called on
        fixedMethodInvocation.arguments().add((Expression) rewrite.createMoveTarget(lscFinder.stringVariableExpression));
        return fixedMethodInvocation;
    }

    private static class LSCVisitor extends ASTVisitor {

        private static Set<String> comparisonMethods = new HashSet<String>(3);
        static {
            comparisonMethods.add("equals");
            comparisonMethods.add("compareTo");
            comparisonMethods.add("equalsIgnoreCase");
        }

        public MethodInvocation lscMethodInvocation;

        public Expression stringLiteralExpression;

        public Expression stringVariableExpression;

        @Override
        public boolean visit(MethodInvocation node) {
            if (this.lscMethodInvocation != null) {
                return false;
            }
            if (comparisonMethods.contains(node.getName().toString())) {

                @SuppressWarnings("unchecked")
                List<Expression> arguments = (List<Expression>) node.arguments();
                if (arguments.size() == 1) { // I doubt this could be anything other than 1
                    // if this was a constant string, resolveConstantExpressionValue() will be nonnull
                    Expression argument = arguments.get(0);
                    if (null != argument.resolveConstantExpressionValue()) {
                        this.lscMethodInvocation = node;
                        this.stringLiteralExpression = argument;
                        this.stringVariableExpression = node.getExpression();
                        return false;
                    }
                }

            }
            return true;
        }
    }

}
