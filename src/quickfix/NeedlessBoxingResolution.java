package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelBugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.apache.bcel.generic.RETURN;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class NeedlessBoxingResolution extends CustomLabelBugResolution {

    private boolean useBooleanConstants;

    @Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        return new NeedlessBoxingVisitor();
    }
    
    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        useBooleanConstants = Boolean.parseBoolean(options.get("useBooleanConstants"));
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        NeedlessBoxingVisitor visitor = new NeedlessBoxingVisitor();
        node.accept(visitor);

        if (useBooleanConstants) {
            Expression fixedBooleanConstant = makeFixedBooleanConstant(rewrite.getAST(), visitor);
            rewrite.replace(visitor.badBooleanLiteral, fixedBooleanConstant, null);
        } else {
            MethodInvocation fixedMethodInvocation = makeFixedMethodInvocation(rewrite, visitor);
            rewrite.replace(visitor.badMethodInvocation, fixedMethodInvocation, null);
        }
    }

    private Expression makeFixedBooleanConstant(AST ast, NeedlessBoxingVisitor visitor) {
        Expression retVal = ast.newQualifiedName(ast.newSimpleName("Boolean"),
                ast.newSimpleName(visitor.makeTrueOrFalse()));
        return retVal;
    }

    @SuppressWarnings("unchecked")
    private MethodInvocation makeFixedMethodInvocation(ASTRewrite rewrite, NeedlessBoxingVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation retVal = ast.newMethodInvocation();
        MethodInvocation original = visitor.badMethodInvocation;
        retVal.setExpression((Expression) rewrite.createMoveTarget(original.getExpression()));

        retVal.setName(ast.newSimpleName(visitor.makeParseMethod()));

        for (Object arg : original.arguments()) {
            retVal.arguments().add(rewrite.createMoveTarget((ASTNode) arg));
        }

        return retVal;
    }

    private class NeedlessBoxingVisitor extends CustomLabelVisitor {

        public MethodInvocation badMethodInvocation;
        
        public BooleanLiteral badBooleanLiteral;

        public String makeParseMethod() {
            if (badMethodInvocation == null)
                return "parseXXX";
            String typeName = badMethodInvocation.resolveTypeBinding().getName();
            if ("Boolean".equals(typeName)) {
                return "parseBoolean";
            } else if ("Integer".equals(typeName)) {
                return "parseInt";
            } else if ("Double".equals(typeName)) {
                return "parseDouble";
            } else if ("Float".equals(typeName)) {
                return "parseFloat";
            }
            return "parseXXX";
        }

        public String makeTrueOrFalse() {
            return badBooleanLiteral.booleanValue()?"TRUE":"FALSE";
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (badMethodInvocation != null)
                return false;

            if ("valueOf".equals(node.getName().getIdentifier()) &&
                    node.getExpression().resolveTypeBinding().getQualifiedName().startsWith("java.lang")) {
                badMethodInvocation = node;
                return false;
            }

            return true;
        }
        
        @Override
        public boolean visit(BooleanLiteral node) {
            if (this.badBooleanLiteral != null) {
                return false;
            }
            if (node.resolveBoxing()) {     //did boxing happen?  If so, that's our cue
                badBooleanLiteral = node;
                return false;
            }
            
            return true;
        }

        @Override
        public String getLabelReplacement() {
            if (useBooleanConstants) {
                return "Boolean."+makeTrueOrFalse();
            }
            if (badMethodInvocation == null) {
                return "the parse equivalent";
            }
            return badMethodInvocation.resolveTypeBinding().getName() + '.' +
                    makeParseMethod() + argsToString(badMethodInvocation.arguments());
        }

        private String argsToString(List<?> arguments) {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (Object arg : arguments) {
                if (arguments.size() > 1) {
                    sb.append(", ");
                }
                sb.append(arg);
            }
            return sb.append(')').toString();
        }

    }
    
    
    public Boolean getVal() {
        Map<String, Boolean> map = new HashMap<String, Boolean>();
        map.put("foo", true);
        System.out.println(map);
        return Boolean.FALSE;
    }
}
