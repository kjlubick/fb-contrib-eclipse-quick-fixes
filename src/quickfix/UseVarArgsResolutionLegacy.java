package quickfix;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.osgi.framework.Version;

import util.TraversalUtil;

/**
 * An older version of UseVarArgsResolution that works on Kepler and (possibly) earlier.
 * 
 * The way arrays and their ASTs are up was changed.  Compare the two removeArrayDimensions methods
 * @author KevinLubick
 *
 */
public class UseVarArgsResolutionLegacy extends BugResolution {

    public static final String DESCRIPTION = "Changing the last parameter to use varargs instead of an array is backwards compatible and makes it easier for clients to call the method";

    @Override
    protected boolean resolveBindings() {
        return false;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
    
    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new KeplerVersionDetector();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        MethodDeclaration thisMethod = TraversalUtil.findEnclosingMethod(workingUnit, bug.getPrimarySourceLineAnnotation());

        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> params = thisMethod.parameters();
        SingleVariableDeclaration lastParam = params.get(params.size() - 1);

        removeArrayDimensions(rewrite, lastParam);
        rewrite.set(lastParam, SingleVariableDeclaration.VARARGS_PROPERTY, Boolean.TRUE, null);

        Type lastType = lastParam.getType();
        if (lastType.isArrayType()) { // can be false for the int a[] declaration
            Type bareType = ((ArrayType) lastType).getElementType();
            rewrite.replace(lastType, rewrite.createCopyTarget(bareType), null);
        }
    }

    @SuppressWarnings({ "deprecation" })
    private void removeArrayDimensions(ASTRewrite rewrite, SingleVariableDeclaration lastParam) {
       // removes any additional dimensions (for variables declared like int a[])
       rewrite.set(lastParam, SingleVariableDeclaration.EXTRA_DIMENSIONS_PROPERTY, 0, null);

    }
    
    private static class KeplerVersionDetector extends ASTVisitor implements ApplicabilityVisitor {

        @Override
        public boolean isApplicable() {
            Version version = Platform.getBundle("org.eclipse.platform").getVersion();
            return version.getMajor() == 4 && version.getMinor() <= 3;
        }
        
    }

}
