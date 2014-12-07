package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static util.TraversalUtil.backtrackToBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class ConvertingStringLiteralsResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = backtrackToBlock(node);
        StringLiteralVisitor visitor = new StringLiteralVisitor();
        node.accept(visitor);
        
        StringLiteral newLiteral = rewrite.getAST().newStringLiteral();
        newLiteral.setLiteralValue(visitor.fixedStringLiteral);
        rewrite.replace(visitor.badMethodInvocation, newLiteral, null);
    }
    
    private static Set<String> dummyMethods = new HashSet<String>(3);
    
    static {
        dummyMethods.add("trim");
        dummyMethods.add("toLowerCase");
        dummyMethods.add("toUpperCase");
    }
    
    private class StringLiteralVisitor extends ASTVisitor {
        
        
        
        private MethodInvocation badMethodInvocation;
        private String fixedStringLiteral;

        @Override
        public boolean visit(MethodInvocation node) {
            
            if (isRedundantMethod(node)) {
                if (isInvokedOnStringLiteral(node.getExpression())) {
                    this.badMethodInvocation = node;
                    this.fixedStringLiteral = fixStringLiteral(node);
                    return false;
                }
            }
            return true;
        }

        private String fixStringLiteral(MethodInvocation node) {
            Expression expr = node.getExpression();
            
            String retVal = (String) expr.resolveConstantExpressionValue();
            if (retVal == null && expr instanceof MethodInvocation) {
                retVal = fixStringLiteral((MethodInvocation) expr);
            }
            
            if (retVal == null) {
                throw new RuntimeException("Could not find literal");
            }
            
            switch (node.getName().getIdentifier()) {
            case "trim":
                retVal = retVal.trim();
                break;
            case "toLowerCase":
                if (node.arguments().isEmpty()) {
                    retVal = retVal.toLowerCase();
                } else {
                    // TODO handle locales
                }
                break;
            case "toUpperCase":
                if (node.arguments().isEmpty()) {
                    retVal = retVal.toUpperCase();
                } else {
                    // TODO handle locales
                }
                break;
            default:
                break;
            }

            return retVal;
        }

        private boolean isInvokedOnStringLiteral(Expression expression) {
            if (!"String".equals(expression.resolveTypeBinding().getName())) {
                return false;
            }
            if (expression.resolveConstantExpressionValue() != null) {
                return true;
            }
            if (expression instanceof MethodInvocation) {
                MethodInvocation mi = (MethodInvocation) expression;
                return isRedundantMethod(mi) && isInvokedOnStringLiteral(mi.getExpression());
            }
            return false;
        }

        private boolean isRedundantMethod(MethodInvocation node) {
            if (!dummyMethods.contains(node.getName().getIdentifier())) {
                return false;
            }
            List<Expression> arguments = node.arguments();
            if (arguments.isEmpty()) {
                return true;
            }
            return !(arguments.get(0) instanceof MethodInvocation);
        }
        
    }

}
