package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
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
        ReturnStatement retVal = rewrite.getAST().newReturnStatement();
        retVal.setExpression((Expression) rewrite.createCopyTarget(visitor.unnecessaryStoreExpression));
        return retVal;
    }

    private static class USBRVisitor extends ASTVisitor {

        public Statement unnecessaryStoreStatement;

        public Expression unnecessaryStoreExpression;

        public ReturnStatement originalReturn;

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
                this.unnecessaryStoreStatement = storeStatement;
                this.unnecessaryStoreExpression = ((Assignment) storeExpression).getRightHandSide();
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
