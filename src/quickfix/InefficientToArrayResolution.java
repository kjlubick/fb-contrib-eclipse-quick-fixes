package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class InefficientToArrayResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new ITAVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        ITAVisitor visitor = new ITAVisitor();
        node.accept(visitor);

        if (visitor.collectionTurnedToArray != null) {
            ASTNode sizeBasedInitialization = createSizedBasedInitialization(rewrite, visitor);
            rewrite.replace(visitor.dimensionInitializerToReplace, sizeBasedInitialization, null);
        }
    }

    private MethodInvocation createSizedBasedInitialization(ASTRewrite rewrite, ITAVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation sizeBasedInitialization = ast.newMethodInvocation();

        sizeBasedInitialization.setExpression((Expression) rewrite.createCopyTarget(visitor.collectionTurnedToArray));
        sizeBasedInitialization.setName(ast.newSimpleName("size"));
        return sizeBasedInitialization;
    }

    private static class ITAVisitor extends ASTVisitor implements CustomLabelVisitor {

        public Expression collectionTurnedToArray = null;

        public Expression dimensionInitializerToReplace = null;

        public String arrayTypeName = null; // for label purposes

        @Override
        public boolean visit(ArrayCreation node) {
            if (dimensionInitializerToReplace != null) {
                return false;
            }

            @SuppressWarnings("unchecked")
            List<Expression> dimensions = node.dimensions();
            if (dimensions.size() != 1) {
                return true;
            }

            Expression initializer = dimensions.get(0);
            if (Integer.valueOf(0).equals(initializer.resolveConstantExpressionValue())) {
                try {
                    collectionTurnedToArray = findCollection(node);
                } catch (NotPartOfToArrayMethodInvocationException e) {
                    // bail out, we haven't found a toArray call
                    return true;
                }
                dimensionInitializerToReplace = initializer;
                arrayTypeName = findArrayTypeName(node);
                return false;
            }

            return true;

        }

        private String findArrayTypeName(ArrayCreation node) {
            return node.getType().getElementType().toString();
        }

        private Expression findCollection(ArrayCreation node) throws NotPartOfToArrayMethodInvocationException {
            ASTNode parent = node.getParent();
            if (parent instanceof MethodInvocation) {
                MethodInvocation parentMethodInvocation = (MethodInvocation) parent;
                if ("toArray".equals(parentMethodInvocation.getName().getIdentifier())) {
                    return parentMethodInvocation.getExpression();
                }
            }
            throw new NotPartOfToArrayMethodInvocationException();
        }

        @Override
        public String getLabelReplacement() {
            return String.format("new %s[%s.size()]", arrayTypeName, collectionTurnedToArray.toString());
        }
    }

    private static class NotPartOfToArrayMethodInvocationException extends Exception {
        private static final long serialVersionUID = -5779852902622201169L;
    }

}
