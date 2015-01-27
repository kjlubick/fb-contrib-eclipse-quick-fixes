package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class CompareFloatResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new CompareToVisitor();
    }
    
    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new CompareToVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);
        CompareToVisitor visitor = new CompareToVisitor();
        node.accept(visitor);

        if (visitor.expressionToReplace != null) {

            AST ast = rewrite.getAST();
            MethodInvocation newMethod = ast.newMethodInvocation();
            newMethod.setName(ast.newSimpleName("compare"));
            newMethod.setExpression(ast.newSimpleName(visitor.floatOrDouble));
            newMethod.arguments().add(rewrite.createCopyTarget(visitor.firstFloat));
            newMethod.arguments().add(rewrite.createCopyTarget(visitor.secondFloat));
            
            if (visitor.optionalTempVariableToDelete != null) {
                rewrite.remove(visitor.optionalTempVariableToDelete, null);
            }
            rewrite.replace(visitor.expressionToReplace, newMethod, null);
        }
    }

    private static class CompareToVisitor extends ASTVisitor implements ApplicabilityVisitor, CustomLabelVisitor {

        ConditionalExpression expressionToReplace;

        VariableDeclarationStatement optionalTempVariableToDelete;

        Name firstFloat;

        Name secondFloat;

        String floatOrDouble;

        @Override
        public boolean visit(ConditionalExpression node) {
            if (expressionToReplace != null) {
                return false;
            }
            
            if (node.getExpression() instanceof InfixExpression) {
                InfixExpression condExpr = (InfixExpression) node.getExpression();
                boolean retVal = findFirstAndSecondFloat(node, condExpr);
                if (condExpr.getOperator() == InfixExpression.Operator.GREATER) {
                    return retVal;
                } 
                else if (condExpr.getOperator() == InfixExpression.Operator.LESS) {
                    swapFirstAndSecondFloat();
                    return retVal;
                } 
            }
            return true;
        }

        private boolean findFirstAndSecondFloat(ConditionalExpression node, InfixExpression condExpr) {
            if (!handleTwoSimpleNames(node, condExpr)) {
                // this is a if diff > 0 case
                try {
                    if (condExpr.getLeftOperand() instanceof SimpleName) {
                        findDiffAndFloats((SimpleName) condExpr.getLeftOperand());
                    } else if (condExpr.getRightOperand() instanceof SimpleName) {
                        findDiffAndFloats((SimpleName) condExpr.getRightOperand());
                    } else {
                        return true; // unexpected comparison
                    }
                    floatOrDouble = getFloatOrDouble(firstFloat, secondFloat);
                    expressionToReplace = node;

                } catch (CouldntFindDiffException e) {
                    return true; // keep nesting if we have a problem
                }
            }
            return false;
        }

        private boolean handleTwoSimpleNames(ConditionalExpression node, InfixExpression condExpr) {
            if (!areNames(condExpr.getLeftOperand(), condExpr.getRightOperand())) {
                return false;
            }
            firstFloat = (Name) condExpr.getLeftOperand();
            secondFloat = (Name) condExpr.getRightOperand();
            expressionToReplace = node;
            floatOrDouble = getFloatOrDouble(firstFloat, secondFloat);
            return true;
        }

        private void swapFirstAndSecondFloat() {
            Name temp = firstFloat;
            firstFloat = secondFloat;
            secondFloat = temp;
        }

        @SuppressWarnings("unchecked")
        private void findDiffAndFloats(SimpleName diffName) throws CouldntFindDiffException {
            ConditionalExpression originalLine = TraversalUtil.findClosestAncestor(diffName, ConditionalExpression.class);

            if (originalLine == null || !(originalLine.getExpression() instanceof InfixExpression)) {
                throw new CouldntFindDiffException();
            }

            Block surroundingBlock = TraversalUtil.findClosestAncestor(originalLine, Block.class);

            List<Statement> blockStatements = surroundingBlock.statements();
            for (int i = blockStatements.size() - 1; i >= 0; i--) {
                Statement statement = blockStatements.get(i);
                if (statement instanceof VariableDeclarationStatement) {
                    List<VariableDeclarationFragment> frags = ((VariableDeclarationStatement) statement).fragments();

                    // I won't fix the the diff variable if it's nested with other frags, if they exist
                    // but we do need to look at them
                    VariableDeclarationFragment fragment = frags.get(0);
                    if (fragment.getName().getIdentifier().equals(diffName.getIdentifier())) {
                        Expression initializer = fragment.getInitializer();
                        if (initializer instanceof InfixExpression) {
                            InfixExpression subtraction = (InfixExpression) initializer;
                            if (subtraction.getOperator() == InfixExpression.Operator.MINUS &&
                                    areNames(subtraction.getLeftOperand(), subtraction.getRightOperand())) {

                                this.firstFloat = (Name) subtraction.getLeftOperand();
                                this.secondFloat = (Name) subtraction.getRightOperand();

                                if (frags.size() == 1) {
                                    this.optionalTempVariableToDelete = (VariableDeclarationStatement) statement;
                                }
                                return;
                            }
                        }
                    }
                }
            }
            throw new CouldntFindDiffException();
        }

        private String getFloatOrDouble(Name... variables) {
            boolean isDouble = false;
            for(Name v : variables) {
                if ("double".equals(v.resolveTypeBinding().getQualifiedName())) {
                    isDouble = true;
                }
            }
            return isDouble ? "Double" : "Float";
        }

        private boolean areNames(Expression... expressions) {  //returns true if all expressions are simple names
            for (Expression e: expressions) {
                if (!(e instanceof Name)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isApplicable() {
            return expressionToReplace != null;
        }
        
        @Override
        public String getLabelReplacement() {
            return String.format("%s.compare(%s,%s)", this.floatOrDouble, this.firstFloat, this.secondFloat);
        }
        
        
    }
    
    private static class CouldntFindDiffException extends Exception {
        
    }

}
