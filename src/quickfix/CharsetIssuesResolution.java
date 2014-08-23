package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import org.apache.bcel.generic.Type;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import com.mebigfatguy.fbcontrib.detect.CharsetIssues;

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

		CSIVisitor csiFinder = findCSIOccurrence(workingUnit, bug);	

        ASTNode badMethodInvocation = csiFinder.getBadInvocation();

        ASTNode fixedMethodInvocation = csiFinder.createFixedInvocation(rewrite);

        rewrite.replace(badMethodInvocation, fixedMethodInvocation, null);
	}
	
//	@SuppressWarnings("unchecked")
//	private ConstructorInvocation createFixedConstructorInvocation(ASTRewrite rewrite, CSIVisitor csiFinder) {
//		AST ast = rewrite.getAST();
//        ConstructorInvocation fixedMethodInvocation = ast.newConstructorInvocation();
//        
////        fixedMethodInvocation.t
////        
////        String invokedMethodName = csiFinder.lscMethodInvocation.getName().toString();
////		fixedMethodInvocation.setName(ast.newSimpleName(invokedMethodName));
////        
////		List<Expression> oldArgs = csiFinder.lscMethodInvocation.arguments();
////		
////		for(int i = 0; i< oldArgs.size(); i++) {
////			if (i != csiFinder.argumentIndex) {
////				fixedMethodInvocation.arguments().add(rewrite.createCopyTarget(oldArgs.get(i)));
////			} else {
////				fixedMethodInvocation.arguments().add(makeReplacedArgument(ast, csiFinder.stringLiteralExpression));
////			}
////		}
//		
//		return fixedMethodInvocation;
//	}

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
	
		private Map<QTypeAndArgs, Object> csiConstructors;
		private Map<QTypeAndArgs, Object> csiMethods;
		
	    private ClassInstanceCreation csiConstructorInvocation;
	    private MethodInvocation csiMethodInvocation;
	    private String stringLiteralExpression;
	    
	    private ASTNode fixedAstNode = null;
	    
	    public int argumentIndex;
	
		private boolean needsToInvokeName;  //should use StandardCharsets.UTF_8.name() instead of StandardCharsets
		
	
	    public CSIVisitor(boolean needsToInvokeName) {
			if (needsToInvokeName) {
				parseToTypeArgs(CharsetIssues.UNREPLACEABLE_ENCODING_METHODS);
			} else {
				parseToTypeArgs(CharsetIssues.REPLACEABLE_ENCODING_METHODS);
			}
			this.needsToInvokeName = needsToInvokeName;
		}
	    
		public ASTNode createFixedInvocation(ASTRewrite rewrite) {
			if (needsToInvokeName) {
				
			}
			return null;
		}

		public ASTNode getBadInvocation() {
			if (csiConstructorInvocation != null) {
				return csiConstructorInvocation;
			}
			return csiMethodInvocation;
		}

		private void parseToTypeArgs(Map<String, ? extends Object> map) {
			this.csiConstructors = new HashMap<>();
			this.csiMethods = new HashMap<>();
			
			for(Entry<String, ? extends Object> entry:map.entrySet()) {
				QTypeAndArgs struct = new QTypeAndArgs(entry.getKey());
				if (struct.wasConstructor) {
					csiConstructors.put(struct, entry.getValue());
				}
				else {
					csiMethods.put(struct, entry.getValue());
				}
			}
			
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public boolean visit(MethodInvocation node) {
	        if (foundThingToReplace()) {
	            return false;
	        }
	        QTypeAndArgs key = new QTypeAndArgs(node);
			
			if (csiConstructors.containsKey(key)) {
				List<Expression> arguments = (List<Expression>) node.arguments();		
				int indexOfArgumentToReplace = getIndexOfArgument(key, arguments); 
	
				//if this was a constant string, resolveConstantExpressionValue() will be nonnull
				Expression argument = arguments.get(indexOfArgumentToReplace);
				if (null != argument.resolveConstantExpressionValue()) {
					this.csiMethodInvocation = node;
					this.stringLiteralExpression = (String) argument.resolveConstantExpressionValue();
					this.argumentIndex = indexOfArgumentToReplace;
					return false;
				}
	
			}
	        return true;
		}
		
		@SuppressWarnings("unchecked")
		@Override
	    public boolean visit(ClassInstanceCreation node) {
	        if (foundThingToReplace()) {
	            return false;
	        }
			QTypeAndArgs key = new QTypeAndArgs(node);
			
			if (csiConstructors.containsKey(key)) {
				List<Expression> arguments = (List<Expression>) node.arguments();		
				int indexOfArgumentToReplace = getIndexOfArgument(key, arguments); 
	
				//if this was a constant string, resolveConstantExpressionValue() will be nonnull
				Expression argument = arguments.get(indexOfArgumentToReplace);
				if (null != argument.resolveConstantExpressionValue()) {
					this.csiConstructorInvocation = node;
					this.stringLiteralExpression = (String) argument.resolveConstantExpressionValue();
					this.argumentIndex = indexOfArgumentToReplace;
					return false;
				}
	
			}
	        return true;
	    }

		private boolean foundThingToReplace() {
			return this.csiConstructorInvocation != null || this.csiMethodInvocation != null;
		}
	
		private int getIndexOfArgument(QTypeAndArgs key, List<Expression> arguments) {
			Integer indexVal = (Integer) csiConstructors.get(key);
			// indexVal is nth to last argument, so we convert to index
			return (arguments.size() - indexVal) - 1;

		}
	}

	private static class QTypeAndArgs {
		public boolean wasConstructor;
		final String qualifiedType;
		final List<String> argumentTypes = new ArrayList<String>();
		
		//expecting in form "java/io/InputStreamReader.<init>(Ljava/io/InputStream;Ljava/lang/String;)V"
		public QTypeAndArgs(String fullSignatureWithArgs) {
			int splitIndex = fullSignatureWithArgs.indexOf('(');
			String qualifiedTypeWithSlashes = fullSignatureWithArgs.substring(0, splitIndex);
			
			qualifiedType = qualifiedTypeWithSlashes.replace('/', '.');
			
			Type[] bcelTypes = Type.getArgumentTypes(fullSignatureWithArgs.substring(splitIndex));
			
			for(Type t: bcelTypes) {
				argumentTypes.add(t.toString());		//toString returns them in human-readable dot notation
			}
			
			wasConstructor = fullSignatureWithArgs.contains("<init>");
		}
		
		private QTypeAndArgs(String qualifiedName, List<Expression> arguments) {
			this.qualifiedType = qualifiedName;
			for(Expression type: arguments) {
				argumentTypes.add(type.resolveTypeBinding().getQualifiedName());
			}
		}
	
		@SuppressWarnings("unchecked")
		public QTypeAndArgs(ClassInstanceCreation node) {
			this(node.getType().resolveBinding().getQualifiedName()+".<init>",
					node.arguments());
			wasConstructor = true;
		}
		
		@SuppressWarnings("unchecked")
		public QTypeAndArgs(MethodInvocation node) {
			this(node.resolveTypeBinding().getQualifiedName(),
					node.arguments());
			wasConstructor = false;
		}
	
		@Override
		public String toString() {
			return "QTypeAndArgs [qualifiedType=" + qualifiedType + ", argumentTypes=" + argumentTypes + "]";
		}
	
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((argumentTypes == null) ? 0 : argumentTypes.hashCode());
			result = prime * result + ((qualifiedType == null) ? 0 : qualifiedType.hashCode());
			result = prime * result + (wasConstructor ? 1231 : 1237);
			return result;
		}
	
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			QTypeAndArgs other = (QTypeAndArgs) obj;
			if (argumentTypes == null) {
				if (other.argumentTypes != null)
					return false;
			} else if (!argumentTypes.equals(other.argumentTypes))
				return false;
			if (qualifiedType == null) {
				if (other.qualifiedType != null)
					return false;
			} else if (!qualifiedType.equals(other.qualifiedType))
				return false;
			if (wasConstructor != other.wasConstructor)
				return false;
			return true;
		}
		
		
	}

}
