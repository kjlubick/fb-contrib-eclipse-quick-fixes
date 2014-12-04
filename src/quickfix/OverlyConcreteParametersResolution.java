package quickfix;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class OverlyConcreteParametersResolution extends BugResolution {
    
    private Pattern annotationParser = Pattern.compile(".*parameter '(.+)' could be declared as (.+) instead.*");
    private String paramName;
    private String newDotSeperatedClass;
    private SingleVariableDeclaration badParam;
    private AST ast;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        MethodDeclaration method = TraversalUtil.findEnclosingMethod(workingUnit, bug.getPrimarySourceLineAnnotation());
        
        parseParamAndNewClass(bug.getAnnotations());
        
        List<SingleVariableDeclaration> params = method.parameters();
        for(SingleVariableDeclaration param : params) {
            if (param.getName().getIdentifier().equals(paramName)) {
                badParam = param;
                break;
            }
        }
        
        ast = rewrite.getAST();
        String unqualifiedClass = newDotSeperatedClass.substring(newDotSeperatedClass.lastIndexOf('.')+1);
        
        Type newType = ast.newSimpleType(ast.newName(unqualifiedClass));
        Type oldType = badParam.getType();
        if (oldType.isParameterizedType()) {
            ParameterizedType newPType = ast.newParameterizedType(newType);
            transferTypeArguments((ParameterizedType) oldType, newPType);
            newType = newPType;
        } 
        
        rewrite.replace(oldType, newType, null);
        
        ASTUtil.addImports(rewrite, workingUnit, newDotSeperatedClass);
    }
    
    @SuppressWarnings("unchecked")
    private void transferTypeArguments(ParameterizedType existingType, ParameterizedType newType) {
        List<Type> oldTypeArgs = existingType.typeArguments();

        while (!oldTypeArgs.isEmpty()) {
            // This is the only way I could find to copy the Types. rewrite.createCopyTarget didn't help
            // because the types seemed to be in a limbo between attached and not attached.
            // If I try to copy w/o deleting them from the original list, some sort of infinite loop happens
            // on clone
            Type oldType = oldTypeArgs.get(0);
            oldType.delete();
            // oldType is okay to add now w/o a clone, because it is detached.
            newType.typeArguments().add(oldType);
        }
    }

    // Convenience method to copy nodes
    @SuppressWarnings("unchecked")
    private <T extends ASTNode> T copy(T original) {
        return (T) ASTNode.copySubtree(ast, original);
    }

    private void parseParamAndNewClass(List<? extends BugAnnotation> annotations) {
        BugAnnotation annotationWithFix = annotations.get(3);
        
        //this should be like Bug: 1st parameter 's' could be declared as java.util.Set instead
        String toParse = annotationWithFix.toString();
        Matcher matcher = annotationParser.matcher(toParse);
        if (matcher.matches()) {
            paramName = matcher.group(1);
            newDotSeperatedClass = matcher.group(2); 
        }
        
        
    }

}
