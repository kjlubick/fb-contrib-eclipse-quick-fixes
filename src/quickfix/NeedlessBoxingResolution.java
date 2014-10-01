package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import javax.xml.namespace.QName;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelBugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class NeedlessBoxingResolution extends CustomLabelBugResolution {

    @Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        return new NeedlessBoxingVisitor();
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
        
        MethodInvocation fixedMethodInvocation = makeFixed(rewrite, visitor);
        
        rewrite.replace(visitor.badMethodInvocation, fixedMethodInvocation, null);
    }

    
    
    @SuppressWarnings("unchecked")
    private MethodInvocation makeFixed(ASTRewrite rewrite, NeedlessBoxingVisitor visitor) {
        AST ast = rewrite.getAST();
        MethodInvocation retVal = ast.newMethodInvocation();
        MethodInvocation original = visitor.badMethodInvocation;
        retVal.setExpression((Expression) rewrite.createMoveTarget(original.getExpression()));
        
        retVal.setName(ast.newSimpleName(visitor.makeParseMethod()));
        
        for(Object arg:original.arguments()) {
            retVal.arguments().add(rewrite.createMoveTarget((ASTNode) arg));
        }
        
        return retVal;
    }



    private static class NeedlessBoxingVisitor extends CustomLabelVisitor {
        
        public MethodInvocation badMethodInvocation;
        
        
        public NeedlessBoxingVisitor() {
            // TODO Auto-generated constructor stub
        }
        
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
        public String getLabelReplacement() {
            if (badMethodInvocation == null) {
                return "the parse equivalent";
            }
            return badMethodInvocation.resolveTypeBinding().getName() + '.' +
            makeParseMethod() + argsToString(badMethodInvocation.arguments());
        }

        private String argsToString(List<?> arguments) {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for(Object arg:arguments) {
                if (arguments.size() > 1) {
                    sb.append(", ");
                }
                sb.append(arg);
            }
            return sb.append(')').toString();
        }
        
        
    }
    
    public static void main(String[] args) {
        boolean val = Boolean.valueOf(args[0]);
        
        if (val && check(args[1])) {
            int i = Integer.valueOf(args[2], 8);
            System.out.println(i);
            i = Integer.valueOf(args[2]);
            System.out.println(i);
            i = Integer.parseInt(args[2], 8);
            System.out.println(i);
            
            double d = Double.valueOf(args[3]);
            System.out.println(d);
        }
    }

    private static boolean check(String string) {
        return Boolean.valueOf(string);
    }
}
