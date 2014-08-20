package quickfix;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;


import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;


public class CharsetIssuesResolution extends BugResolution {

	@Override
	protected boolean resolveBindings() {
		return true;
	}

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug)
			throws BugResolutionException {
		// TODO Auto-generated method stub
		//CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET 
		
		
	
//		CSIVisitor lscFinder = findLSCOccurrence(workingUnit, bug);	
//
//        MethodInvocation badMethodInvocation = lscFinder.lscMethodInvocation;
//
//        MethodInvocation fixedMethodInvocation = createFixedMethodInvocation(rewrite, lscFinder);
//
//        rewrite.replace(badMethodInvocation, fixedMethodInvocation, null);
	}
	
	private static class CSIVisitor extends ASTVisitor {

    	private static Set<String> comparisonMethods = new HashSet<String>(3);
    	static {
    		comparisonMethods.add("equals");
    		comparisonMethods.add("compareTo");
    		comparisonMethods.add("equalsIgnoreCase");
    	}
    	
        public MethodInvocation lscMethodInvocation;
        public Expression stringLiteralExpression;
        public Expression stringVariableExpression;

        @SuppressWarnings("unchecked")
		@Override
        public boolean visit(MethodInvocation node) {
            if (this.lscMethodInvocation != null) {
                return false;
            }
            List<Expression> arguments = (List<Expression>) node.arguments();
            if (arguments.size() == 1) {        // I doubt this could be anything other than 1
            	//if this was a constant string, resolveConstantExpressionValue() will be nonnull
            	Expression argument = arguments.get(0);
            	if (null != argument.resolveConstantExpressionValue()) {
            		this.lscMethodInvocation = node;
            		this.stringLiteralExpression = argument;
            		this.stringVariableExpression = node.getExpression();
            		return false;
            	}
            }
            return true;
        }
    }

}
