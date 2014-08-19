package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.Debug;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

public class LiteralStringComparisonResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        Debug.println("Do work here");

        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        Debug.println(node);
        LSCVisitor visitor = new LSCVisitor();
        node.accept(visitor);

        MethodInvocation badMethodInvocation = visitor.lscMethodInvocation;

        AST ast = rewrite.getAST();
        MethodInvocation fixedMethodInvocation = ast.newMethodInvocation();
        fixedMethodInvocation.setName(ast.newSimpleName(visitor.lscMethodInvocation.getName().toString()));
        //can't simply use visitor.stringLiteralExpression because an IllegalArgumentException
        //will be thrown because it belongs to another AST.  So, we use a moveTarget to eventually
        //move the literal into the right place
        fixedMethodInvocation.setExpression((Expression) rewrite.createMoveTarget(visitor.stringLiteralExpression));  //thing the method is called on
        fixedMethodInvocation.arguments().add((Expression) rewrite.createMoveTarget(visitor.stringVariableExpression));

        rewrite.replace(badMethodInvocation, fixedMethodInvocation, null);
    }

    private static class LSCVisitor extends ASTVisitor {

    	private static Set<String> comparisonMethods = new HashSet<String>(3);
    	static {
    		comparisonMethods.add("equals");
    		comparisonMethods.add("compareTo");
    		comparisonMethods.add("equalsIgnoreCase");
    	}
    	
        public MethodInvocation lscMethodInvocation;
        public Expression stringLiteralExpression;
        public Expression stringVariableExpression;

        @Override
        public boolean visit(MethodInvocation node) {
            Debug.println();
            Debug.println(node);
            if (this.lscMethodInvocation != null) {
                return false;
            }
            Debug.println(node.getName());
            Debug.println(node.getExpression());
            Debug.println(node.arguments());
            if (comparisonMethods.contains(node.getName().toString())) {
                Debug.println("was equals, or related");

                List<Expression> arguments = (List<Expression>) node.arguments();
                if (arguments.size() == 1) {        // I doubt this could be anything other than 1
                    Debug.println(arguments.get(0).resolveConstantExpressionValue());
                    //if this was a constant string, resolveConstantExpressionValue() will be nonnull
                    if (null != arguments.get(0).resolveConstantExpressionValue()) {
                        this.lscMethodInvocation = node;
                        this.stringLiteralExpression = arguments.get(0);
                        this.stringVariableExpression = node.getExpression();
                        return false;
                    }
                }

            }
            return true;
        }
    }

}
