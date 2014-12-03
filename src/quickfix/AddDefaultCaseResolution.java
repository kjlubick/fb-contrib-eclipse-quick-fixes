package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import util.TraversalUtil;

public class AddDefaultCaseResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);
        DefaultCaseVisitor visitor = new DefaultCaseVisitor();
        node.accept(visitor);

        if (visitor.badSwitchStatement != null) {
            ListRewrite statementsRewrite = rewrite.getListRewrite(visitor.badSwitchStatement,
                    SwitchStatement.STATEMENTS_PROPERTY);
            SwitchCase defaultCase = rewrite.getAST().newSwitchCase();
            defaultCase.setExpression(null);
            statementsRewrite.insertLast(defaultCase, null);
            statementsRewrite.insertLast(rewrite.getAST().newBreakStatement(), null);
        }
    }

    private static class DefaultCaseVisitor extends ASTVisitor {

        private SwitchStatement badSwitchStatement;

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(SwitchStatement node) {
            if (badSwitchStatement != null) {
                return false;
            }

            List<Statement> switchStatements = node.statements();
            for (Statement statement : switchStatements) {
                if (statement instanceof SwitchCase) {
                    if (((SwitchCase) statement).getExpression() == null) {
                        return true; // this one has a default case...skip it
                    }
                }
            }

            badSwitchStatement = node;

            return false;
        }
    }

}
