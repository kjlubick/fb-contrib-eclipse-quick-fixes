package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import util.TraversalUtil;

public class HTTPClientResolution extends BugResolution {

    private boolean appendToOrAddFinally;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.appendToOrAddFinally = Boolean.parseBoolean(options.get("appendToOrAddFinally"));
    }

    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new HCPVisitor();
    }

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new HCPVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        HCPVisitor visitor = new HCPVisitor();
        node.accept(visitor);

        TryStatement lastTryStatement = findLastTryStatementUsingVariable(visitor.badHTTPVerb);
        ExpressionStatement callToReset = makeCallToReset(rewrite, visitor);

        if (appendToOrAddFinally) {
            Block finallyBlock = lastTryStatement.getFinally();
            if (finallyBlock == null) {
                finallyBlock = rewrite.getAST().newBlock();
            }
            // first add the call to reset
            ListRewrite finallyRewrite = rewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY);
            finallyRewrite.insertFirst(callToReset, null);

            // replace/insert the finally block
            rewrite.set(lastTryStatement, TryStatement.FINALLY_PROPERTY, finallyBlock, null);
        } else {
            // just stick it after the try
            Block parentBlock = TraversalUtil.findClosestAncestor(lastTryStatement, Block.class);
            ListRewrite statements = rewrite.getListRewrite(parentBlock, Block.STATEMENTS_PROPERTY);
            statements.insertAfter(callToReset, lastTryStatement, null);
        }

    }

    private ExpressionStatement makeCallToReset(ASTRewrite rewrite, HCPVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation releaseMethodInvocation = ast.newMethodInvocation();
        releaseMethodInvocation.setExpression((Expression) rewrite.createCopyTarget(visitor.badHTTPVerb));
        releaseMethodInvocation.setName(ast.newSimpleName("releaseConnection"));
        return ast.newExpressionStatement(releaseMethodInvocation);
    }

    @SuppressWarnings("unchecked")
    private static TryStatement findLastTryStatementUsingVariable(SimpleName variable) {
        // looks for the last try statement that has a reference to the variable referred to
        // by the included simpleName.
        // If we can't find such a try block, we give up trying to fix
        Block parentBlock = TraversalUtil.findClosestAncestor(variable, Block.class);
        List<Statement> statements = parentBlock.statements();
        for (int i = statements.size() - 1; i >= 0; i--) {
            Statement s = statements.get(i);
            if (s instanceof TryStatement) {
                TryStatement tryStatement = (TryStatement) s;
                if (tryRefersToVariable(tryStatement, variable)) {
                    return tryStatement;
                }
            }
        }
        return null;
    }

    private static boolean tryRefersToVariable(TryStatement s, SimpleName badHTTPVerb) {
        TryInspector inspector = new TryInspector(badHTTPVerb);
        s.accept(inspector);
        return inspector.foundName;
    }

    private static Set<String> httpVerbClasses = new HashSet<String>();
    static
    {
        httpVerbClasses.add("org.apache.http.client.methods.HttpGet");
        httpVerbClasses.add("org.apache.http.client.methods.HttpPut");
        httpVerbClasses.add("org.apache.http.client.methods.HttpDelete");
        httpVerbClasses.add("org.apache.http.client.methods.HttpPost");
        httpVerbClasses.add("org.apache.http.client.methods.HttpPatch");
    }

    private class HCPVisitor extends ASTVisitor implements ApplicabilityVisitor, CustomLabelVisitor {

        public SimpleName badHTTPVerb;

        private TryStatement associatedTryStatement;

        @Override
        @SuppressFBWarnings(value = "PRMC_POSSIBLY_REDUNDANT_METHOD_CALLS", justification =
                "node.getName() does not need to local - code's concise as is")
        public boolean visit(VariableDeclarationFragment node) {
            if (badHTTPVerb != null) {
                return false;
            }

            IBinding binding = node.getName().resolveBinding();
            if (binding instanceof IVariableBinding) {
                if (httpVerbClasses.contains(((IVariableBinding) binding).getType().getQualifiedName())) {
                    badHTTPVerb = node.getName();
                    associatedTryStatement = findLastTryStatementUsingVariable(badHTTPVerb);
                    return false;
                }
            }
            return true;
        }

        @Override
        public String getLabelReplacement() {
            if (associatedTryStatement.getFinally() != null) {
                // a finally is defined
                if (appendToOrAddFinally) {
                    return "Add call to httpGet.releaseConnection() to finally block";
                } else {
                    // put resetConnection after "finally" block
                    return "finally";
                }
            } else {
                // no finally defined
                if (appendToOrAddFinally) {
                    return "Add finally block to release connections of httpGet";
                } else {
                    // put resetConnection after "catch" block
                    return "catch";
                }
            }
        }

        @Override
        public boolean isApplicable() {
            // if we can't find a verb or a try statement, it's much too complex to fix
            return null != badHTTPVerb && null != associatedTryStatement;
        }

    }

    private static class TryInspector extends ASTVisitor {
        // a simple visitor that simply keeps track of if we saw a simple name
        // with the same identifier as the SimpleName passed in

        private SimpleName nameSoughtAfter;

        public boolean foundName;

        public TryInspector(SimpleName nameSoughtAfter) {
            this.nameSoughtAfter = nameSoughtAfter;
        }

        @Override
        public boolean visit(SimpleName node) {
            if (node.getIdentifier().equals(nameSoughtAfter.getIdentifier())) {
                foundName = true;
            }
            return true;
        }
    }

}
