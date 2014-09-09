package quickfix;

import java.util.Map;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class InsecureRandomResolution extends BugResolution {

    private boolean useSecureRandomObject;

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        this.useSecureRandomObject = Boolean.parseBoolean(options.get("useSecureRandomObject"));
    }
    
    @Override
    public String getDescription() {
        if (useSecureRandomObject) {
            return "java.security.SecureRandom can be a drop-in replacement for Random, "
                    + "however, calls to the object's methods (e.g. nextInt(), nextBytes()) "
                    + "may be significantly slower.";
        }
        return "Initializing the seed like <br/><code>new Random(Random(SecureRandom.getInstance().generateSeed())</code><br/>"
                + "creates a more secure starting point for the random number generation than the default.<br/><br/>"
                + "However, still using <code>java.lang.Random</code> makes this slightly less secure than using "
                + "<code>java.secure.SecureRandom</code>, but at the benefit of being faster.  ";
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        // TODO Auto-generated method stub

    }

}
