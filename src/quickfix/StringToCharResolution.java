package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class StringToCharResolution extends BugResolution {

    private boolean useStringBuilder;

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    public void setOptions(Map<String, String> options) {
        useStringBuilder = Boolean.parseBoolean(options.get("useStringBuilder"));
    }
    
    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new StringToCharVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);
        
        StringToCharVisitor visitor = new StringToCharVisitor();
        node.accept(visitor);

        AST ast = rewrite.getAST();

        //TODO actually fix the problem.
//        for (Map.Entry<StringLiteral, Character> entry : visitor.replacements.entrySet()) {
//            CharacterLiteral charLiteral = ast.newCharacterLiteral();
//            charLiteral.setCharValue(entry.getValue());
//
//            rewrite.replace(entry.getKey(), charLiteral, null);
//        }
    }

    private class StringToCharVisitor extends ASTVisitor implements ApplicabilityVisitor {
        private static final String STRING_IDENTIFIER = "java.lang.String";

        List<Expression> nodesBeingConcatenated = new ArrayList<>();

        private InfixExpression infixExpression;

        @Override
        public boolean visit(InfixExpression node) {
            if (!(node.getOperator() == InfixExpression.Operator.PLUS &&
                    STRING_IDENTIFIER.equals(node.resolveTypeBinding().getQualifiedName()))) {
                return true;
            }
            this.infixExpression = node;
            nodesBeingConcatenated.add(node.getLeftOperand());
            nodesBeingConcatenated.add(node.getRightOperand());
            
            @SuppressWarnings("unchecked")
            List<Expression> extendedOperations = node.extendedOperands();
            
            for(Expression expression: extendedOperations) {
                nodesBeingConcatenated.add(expression);
            }
            
            return false;
        }

        @Override
        public boolean isApplicable() {
            if (infixExpression == null) {
                return false;
            }
            if (useStringBuilder) {
                return true;        // this is a safe fallback
            }
            
            if (isNonReplaceableString(infixExpression.getLeftOperand())) {
                return true;
            }
            return isNonReplaceableString(infixExpression.getRightOperand());
            
        }

        private boolean isNonReplaceableString(Expression expression) {
            // TODO worry about String constants?  See unit test, line 37 for more
            if (expression instanceof StringLiteral) {
                if (((StringLiteral) expression).getLiteralValue().length() > 1) {
                    return true;  
                }
            } else if (STRING_IDENTIFIER.equals(expression.resolveTypeBinding().getQualifiedName())) {
                return true;
            }
            return false;
        }
        

    }

}
