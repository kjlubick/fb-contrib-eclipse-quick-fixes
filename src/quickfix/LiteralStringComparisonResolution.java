package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class LiteralStringComparisonResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true; // we want the type to make sure the receiver of the .equals() is a String
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        if (node instanceof Name) {
            node = node.getParent();
        }
        LSCVisitor lscFinder = new LSCVisitor();
        node.accept(lscFinder);

        MethodInvocation badMethodInvocation = lscFinder.lscMethodInvocation;  
        //System.out.println();

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
        String invokedMethodName = lscFinder.lscMethodInvocation.getName().getIdentifier();
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
        @SuppressWarnings("unchecked")
        public boolean visit(MethodInvocation node) {
            if (this.lscMethodInvocation != null) {
                return false;
            }

            // for checking the type of the receiver. Although it is tempting to try
            // node.resolveTypeBinding(), that refers to the return value.
            Expression expression = node.getExpression();
            if (expression != null) {
                ITypeBinding typeBinding = expression.resolveTypeBinding();
                if (comparisonMethods.contains(node.getName().getIdentifier()) && // check the method name
                        "java.lang.String".equals(typeBinding.getQualifiedName())) {

                    List<Expression> arguments = (List<Expression>) node.arguments();
                    if (arguments.size() == 1) { // Sanity check to make sure this isn't a look alike
                        Expression argument = arguments.get(0);
                        if (argument.resolveConstantExpressionValue() != null) {
                            // if this was a constant string, resolveConstantExpressionValue() will be nonnull
                            // We can't simply do argument instanceof StringLiteral because if we have Class.CONSTANT,
                            // that isn't a StringLiteral
                            this.lscMethodInvocation = node;
                            this.stringLiteralExpression = argument;
                            this.stringVariableExpression = expression;
                            return false;
                        }
                    }

                }
            }
            return true;
        }
    }

}
