package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static util.TraversalUtil.backtrackToBlock;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class IsEmptyResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = backtrackToBlock(node);
        IsEmptyVisitor visitor = new IsEmptyVisitor();
        node.accept(visitor);

        for (ResolutionBundle bundle : visitor.resolutionBundles) {

            Expression newIsEmpty = makeCallToIsEmpty(rewrite, bundle);

            rewrite.replace(bundle.badEqualsCheck, newIsEmpty, null);
        }
    }

    private Expression makeCallToIsEmpty(ASTRewrite rewrite, ResolutionBundle bundle) {

        AST ast = rewrite.getAST();
        MethodInvocation callToEmpty = ast.newMethodInvocation();
        callToEmpty.setName(ast.newSimpleName("isEmpty"));
        callToEmpty.setExpression((Expression) rewrite.createCopyTarget(bundle.collectionToFix));

        if (bundle.wasNegated) {
            PrefixExpression negatedExpression = ast.newPrefixExpression();
            negatedExpression.setOperator(PrefixExpression.Operator.NOT);
            negatedExpression.setOperand(callToEmpty);
            return negatedExpression;
        }

        return callToEmpty;
    }

    private static class ResolutionBundle {
        public boolean wasNegated;

        public Expression badEqualsCheck;

        public Expression collectionToFix;

        public ResolutionBundle(Expression collectionToFix, InfixExpression badEqualsCheck, boolean wasNegated) {
            this.collectionToFix = collectionToFix;
            this.badEqualsCheck = badEqualsCheck;
            this.wasNegated = wasNegated;
        }

    }

    private static class IsEmptyVisitor extends ASTVisitor {

        public List<ResolutionBundle> resolutionBundles = new ArrayList<>();

        @Override
        public boolean visit(InfixExpression node) {
            if (node.getOperator() == InfixExpression.Operator.EQUALS ||
                    node.getOperator() == InfixExpression.Operator.NOT_EQUALS) {
                Expression left = node.getLeftOperand();
                Expression right = node.getRightOperand();
                Object rightConst = right.resolveConstantExpressionValue();
                Object leftConst = left.resolveConstantExpressionValue();
                if (left instanceof MethodInvocation && rightConst instanceof Integer) {
                    if (rightConst.equals(0)) {
                        foundPotentialNewCollection((MethodInvocation) left, node);
                    }
                } else if (right instanceof MethodInvocation && leftConst instanceof Integer) {
                    if (leftConst.equals(0)) {
                        foundPotentialNewCollection((MethodInvocation) right, node);
                    }
                }
            }
            return true;
        }

        private void foundPotentialNewCollection(MethodInvocation callToSize, InfixExpression node) {
            if ("size".equals(callToSize.getName().getIdentifier())) {
                resolutionBundles.add(new ResolutionBundle(callToSize.getExpression(), node,
                        node.getOperator() == InfixExpression.Operator.NOT_EQUALS));
            }
        }
    }

}
