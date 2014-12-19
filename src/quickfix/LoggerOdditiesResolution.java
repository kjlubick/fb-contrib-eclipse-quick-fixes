package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.QMethod;
import util.TraversalUtil;

public class LoggerOdditiesResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        LOVisitor visitor = new LOVisitor();
        node.accept(visitor);
        
        TypeLiteral fixedTypeLiteral = makeTypeLiteral(rewrite, node);
        rewrite.replace(visitor.badArgument, fixedTypeLiteral, null);
    }

    private TypeLiteral makeTypeLiteral(ASTRewrite rewrite, ASTNode node) {
        TypeDeclaration tDeclaration = TraversalUtil.findClosestAncestor(node, TypeDeclaration.class);
        SimpleName name = tDeclaration.getName();
        AST ast = rewrite.getAST();
        SimpleType parentType = ast.newSimpleType(ast.newSimpleName(name.getIdentifier()));
        TypeLiteral fixedTypeLiteral = ast.newTypeLiteral();
        fixedTypeLiteral.setType(parentType);
        return fixedTypeLiteral;
    }
    
    
    private static class LOVisitor extends ASTVisitor
    {

        public Expression badArgument;
        
        
        @Override
        public boolean visit(MethodInvocation node) {
            if (badArgument != null) {
                return false;
            }
            
            QMethod qm = QMethod.make(node);
            
            if ("getLogger".equals(qm.invokedMethodString) && "org.apache.log4j.Logger".equals(qm.qualifiedTypeString)) {
                if (node.arguments().size() > 0) {
                    badArgument = (Expression) node.arguments().get(0);
                    return false;
                }
            }
            return true;
        }
    }
}
