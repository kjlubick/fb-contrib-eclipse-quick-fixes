package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.apache.bcel.classfile.EnclosingMethod;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import util.TraversalUtil;

public class SwitchFallThroughResolution extends BugResolution {

    public static final String RETURN_FIELD = "Adds <code>return YYY;</code> to close off the case statement";
    public static final String BREAK_DESCRIPTION = "Adds <code>break;</code> to close off the case statement";
    
    private boolean shouldUseBreak;
    private String description;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    public void setOptions(Map<String, String> options) {
        this.shouldUseBreak = Boolean.parseBoolean(options.get("useBreak"));
    }
    

    @Override
    @SuppressFBWarnings(value = "BAS_BLOATED_ASSIGNMENT_SCOPE",
            justification = "the call to getLabel() is needed to fill out information for custom descriptions")
    public String getDescription() {
        
        if (shouldUseBreak)
            return BREAK_DESCRIPTION;
        if (description == null) {
            String label = getLabel(); // force traversing, which fills in description
            if (description == null) {
                return label; // something funky is happening, description shouldn't be null
                              // We'll be safe and return label (which is not null)
            }
        }
        return description;
    }
    
    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new FallThroughVisitor();
    }
    
    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new FallThroughVisitor();
    }
    
    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        FallThroughVisitor visitor = new FallThroughVisitor();
        node.accept(visitor);
        
        if (visitor.badSwitchStatement != null && visitor.caseFellThrough != null) {
            SwitchStatement ss = visitor.badSwitchStatement;
            // easiest way to insert a new statement into the old statements is
            // to make a listRewrite from the statements
            ListRewrite switchRewrite = rewrite.getListRewrite(ss, SwitchStatement.STATEMENTS_PROPERTY);
            Statement newStatement = makeFixedStatement(rewrite, visitor);
            switchRewrite.insertBefore(newStatement, visitor.caseFellThrough, null);
        }
    }
    
    
    private Statement makeFixedStatement(ASTRewrite rewrite, FallThroughVisitor visitor) {
        AST ast = rewrite.getAST();
        if (shouldUseBreak) {
            return ast.newBreakStatement();
        }
        ReturnStatement retVal = ast.newReturnStatement();
        // this allows the fallThroughField to be either a FieldAccess (i.e. with this)
        // or a SimpleName
        retVal.setExpression((Expression) rewrite.createCopyTarget(visitor.fallThroughField));
        return retVal;
    }


    private class FallThroughVisitor extends ASTVisitor implements CustomLabelVisitor, ApplicabilityVisitor {

        public SwitchCase caseFellThrough;
        public SwitchStatement badSwitchStatement;
        public Expression fallThroughField;
        
        
        @Override
        public boolean visit(Assignment node) {
            if (badSwitchStatement != null) {
                return false;
            }
            badSwitchStatement = TraversalUtil.findClosestAncestor(node, SwitchStatement.class);
            caseFellThrough = findBadFallThroughCase(TraversalUtil.findClosestAncestor(node, Statement.class));
            if (!shouldUseBreak) {
                findFieldName(node.getLeftHandSide());
            }

            return false;
        }
        
        
        private void findFieldName(Expression leftHandSide) {
            if ((leftHandSide instanceof FieldAccess) ||
                    (leftHandSide instanceof SimpleName && TraversalUtil.nameRefersToField((SimpleName)leftHandSide))){
                
                boolean equals = doesThisTypeMatchMethodReturnType(leftHandSide);
                if (equals) {
                    this.fallThroughField = leftHandSide;
                }
            }
        }


        private boolean doesThisTypeMatchMethodReturnType(Expression expression) {
            MethodDeclaration enclosingMethod = TraversalUtil.findClosestAncestor(expression, MethodDeclaration.class);        
            String returnMethodType = enclosingMethod.getReturnType2().resolveBinding().getQualifiedName();
            String storedObjectType = expression.resolveTypeBinding().getQualifiedName();
            
            // if the type of the object we are storing 
            return storedObjectType.equals(returnMethodType);
        }


        @SuppressWarnings("unchecked")
        private SwitchCase findBadFallThroughCase(Statement lookingForStatement) {
            List<Statement> switchStatements = badSwitchStatement.statements();
            SwitchCase lastSwitchCase = null;
            for (Statement statement : switchStatements) {
                if (statement instanceof SwitchCase) {
                    lastSwitchCase = (SwitchCase) statement;
                }
                if (statement == lookingForStatement) {
                    return lastSwitchCase;
                }
            }
            return null;
        }


        @Override
        public boolean isApplicable() {
            return shouldUseBreak || fallThroughField != null;
        }

        @Override
        public String getLabelReplacement() {
            if (shouldUseBreak) {
                return "";
            }
            String fieldString = fallThroughField == null ? "" : fallThroughField.toString();
            description = RETURN_FIELD.replace("YYY", fieldString);
            return fieldString;
        }
        
    }

}
