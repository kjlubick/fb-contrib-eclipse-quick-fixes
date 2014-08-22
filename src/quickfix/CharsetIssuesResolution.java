package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.Debug;

import com.mebigfatguy.fbcontrib.detect.CharsetIssues;
import com.mebigfatguy.fbcontrib.detect.CharsetIssues.CSI_Pair;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;


public class CharsetIssuesResolution extends BugResolution {

	private boolean isName;

	@Override
	protected boolean resolveBindings() {
		return true;
	}
	
	@Override
	public void setOptions(@Nonnull Map<String, String> options){
		isName = Boolean.parseBoolean(options.get("isName"));
	}

	@Override
	protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug)
			throws BugResolutionException {
		// TODO Auto-generated method stub
		//CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET 
		
		Debug.println(CharsetIssues.REPLACEABLE_ENCODING_METHODS);
	
		CSIVisitor csiFinder = findCSIOccurrence(workingUnit, bug);	

        ClassInstanceCreation badMethodInvocation = csiFinder.lscMethodInvocation;

        ConstructorInvocation fixedMethodInvocation = createFixedConstructorInvocation(rewrite, csiFinder);

        rewrite.replace(badMethodInvocation, fixedMethodInvocation, null);
	}
	
	@SuppressWarnings("unchecked")
	private ConstructorInvocation createFixedConstructorInvocation(ASTRewrite rewrite, CSIVisitor csiFinder) {
		AST ast = rewrite.getAST();
        ConstructorInvocation fixedMethodInvocation = ast.newConstructorInvocation();
        
//        fixedMethodInvocation.t
//        
//        String invokedMethodName = csiFinder.lscMethodInvocation.getName().toString();
//		fixedMethodInvocation.setName(ast.newSimpleName(invokedMethodName));
//        
//		List<Expression> oldArgs = csiFinder.lscMethodInvocation.arguments();
//		
//		for(int i = 0; i< oldArgs.size(); i++) {
//			if (i != csiFinder.argumentIndex) {
//				fixedMethodInvocation.arguments().add(rewrite.createCopyTarget(oldArgs.get(i)));
//			} else {
//				fixedMethodInvocation.arguments().add(makeReplacedArgument(ast, csiFinder.stringLiteralExpression));
//			}
//		}
		
		return fixedMethodInvocation;
	}

	private Expression makeReplacedArgument(AST ast, String stringLiteralExpression) {
		
		if (isName) {
			return ast.newQualifiedName(ast.newName("StandardCharsets"), 
					ast.newSimpleName(stringLiteralExpression));
		}
		return ast.newQualifiedName(ast.newName("StandardCharsets"), 
				ast.newSimpleName(stringLiteralExpression));
	}

	private CSIVisitor findCSIOccurrence(CompilationUnit workingUnit, BugInstance bug) throws ASTNodeNotFoundException {
		ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
		CSIVisitor lscFinder = new CSIVisitor(isName);
        node.accept(lscFinder);
		return lscFinder;
	}

	private static class CSIVisitor extends ASTVisitor {

    	private static Map<String, ? extends Object> comparisonMethods;
    	
        public ClassInstanceCreation lscMethodInvocation;
        public String stringLiteralExpression;
        public int argumentIndex;

		private boolean isName;

        public CSIVisitor(boolean isName) {
			if (isName) {
				comparisonMethods = CharsetIssues.UNREPLACEABLE_ENCODING_METHODS;
			} else {
				comparisonMethods = CharsetIssues.REPLACEABLE_ENCODING_METHODS;
			}
			this.isName = isName;
		}
        
		@SuppressWarnings("unchecked")
		@Override
        public boolean visit(ClassInstanceCreation node) {
            if (this.lscMethodInvocation != null) {
                return false;
            }
            ITypeBinding resolveBinding = node.getType().resolveBinding();
			String invokedConstructorType = resolveBinding.getQualifiedName();
			if (comparisonMethods.containsKey(invokedConstructorType)) {
            	List<Expression> arguments = (List<Expression>) node.arguments();
            		//if this was a constant string, resolveConstantExpressionValue() will be nonnull
            		int indexOfArgument = getIndexOfArgument(invokedConstructorType);
            		
            		if (arguments.size() > indexOfArgument) {
            			Expression argument = arguments.get(indexOfArgument);
            			if (null != argument.resolveConstantExpressionValue()) {
            				this.lscMethodInvocation = node;
            				this.stringLiteralExpression = (String) argument.resolveConstantExpressionValue();
            				this.argumentIndex = indexOfArgument;
            				return false;
            			}
            		}
            }
            return true;
        }

		private int getIndexOfArgument(String invokedMethodName) {
			Object indexVal = comparisonMethods.get(invokedMethodName);
			if (isName && indexVal instanceof Integer) {
				return (Integer) indexVal;
			}
			else if (indexVal instanceof CSI_Pair){
				return ((CSI_Pair) indexVal).indexOfStringSig;
			}
			return -1;	//shouldn't happen
		}
    }

}
