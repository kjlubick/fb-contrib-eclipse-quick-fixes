package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static util.TraversalUtil.backtrackToBlock;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.omg.CORBA.FieldNameHelper;

public class ConvertingStringLiteralsResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new StringLiteralVisitor(); 
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = backtrackToBlock(node);
        StringLiteralVisitor visitor = new StringLiteralVisitor();
        node.accept(visitor);
        
        StringLiteral newLiteral = rewrite.getAST().newStringLiteral();
        newLiteral.setLiteralValue(visitor.fixedStringLiteral);
        rewrite.replace(visitor.badMethodInvocation, newLiteral, null);
    }
    
    private static Set<String> dummyMethods = new HashSet<String>(3);
    
    static {
        dummyMethods.add("trim");
        dummyMethods.add("toLowerCase");
        dummyMethods.add("toUpperCase");
    }
    
    private class StringLiteralVisitor extends ASTVisitor implements ApplicabilityVisitor{
        
        private MethodInvocation badMethodInvocation;
        private String fixedStringLiteral;
        private boolean isApplicable = false;

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(MethodInvocation node) {
            
            try {
                if (isRedundantMethod(node)) {
                    if (isInvokedOnStringLiteral(node.getExpression())) {
                        isApplicable = true;
                        this.badMethodInvocation = node;
                        this.fixedStringLiteral = fixStringLiteral(node);
                        List<Expression> arguments = node.arguments();
                        if (arguments.size() > 0) {
                            isApplicable = parseLocale(arguments.get(0)) != null;
                        }
                        return false;
                    }
                }
            } catch (RuntimeException e) {
                return false;
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private String fixStringLiteral(MethodInvocation node) {
            Expression expr = node.getExpression();
            
            String fixedString = (String) expr.resolveConstantExpressionValue();
            if (fixedString == null && expr instanceof MethodInvocation) {
                fixedString = fixStringLiteral((MethodInvocation) expr);
            }
            
            if (fixedString == null) {
                throw new RuntimeException("Could not find literal in "+node);
            }
            
            List<Expression> arguments = node.arguments();
            switch (node.getName().getIdentifier()) {
            case "trim":
                fixedString = fixedString.trim();
                break;
            case "toLowerCase":
                if (arguments.isEmpty()) {
                    fixedString = fixedString.toLowerCase();
                } else {
                    Locale parsedLocale = parseLocale(arguments.get(0));
                    if (parsedLocale != null) {
                        fixedString = fixedString.toLowerCase(parsedLocale);
                    }
                }
                break;
            case "toUpperCase":
                if (arguments.isEmpty()) {
                    fixedString = fixedString.toUpperCase();
                } else {
                    Locale parsedLocale = parseLocale(arguments.get(0));
                    if (parsedLocale != null) {
                        fixedString = fixedString.toUpperCase(parsedLocale);
                    }
                }
                break;
            default:
                break;
            }

            return fixedString;
        }

        private Locale parseLocale(Expression expression) {
            if (expression instanceof Name) {
                IBinding varBinding = ((Name) expression).resolveBinding();
                String fieldName = ((IVariableBinding) varBinding).getName();

                Class<Locale> localeClass = Locale.class;
                try {
                    Field f = localeClass.getField(fieldName);
                    return (Locale) f.get(null);        //gets the actual locale, if it exists
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }

        private boolean isInvokedOnStringLiteral(Expression expression) {
            if (!"String".equals(expression.resolveTypeBinding().getName())) {
                return false;
            }
            if (expression.resolveConstantExpressionValue() != null) {
                return true;
            }
            if (expression instanceof MethodInvocation) {
                MethodInvocation mi = (MethodInvocation) expression;
                return isRedundantMethod(mi) && isInvokedOnStringLiteral(mi.getExpression());
            }
            return false;
        }

        private boolean isRedundantMethod(MethodInvocation node) {
            if (!dummyMethods.contains(node.getName().getIdentifier())) {
                return false;
            }
            List<Expression> arguments = node.arguments();
            if (arguments.isEmpty()) {
                return true;
            }
            return !(arguments.get(0) instanceof MethodInvocation);
        }

        @Override
        public boolean isApplicable() {
            return isApplicable;
        }

    }

}
