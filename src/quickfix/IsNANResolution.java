package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.CustomLabelVisitor;

public class IsNANResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    
    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        IsNANVisitor visitor = new IsNANVisitor();
        node.accept(visitor);
        
        AST ast = rewrite.getAST();
        
        Expression fixedExpression = makeFixedExpression(rewrite, visitor, ast);
        
        rewrite.replace(visitor.infixToReplace, fixedExpression, null);
    }


    @SuppressWarnings("unchecked")
    private Expression makeFixedExpression(ASTRewrite rewrite, IsNANVisitor visitor, AST ast) {
        Expression retVal = null;
        
        if (visitor.isPrimitive) {
            MethodInvocation fixedExpression = ast.newMethodInvocation();
            SimpleName staticType = ast.newSimpleName(visitor.isDouble? "Double" : "Float");
            fixedExpression.setExpression(staticType);
            fixedExpression.setName(ast.newSimpleName("isNaN"));
            fixedExpression.arguments().add(rewrite.createMoveTarget(visitor.testedVariable));
            
            if (!visitor.isEquals) {
                PrefixExpression not = ast.newPrefixExpression();
                not.setOperator(PrefixExpression.Operator.NOT);
                not.setOperand(fixedExpression);
                retVal = not;
            } else {
                retVal = fixedExpression;
            }
            
        } else {
            
        }
        
        return retVal;
    }
    
    
    private static class IsNANVisitor extends ASTVisitor {
        
        public InfixExpression infixToReplace;
        public Expression testedVariable;
        public boolean isEquals;
        public boolean isDouble;
        public boolean isPrimitive;
        
        @Override
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
                handleVariable(node.getLeftOperand());
            } else if (node.getRightOperand() instanceof SimpleName){
                handleVariable(node.getRightOperand());
            }
            
            return true;
        }

        private void handleVariable(Expression side) {
            this.testedVariable = side;
            ITypeBinding type = this.testedVariable.resolveTypeBinding();
            this.isPrimitive = type.isPrimitive();

            String name = type.getName();
            this.isDouble = "double".equals(name) || "Double".equals(name);
        }
        
        
    }


    //@Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        // TODO Auto-generated method stub
        return null;
    }

}
