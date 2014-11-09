package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ba.heap.FieldSet;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class DeadShadowStoreResolution extends BugResolution {

    public static final String DSS_DESC = "Turns the assignment to a local shadow variable into a field assignment "
            + "by adding a this. to the variable";
    private boolean searchParentClass;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    public void setOptions(Map<String, String> options) {
        searchParentClass = Boolean.parseBoolean(options.get("searchParentClass"));
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = TraversalUtil.backtrackToBlock(node);

        DeadStoreVisitor visitor = new DeadStoreVisitor();
        node.accept(visitor);

        SimpleName leftSide = visitor.badLeftSideName;

        if (leftSide != null) {
            FieldAccess newField = makeFieldAccess(rewrite, visitor);

            rewrite.replace(leftSide, newField, null);
        } else {
            System.err.println("Could not find a local assignment to replace.");
        }
    }

    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        if (searchParentClass) {
            return new DeadStoreVisitor();
        }
        // don't need applicability if we don't search the enclosing classes
        return null;
    }

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        if (searchParentClass) {
            return new DeadStoreVisitor();
        }
        // don't need a custom label if we don't search the enclosing classes
        return null;
    }

    private FieldAccess makeFieldAccess(ASTRewrite rewrite, DeadStoreVisitor visitor) {
        AST ast = rewrite.getAST();

        FieldAccess newField = ast.newFieldAccess();
        ThisExpression thisExpression = ast.newThisExpression();
        if (searchParentClass) {
            thisExpression.setQualifier(ast.newSimpleName(visitor.simpleNameOfEnclosingType));
        }
        newField.setExpression(thisExpression);
        newField.setName((SimpleName) rewrite.createMoveTarget(visitor.badLeftSideName));
        return newField;
    }

    @Override
    public String getDescription() {
        return DSS_DESC;
    }

    private class DeadStoreVisitor extends ASTVisitor implements ApplicabilityVisitor, CustomLabelVisitor {

        public SimpleName badLeftSideName;

        public String simpleNameOfEnclosingType;

        @Override
        public boolean visit(Assignment node) {
            if (badLeftSideName != null) {
                return false;
            }

            Expression left = node.getLeftHandSide();
            if (left instanceof SimpleName) {
                badLeftSideName = (SimpleName) left;

                checkParentScope(node);
                return false;
            }
            return true;
        }

        private void checkParentScope(ASTNode node) {
            if (!searchParentClass || node == null) {
                return;
            }

            if (node instanceof TypeDeclaration) {
                if (searchForField((TypeDeclaration) node)) {
                    simpleNameOfEnclosingType = ((TypeDeclaration) node).resolveBinding().getName();
                    return;
                }
            }

            checkParentScope(node.getParent());
        }

        private boolean searchForField(TypeDeclaration node) {
            FieldDeclaration[] fields = node.getFields();
            // enumerate fields
            for (FieldDeclaration field : fields) {
                @SuppressWarnings("unchecked")
                // each fieldDeclaration has 1 or more fragments that declare a variable.
                List<VariableDeclarationFragment> fragments = field.fragments();
                for (VariableDeclarationFragment declaration : fragments) {
                    // the normal .equals() only does reference equality, so we need to check
                    // the name of the variables.
                    if (badLeftSideName.getIdentifier().equals(declaration.getName().getIdentifier())) {
                        simpleNameOfEnclosingType = node.getName().getIdentifier();
                    }
                }
            }
            return false;
        }

        @Override
        public boolean isApplicable() {
            return !searchParentClass || simpleNameOfEnclosingType != null;
        }

        @Override
        public String getLabelReplacement() {
            if (searchParentClass && simpleNameOfEnclosingType != null) {
                return simpleNameOfEnclosingType + ".this." + badLeftSideName.getIdentifier();
            }
            return "";
        }
    }
}
