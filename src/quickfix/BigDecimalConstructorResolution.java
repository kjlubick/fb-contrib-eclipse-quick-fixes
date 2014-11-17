package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class BigDecimalConstructorResolution extends BugResolution {

    private boolean useConstructor;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        useConstructor = Boolean.parseBoolean(options.get("useConstructor"));
    }

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new BigDecimalVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        BigDecimalVisitor visitor = new BigDecimalVisitor();
        node.accept(visitor);

        Expression fixedExpression = makeFixedExpression(rewrite, visitor);
        rewrite.replace(visitor.badBigDecimalConstructor, fixedExpression, null);
    }

    private Expression makeFixedExpression(ASTRewrite rewrite, BigDecimalVisitor visitor) {
        if (useConstructor) {
            return makeConstructor(rewrite, visitor);
        }
        return makeValueOf(rewrite, visitor);
    }

    @SuppressWarnings("unchecked")
    private Expression makeValueOf(ASTRewrite rewrite, BigDecimalVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation fixedMethod = ast.newMethodInvocation();
        fixedMethod.setName(ast.newSimpleName("valueOf"));
        SimpleName staticType = ast.newSimpleName("BigDecimal");
        fixedMethod.setExpression(staticType);
        fixedMethod.arguments().add(rewrite.createMoveTarget(visitor.decimalVar));

        return fixedMethod;
    }

    @SuppressWarnings("unchecked")
    private Expression makeConstructor(ASTRewrite rewrite, BigDecimalVisitor visitor) {
        AST ast = rewrite.getAST();
        ClassInstanceCreation fixedConstructor = ast.newClassInstanceCreation();
        fixedConstructor.setType((Type) rewrite.createCopyTarget(visitor.badBigDecimalConstructor.getType()));

        StringLiteral stringLiteral = ast.newStringLiteral();
        stringLiteral.setLiteralValue(visitor.decimalVar.getToken());
        fixedConstructor.arguments().add(stringLiteral);
        return fixedConstructor;
    }

    private static class BigDecimalVisitor extends ASTVisitor implements CustomLabelVisitor {

        public ClassInstanceCreation badBigDecimalConstructor = null;

        private NumberLiteral decimalVar;

        @Override
        public boolean visit(ClassInstanceCreation node) {
            if (badBigDecimalConstructor != null) {
                return false;
            }
            Type type = node.getType();
            if (type instanceof SimpleType
                    && "BigDecimal".equals(((SimpleType) type).getName().getFullyQualifiedName())) {

                @SuppressWarnings("unchecked")
                List<Expression> args = node.arguments();
                if (args.size() == 1 && args.get(0) instanceof NumberLiteral) {
                    badBigDecimalConstructor = node;
                    this.decimalVar = (NumberLiteral) node.arguments().get(0);
                }
            }

            return true;
        }

        @Override
        public String getLabelReplacement() {
            return decimalVar.getToken();
        }

    }

}
