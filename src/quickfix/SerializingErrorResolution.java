package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import util.TraversalUtil;

public class SerializingErrorResolution extends BugResolution {

    public static final String SE_DESCRIPTION = "The transient keyword prevents the field from being serialized.  You will have to properly initialize it in some other way.";
    
    @Override
    protected boolean resolveBindings() {
        return false;       // no need for bindings, just looking for the field
    }
    
    @Override
    public String getDescription() {
        return SE_DESCRIPTION;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        if (!(node instanceof FieldDeclaration)) {
            node = TraversalUtil.findClosestAncestor(node, FieldDeclaration.class);
        }

        if (node != null) {
            ListRewrite modifiersRewrite = getModifiersRewrite(rewrite, node);
            Modifier newTransient = rewrite.getAST().newModifier(ModifierKeyword.TRANSIENT_KEYWORD);
            
            modifiersRewrite.insertLast(newTransient, null);
            
            
        }
    }

    private ListRewrite getModifiersRewrite(ASTRewrite rewrite, ASTNode node) {
        return rewrite.getListRewrite(node, FieldDeclaration.MODIFIERS2_PROPERTY);
    }

}
