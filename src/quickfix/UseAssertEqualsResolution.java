package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addStaticImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class UseAssertEqualsResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new UAEVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        UAEVisitor visitor = new UAEVisitor();
        node.accept(visitor);

        MethodInvocation badMethodInvocation = visitor.badMethodInvocation;

        MethodInvocation fixedMethodInvocation = createFixedMethodInvocation(rewrite, visitor);

        rewrite.replace(badMethodInvocation, fixedMethodInvocation, null);
        
        if (visitor.usedStaticAssert) { //this static import may not exist.  However, if it wasn't done statically,
            addStaticImports(rewrite, workingUnit, "org.junit.Assert.assertEquals"); // we know the Assert import will have to exist
        }
    }
    
    private MethodInvocation createFixedMethodInvocation(ASTRewrite rewrite, UAEVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation retVal = ast.newMethodInvocation();
        retVal.setName(ast.newSimpleName("assertEquals"));
        if (!visitor.usedStaticAssert) {
            retVal.setExpression(ast.newSimpleName("Assert"));
        }
        @SuppressWarnings("unchecked")
        List<Expression> arguments = retVal.arguments();    //known to be of type Expression
        arguments.add((Expression) rewrite.createCopyTarget(visitor.expectedExpression));
        arguments.add((Expression) rewrite.createCopyTarget(visitor.actualExpression));
        return retVal;
    }

    private static class UAEVisitor extends ASTVisitor implements CustomLabelVisitor {

        public boolean usedStaticAssert;
        public MethodInvocation badMethodInvocation;
        public Expression actualExpression;
        public Expression expectedExpression;
        
        @Override
        public boolean visit(MethodInvocation node) {
            if (badMethodInvocation != null) {
                return false;
            }
            
            if ("assertTrue".equals(node.getName().getIdentifier())) {
                if (this.findExpectedAndActual(node.arguments().get(0))) {
                    // if expression is not null, it was called Assert.assertTrue (most likely)
                    this.usedStaticAssert = node.getExpression() == null;
                    this.badMethodInvocation = node;
                }
            }
            return true;
        }

        //returns true if it was successful
        private boolean findExpectedAndActual(Object comparisonWithEquals) {
            if (comparisonWithEquals instanceof InfixExpression) { // ==
                return handleInfixEquals((InfixExpression) comparisonWithEquals);
            } else if (comparisonWithEquals instanceof MethodInvocation) { // .equals
                return handleDotEquals((MethodInvocation) comparisonWithEquals);
            }
            //not sure what this could be
            return false;
        }

        private boolean handleDotEquals(MethodInvocation comparisonWithEquals) {
            if (!"equals".equals(comparisonWithEquals.getName().getIdentifier()))        {
                return false;
            }
            bucketIntoExpectedActual(comparisonWithEquals.getExpression(),
                    (Expression) comparisonWithEquals.arguments().get(0));
            return true;
        }

        private boolean handleInfixEquals(InfixExpression comparisonWithEquals) {
            if (comparisonWithEquals.getOperator() != InfixExpression.Operator.EQUALS) {
                return false;
            }
            bucketIntoExpectedActual(comparisonWithEquals.getLeftOperand(), comparisonWithEquals.getRightOperand());
            return true;
        }

        private void bucketIntoExpectedActual(Expression left, Expression right) {
            //put the right argument in the expected category if it is a constant
            if (right.resolveConstantExpressionValue() != null && left.resolveConstantExpressionValue() == null) {
                actualExpression = left;
                expectedExpression = right;
            } else {
                expectedExpression = left;
                actualExpression = right;
            }
        }

        @Override
        public String getLabelReplacement() {
            return String.format("assertEquals(%s, %s)", expectedExpression.toString(), actualExpression.toString());
        }
        
    }

}
