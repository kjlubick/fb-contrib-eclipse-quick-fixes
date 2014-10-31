package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
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
    protected ASTNode getNodeForMarker(IMarker marker) throws JavaModelException, ASTNodeNotFoundException {
        return TraversalUtil.backtrackToBlock(super.getNodeForMarker(marker));
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);
        
        StringToCharVisitor visitor = new StringToCharVisitor();
        node.accept(visitor);

        AST ast = rewrite.getAST();
        
        if (useStringBuilder) {
            ClassInstanceCreation newStringBuilder = ast.newClassInstanceCreation();
            newStringBuilder.setType(ast.newSimpleType(ast.newName("StringBuilder")));
            
            Expression lastExpression = newStringBuilder;
            for(Expression expr : visitor.nodesBeingConcatenated) {
                MethodInvocation newAppend = ast.newMethodInvocation();
              //chains the call
                newAppend.setExpression(lastExpression); 
                Expression fixedExpr = fixSingleLengthString(expr, ast);
                
                if (fixedExpr == expr) {
                    //if it wasn't fixed, we have to make a moveTarget
                    newAppend.arguments().add((Expression) rewrite.createMoveTarget(expr));
                } else {
                    // if it was fixed, it doesn't need a move target
                    newAppend.arguments().add(fixedExpr);
                }
                
                newAppend.setName(ast.newSimpleName("append"));
                lastExpression = newAppend;
            }
            
            MethodInvocation toString = ast.newMethodInvocation();
            toString.setExpression(lastExpression);
            toString.setName(ast.newSimpleName("toString"));
            
            rewrite.replace(visitor.infixExpression, toString, null);
            
        } else {
            for(Expression expr:visitor.nodesBeingConcatenated) {
                Expression fixedExpr = fixSingleLengthString(expr, ast);
                if (fixedExpr != expr) { //reference equality is fine here 
                    rewrite.replace(expr, fixedExpr, null);
                }
            }
        }
        
        for (Map.Entry<StringLiteral, Character> entry : visitor.replacements.entrySet()) {
            CharacterLiteral charLiteral = ast.newCharacterLiteral();
            charLiteral.setCharValue(entry.getValue());

            rewrite.replace(entry.getKey(), charLiteral, null);
        }
    }
    
    private static Expression fixSingleLengthString(Expression expr, AST ast) {
        if (expr instanceof StringLiteral) {
            String literalValue = ((StringLiteral) expr).getLiteralValue();
            if (literalValue.length() == 1) {
                CharacterLiteral charLiteral = ast.newCharacterLiteral();
                charLiteral.setCharValue(literalValue.charAt(0));
                return charLiteral;
            }
        }
        return expr;
    }
    

    private class StringToCharVisitor extends ASTVisitor implements ApplicabilityVisitor {
        private static final String STRING_IDENTIFIER = "java.lang.String";

        public List<Expression> nodesBeingConcatenated = new ArrayList<>();

        public InfixExpression infixExpression;
        
        // This maps length 1 strings that are not in infixExpressions
        // to its appropriate replacement
        public Map<StringLiteral, Character> replacements = new HashMap<>();

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
            
            return false;       //prevent traversal to any String Literals
        }
        
        
        @Override
        public boolean visit(StringLiteral node) {
            String literalValue = node.getLiteralValue();
            if (literalValue.length() == 1) {
                replacements.put(node, literalValue.charAt(0));
            }
            return false;
        }
        

        @Override
        public boolean isApplicable() {
            if (useStringBuilder) {
                return infixExpression != null;        
            }
            
            if (!replacements.isEmpty()) {
                return true;
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
