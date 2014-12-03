package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class ChangeEnumEqualsResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true; // we need this to detect
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);
        EnumEqualsVisitor visitor = new EnumEqualsVisitor();
        node.accept(visitor);

        for (ResolutionBundle bundle : visitor.resolutionBundles) {
            InfixExpression newEquals = rewrite.getAST().newInfixExpression();
            if (bundle.wasNegated) {
                newEquals.setOperator(InfixExpression.Operator.NOT_EQUALS);
            } else {
                newEquals.setOperator(InfixExpression.Operator.EQUALS);
            }

            newEquals.setLeftOperand((Expression) rewrite.createCopyTarget(bundle.thisEnum));
            newEquals.setRightOperand((Expression) rewrite.createCopyTarget(bundle.thatEnum));

            rewrite.replace(bundle.badEqualsInvocation, newEquals, null);
        }
    }

    private static class ResolutionBundle {
        public boolean wasNegated;

        public Expression badEqualsInvocation;

        public Expression thisEnum;

        public Expression thatEnum;

        public ResolutionBundle(MethodInvocation badEqualsInvocation, Expression thisEnum, Expression thatEnum) {
            this.badEqualsInvocation = badEqualsInvocation;
            this.thisEnum = thisEnum;
            this.thatEnum = thatEnum;
        }
    }

    private static class EnumEqualsVisitor extends ASTVisitor {

        public List<ResolutionBundle> resolutionBundles = new ArrayList<>();

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(MethodInvocation node) {
            List<Expression> arguments = node.arguments();
            if ("equals".equals(node.getName().getIdentifier()) && arguments.size() == 1) {

                Expression messageReciever = node.getExpression();
                if (messageReciever.resolveTypeBinding().isEnum()) {
                    ResolutionBundle resolutionBundle = new ResolutionBundle(node, messageReciever, arguments.get(0));
                    checkNegated(node, resolutionBundle);
                    resolutionBundles.add(resolutionBundle);
                }
            }
            return true;
        }

        private void checkNegated(MethodInvocation node, ResolutionBundle resolutionBundle) {
            PrefixExpression negatedExpression = TraversalUtil.findClosestAncestor(node, PrefixExpression.class);
            if (negatedExpression == null) {
                return;
            }
            if (negatedExpression.getOperator().equals(PrefixExpression.Operator.NOT)) {
                resolutionBundle.wasNegated = true;
                resolutionBundle.badEqualsInvocation = negatedExpression;
            }
        }
    }

}
