package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class UnnecessaryStoreResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return false; // don't need type bindings
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        USBRVisitor visitor = new USBRVisitor();
        node.accept(visitor);

        if (visitor.unnecessaryStoreStatement != null &&
                visitor.unnecessaryStoreExpression != null &&
                visitor.originalReturn != null) {

            ReturnStatement newReturnStatement = makeNewReturnStatement(rewrite, visitor);

            rewrite.remove(visitor.unnecessaryStoreStatement, null);
            rewrite.replace(visitor.originalReturn, newReturnStatement, null);
        }

    }

    private ReturnStatement makeNewReturnStatement(ASTRewrite rewrite, USBRVisitor visitor) {
        AST ast = rewrite.getAST();
        ReturnStatement retVal = ast.newReturnStatement();
        Expression baseExpression = (Expression) rewrite.createCopyTarget(visitor.unnecessaryStoreExpression);

        Operator assignOperator = visitor.unnecessaryStoreOperator;

        if (assignOperator != null && assignOperator != Operator.ASSIGN) {
            InfixExpression infixExpression = ast.newInfixExpression();
            infixExpression.setLeftOperand((Expression) rewrite.createCopyTarget(visitor.storedVariable));
            infixExpression.setRightOperand(baseExpression);
            infixExpression.setOperator(convertAssignOperatorToInfixOperator(assignOperator));
            baseExpression = infixExpression;
        }

        retVal.setExpression(baseExpression);
        return retVal;
    }

    private InfixExpression.Operator convertAssignOperatorToInfixOperator(Operator assignOperator) {
        if (assignOperator == Operator.PLUS_ASSIGN) {
            return InfixExpression.Operator.PLUS;
        } else if (assignOperator == Operator.TIMES_ASSIGN) {
            return InfixExpression.Operator.TIMES;
        } else if (assignOperator == Operator.MINUS_ASSIGN) {
            return InfixExpression.Operator.MINUS;
        } else if (assignOperator == Operator.DIVIDE_ASSIGN) {
            return InfixExpression.Operator.DIVIDE;
        } else if (assignOperator == Operator.REMAINDER_ASSIGN) {
            return InfixExpression.Operator.REMAINDER;
        } else if (assignOperator == Operator.LEFT_SHIFT_ASSIGN) {
            return InfixExpression.Operator.LEFT_SHIFT;
        } else if (assignOperator == Operator.RIGHT_SHIFT_SIGNED_ASSIGN) {
            return InfixExpression.Operator.RIGHT_SHIFT_SIGNED;
        } else if (assignOperator == Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
            return InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
        } else if (assignOperator == Operator.RIGHT_SHIFT_UNSIGNED_ASSIGN) {
            return InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED;
        } else if (assignOperator == Operator.BIT_AND_ASSIGN) {
            return InfixExpression.Operator.AND;
        } else if (assignOperator == Operator.BIT_OR_ASSIGN) {
            return InfixExpression.Operator.OR;
        } else if (assignOperator == Operator.BIT_XOR_ASSIGN) {
            return InfixExpression.Operator.XOR;
        }
        return null;
    }

    private static class USBRVisitor extends ASTVisitor {

        public Statement unnecessaryStoreStatement;

        public Expression unnecessaryStoreExpression;

        public ReturnStatement originalReturn;

        public Operator unnecessaryStoreOperator;

        private Expression storedVariable;

        @Override
        public boolean visit(ReturnStatement node) {
            if (originalReturn != null) {
                return false;
            }

            if (node.getExpression() instanceof SimpleName) {
                this.originalReturn = node;
                findUnnecessaryStore(node);
            }

            return true;
        }

        private void findUnnecessaryStore(ReturnStatement node) {
            Block block = TraversalUtil.findClosestAncestor(node, Block.class);
            @SuppressWarnings("unchecked")
            List<Statement> blockStatements = block.statements();
            for (int i = 1; i < blockStatements.size(); i++) {
                Statement statement = blockStatements.get(i);
                if (statement == this.originalReturn) {
                    for (int j = i - 1; j >= 0; j--) {
                        Statement storeStatement = blockStatements.get(j);
                        if (storeStatement instanceof VariableDeclarationStatement) {
                            splitStatementAndInitializer((VariableDeclarationStatement) storeStatement);
                            return;
                        } else if (storeStatement instanceof ExpressionStatement) {
                            if (splitStatementAndInitializer((ExpressionStatement) storeStatement)) {
                                // we found our extra storage statement
                                return;
                            }
                        }
                    }
                }
            }
        }

        private boolean splitStatementAndInitializer(ExpressionStatement storeStatement) {
            Expression storeExpression = storeStatement.getExpression();
            if (storeExpression instanceof Assignment) {
                Assignment assignment = (Assignment) storeExpression;
                this.unnecessaryStoreStatement = storeStatement;

                this.storedVariable = assignment.getLeftHandSide();
                this.unnecessaryStoreOperator = assignment.getOperator();
                this.unnecessaryStoreExpression = assignment.getRightHandSide();

                return true;
            }
            return false;
        }

        private void splitStatementAndInitializer(VariableDeclarationStatement statement) {
            this.unnecessaryStoreStatement = statement;
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) statement.fragments().get(0);
            this.unnecessaryStoreExpression = fragment.getInitializer();
        }

    }

}
