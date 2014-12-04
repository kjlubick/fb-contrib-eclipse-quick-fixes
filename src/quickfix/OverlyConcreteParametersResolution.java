package quickfix;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
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
    private AST ast;
    private Type badParamType;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        MethodDeclaration method = TraversalUtil.findEnclosingMethod(workingUnit, bug.getPrimarySourceLineAnnotation());
        ast = rewrite.getAST();
        
        parseParamAndNewClass(bug.getAnnotations());
        findBadParameter(method);

        String unqualifiedClass = newDotSeperatedClass.substring(newDotSeperatedClass.lastIndexOf('.')+1);   
        Type fixedType = makeFixedType(unqualifiedClass); 
        
        rewrite.replace(badParamType, fixedType, null);     
        ASTUtil.addImports(rewrite, workingUnit, newDotSeperatedClass);
    }

    private Type makeFixedType(String unqualifiedClass) {
        Type newBaseType = ast.newSimpleType(ast.newName(unqualifiedClass));
        
        if (badParamType.isParameterizedType()) {
            ParameterizedType newPType = ast.newParameterizedType(newBaseType);
            transferTypeArguments((ParameterizedType) badParamType, newPType);
            return newPType;
        }
        return newBaseType;
    }

    @SuppressWarnings("unchecked")
    private void findBadParameter(MethodDeclaration method) {
        List<SingleVariableDeclaration> params = method.parameters();
        for(SingleVariableDeclaration param : params) {
            if (param.getName().getIdentifier().equals(paramName)) {
                badParamType = param.getType();
                break;
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void transferTypeArguments(ParameterizedType existingType, ParameterizedType newType) {
        List<Type> oldTypeArgs = existingType.typeArguments();

        while (!oldTypeArgs.isEmpty()) {
            // transfer the type from one to the other.
            Type oldType = oldTypeArgs.get(0);
            oldType.delete();
            // oldType is okay to add now w/o a clone, because it is detached.
            newType.typeArguments().add(oldType);
        }
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
