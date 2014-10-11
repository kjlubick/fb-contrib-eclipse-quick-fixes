package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static util.TraversalUtil.backtrackToBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class ArrayToStringResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = backtrackToBlock(node);
        ArrayToStringVisitor atsFinder = new ArrayToStringVisitor();
        node.accept(atsFinder);

        AST ast = node.getAST();

        for (Expression arr : atsFinder.arrayExpressionsToWrap) {
            MethodInvocation wrappedExpression = ast.newMethodInvocation();

            wrappedExpression.setExpression(ast.newSimpleName("Arrays"));
            wrappedExpression.setName(ast.newSimpleName("toString"));
            wrappedExpression.arguments().add(rewrite.createMoveTarget(arr));

            rewrite.replace(arr, wrappedExpression, null);
        }

        ASTUtil.addImports(rewrite, workingUnit, "java.util.Arrays");
    }

    private static class ArrayToStringVisitor extends ASTVisitor {

        public List<Expression> arrayExpressionsToWrap = new ArrayList<>();

        private static Set<String> methodsToCheck = new HashSet<>();

        static {
            methodsToCheck.add("java.io.PrintStream.println");
            methodsToCheck.add("java.io.PrintStream.print");
            methodsToCheck.add("java.lang.StringBuilder.append");
            methodsToCheck.add("java.lang.StringBuffer.append");
        }

        @Override
        public boolean visit(MethodInvocation node) {
            // for stringBuilder.append(array); and System.out.println(one);
            if (methodsToCheck.contains(asQualifiedString(node)))
            {
                if (node.arguments().size() == 1) {
                    Expression firstArg = (Expression) node.arguments().get(0);
                    checkOperand(firstArg);
                }
            }
            return true;
        }

        private static String asQualifiedString(MethodInvocation node) {
            return String.format("%s.%s",
                    node.getExpression().resolveTypeBinding().getQualifiedName(),
                    node.getName().getIdentifier());
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(InfixExpression node) {
            // for "Hello" + array + ':' + otherArray
            if (node.getOperator() == Operator.PLUS &&
                    "java.lang.String".equals(node.resolveTypeBinding().getQualifiedName())) {
                checkOperand(node.getLeftOperand());
                checkOperand(node.getRightOperand());
                List<Expression> extendedOps = node.extendedOperands();
                for (Expression operand : extendedOps) {
                    checkOperand(operand);
                }
            }
            return true;
        }

        private void checkOperand(Expression operand) {
            if (operand.resolveTypeBinding().isArray()) {
                arrayExpressionsToWrap.add(operand);
            }
        }

    }
}
