package quickfix;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import util.TraversalUtil;

public class EmptyAbstractResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        MethodDeclaration method = TraversalUtil.findEnclosingMethod(workingUnit, bug.getPrimarySourceLineAnnotation());
        ListRewrite modifiers = rewrite.getListRewrite(method, MethodDeclaration.MODIFIERS2_PROPERTY);
        Modifier abstractKeyword = rewrite.getAST().newModifier(Modifier.ModifierKeyword.ABSTRACT_KEYWORD);
        // adds abstract key word
        modifiers.insertLast(abstractKeyword, null);

        // sets "no body" on the method, which ends up being a ; on the end.
        rewrite.set(method, MethodDeclaration.BODY_PROPERTY, null, null);
    }

}
