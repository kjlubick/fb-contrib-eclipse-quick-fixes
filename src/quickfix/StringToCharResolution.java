package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class StringToCharResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());

        StringToCharVisitor visitor = new StringToCharVisitor();
        node.accept(visitor);

        AST ast = rewrite.getAST();

        for (Map.Entry<StringLiteral, Character> entry : visitor.replacements.entrySet()) {
            CharacterLiteral charLiteral = ast.newCharacterLiteral();
            charLiteral.setCharValue(entry.getValue());

            rewrite.replace(entry.getKey(), charLiteral, null);
        }
    }

    private static class StringToCharVisitor extends ASTVisitor {

        Map<StringLiteral, Character> replacements = new HashMap<>();

        @Override
        public boolean visit(StringLiteral node) {
            String literalValue = node.getLiteralValue();
            if (literalValue.length() == 1) {
                replacements.put(node, literalValue.charAt(0));
            }
            return true;
        }

    }

}
