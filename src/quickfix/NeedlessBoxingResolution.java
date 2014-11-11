package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class NeedlessBoxingResolution extends BugResolution {

    private boolean useBooleanConstants;

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
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
            if (visitor.badBooleanLiteral!= null) {
                rewrite.replace(visitor.badBooleanLiteral, fixedBooleanConstant, null);
            } else {
                rewrite.replace(visitor.badBooleanObjectLiteral, fixedBooleanConstant, null);    
            }
        } else {
            MethodInvocation fixedMethodInvocation = makeFixedMethodInvocation(rewrite, visitor);
            rewrite.replace(visitor.methodInvocationToReplace, fixedMethodInvocation, null);
        }
    }

    private Expression makeFixedBooleanConstant(AST ast, NeedlessBoxingVisitor visitor) {
        if (visitor.badBooleanLiteral != null) {
            // turn a BooleanLiteral into a qualified name
            return ast.newName("Boolean." + visitor.makeTrueOrFalse());
        }
        return ast.newBooleanLiteral(Boolean.parseBoolean(visitor.makeTrueOrFalse()));
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

    private class NeedlessBoxingVisitor extends ASTVisitor implements CustomLabelVisitor {

        public MethodInvocation badMethodInvocation;
        
        public MethodInvocation methodInvocationToReplace;      //may be the same as badMethodInvocation.  May be parent if there is a call to booleanValue() or similar

        public BooleanLiteral badBooleanLiteral;

        public QualifiedName badBooleanObjectLiteral;

        

        public String makeParseMethod() {
            if (badMethodInvocation == null)
                return "parseXXX";
            String typeName = badMethodInvocation.resolveTypeBinding().getName();
            if ("Boolean".equals(typeName)) {
                return "parseBoolean";
            } else if ("Byte".equals(typeName)) {
                return "parseByte";
            } else if ("Short".equals(typeName)) {
                return "parseShort";
            } else if ("Long".equals(typeName)) {
                return "parseLong";
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
            if (badBooleanLiteral != null) {
                return badBooleanLiteral.booleanValue() ? "TRUE" : "FALSE";
            }
            //This will be Boolean.TRUE or Boolean.FALSE
            return badBooleanObjectLiteral.getName().getIdentifier().toLowerCase();
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (badMethodInvocation != null)
                return false;

            if ("valueOf".equals(node.getName().getIdentifier()) &&
                    node.getExpression().resolveTypeBinding().getQualifiedName().startsWith("java.lang")) {
                badMethodInvocation = node;
                methodInvocationToReplace = node;
                ASTNode parent = badMethodInvocation.getParent();
                if (parent instanceof MethodInvocation && ((MethodInvocation) parent).getName().getIdentifier().endsWith("Value")) {
                    methodInvocationToReplace = (MethodInvocation) parent;
                }
                
                return false;
            }

            return true;
        }

        @Override
        public boolean visit(BooleanLiteral node) {
            if (this.badBooleanLiteral != null) {
                return false;
            }
            if (node.resolveBoxing()) { // did boxing happen? If so, that's our cue
                badBooleanLiteral = node;
                return false;
            }

            return true;
        }
        
        @Override
        public boolean visit(QualifiedName node) {
            if (this.badBooleanObjectLiteral != null) {
                return false;
            }
            if (node.resolveUnboxing()) { // did unboxing happen? If so, that's our cue
                badBooleanObjectLiteral = node;
                return false;
            }

            return true;
        }

        @Override
        public String getLabelReplacement() {
            if (useBooleanConstants) {
                
                if (badBooleanLiteral != null) {
                return "Boolean." + makeTrueOrFalse();
                }
                return makeTrueOrFalse();
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
}
