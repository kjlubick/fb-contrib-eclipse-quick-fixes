package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.Map;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

public class InsecureRandomResolution extends BugResolution {

    private static final String QUALIFIED_SECURE_RANDOM = "java.security.SecureRandom";
    private static final String QUALIFIED_RANDOM = "java.util.Random";
    
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
        return "Initializing the seed like <br/><code>new Random(SecureRandom.getInstance().generateSeed())</code><br/>"
                + "creates a more secure starting point for the random number generation than the default.<br/><br/>"
                + "However, still using <code>java.lang.Random</code> makes this slightly less secure than using "
                + "<code>java.secure.SecureRandom</code>, but at the benefit of being faster.  ";
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        RandomVisitor visitor = new RandomVisitor();
        node.accept(visitor);

        AST ast = rewrite.getAST();
        
        //System.out.println(visitor.randomToFix);
        //Make a new Random ClassInstanceCreation or a SecureRandome one, depending on input
        
        SimpleType randomType = ast.newSimpleType(ast.newName("Random"));
        ClassInstanceCreation newRandom = ast.newClassInstanceCreation();
        newRandom.setType(randomType);
        
        SimpleType secureRandomType = ast.newSimpleType(ast.newName("SecureRandom"));
        ClassInstanceCreation newSecureRandom = ast.newClassInstanceCreation();
        newSecureRandom.setType(secureRandomType);
        
        MethodInvocation getLong = ast.newMethodInvocation();
        getLong.setExpression(newSecureRandom);
        getLong.setName(ast.newSimpleName("nextLong"));
        
        newRandom.arguments().add(getLong);
        
        rewrite.replace(visitor.randomToFix, newRandom, null);
        
        addImports(rewrite, workingUnit, QUALIFIED_SECURE_RANDOM);
    }
    
    private static class RandomVisitor extends ASTVisitor {
        
        
        public ClassInstanceCreation randomToFix;

        @Override
        public boolean visit(ClassInstanceCreation node) {
            if (QUALIFIED_RANDOM.equals(node.resolveTypeBinding().getQualifiedName())) {
                this.randomToFix = node;
            }
            
            return true;
        }
    }

}
