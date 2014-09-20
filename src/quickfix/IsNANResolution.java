package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelBugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class IsNANResolution extends CustomLabelBugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        IsNANVisitor visitor = new IsNANVisitor();
        node.accept(visitor);

        Expression fixedExpression = makeFixedExpression(rewrite, visitor);
        rewrite.replace(visitor.infixToReplace, fixedExpression, null);
    }

    private static String doubleOrFloat(boolean isDouble) {
        return isDouble ? "Double" : "Float";
    }

    @SuppressWarnings("unchecked")
    private Expression makeFixedExpression(ASTRewrite rewrite, IsNANVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation fixedMethod = ast.newMethodInvocation();
        fixedMethod.setName(ast.newSimpleName("isNaN"));

        if (visitor.isPrimitive) {
            // make a reference to Double or Float
            SimpleName staticType = ast.newSimpleName(doubleOrFloat(visitor.isDouble));
            fixedMethod.setExpression(staticType);
            fixedMethod.arguments().add(rewrite.createMoveTarget(visitor.testedVariable));
        } else {
            // call isNaN directly on the boxed variable
            fixedMethod.setExpression((Expression) rewrite.createMoveTarget(visitor.testedVariable));
        }

        if (!visitor.isEquals) {
            PrefixExpression not = ast.newPrefixExpression();
            not.setOperator(PrefixExpression.Operator.NOT);
            not.setOperand(fixedMethod);
            return not;
        }

        return fixedMethod;
    }

    private static class IsNANVisitor extends CustomLabelVisitor {

        public InfixExpression infixToReplace;

        public SimpleName testedVariable;

        public boolean isEquals;

        public boolean isDouble;

        public boolean isPrimitive;

        @Override
        @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS",
                justification = "Fixing the duplications would not impact performance and probably harm readibility")
        public boolean visit(InfixExpression node) {
            if (infixToReplace != null) {
                return false;
            }
            this.infixToReplace = node;

            if (node.getOperator() == Operator.EQUALS) {
                this.isEquals = true;
            } else if (node.getOperator() == Operator.NOT_EQUALS) {
                this.isEquals = false;
            }

            if (node.getLeftOperand() instanceof SimpleName) {
                handleVariable((SimpleName) node.getLeftOperand());
            } else if (node.getRightOperand() instanceof SimpleName) {
                handleVariable((SimpleName) node.getRightOperand());
            }

            return true;
        }

        private void handleVariable(SimpleName side) {
            this.testedVariable = side;
            ITypeBinding type = this.testedVariable.resolveTypeBinding();
            this.isPrimitive = type.isPrimitive();

            String name = type.getName();
            this.isDouble = "double".equals(name) || "Double".equals(name);
        }

        @Override
        public String getLabelReplacement() {
            if (isPrimitive) {
                return String.format("a call to %s%s.isNaN(%s)", isEquals ? "" : "!",
                        doubleOrFloat(isDouble), testedVariable.getIdentifier());
            }
            return String.format("%s%s.isNaN()", isEquals ? "" : "!", testedVariable.getIdentifier());
        }

    }

    @Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        return new IsNANVisitor();
    }

}
