package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class UseEnumCollectionsResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected ASTVisitor getCustomLabelVisitor() {
        return new EnumCollectionsVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        EnumCollectionsVisitor visitor = new EnumCollectionsVisitor();
        node.accept(visitor);

        Expression newEnumConstructor = makeEnumConstructor(visitor, rewrite);

        rewrite.replace(visitor.badConstructorUsage, newEnumConstructor, null);

        if (visitor.isMap) {
            addImports(rewrite, workingUnit, "java.util.EnumMap");
        } else {
            addImports(rewrite, workingUnit, "java.util.EnumSet");
        }

    }

    private Expression makeEnumConstructor(EnumCollectionsVisitor visitor, ASTRewrite rewrite) {
        AST ast = rewrite.getAST();
        TypeLiteral enumDotClass = ast.newTypeLiteral(); // this is the EnumHere.class
        enumDotClass.setType(ast.newSimpleType(ast.newName(visitor.enumNameToUse)));

        if (visitor.isMap) {
            return makeEnumMap(enumDotClass, ast);
        }
        return makeEnumSet(enumDotClass, ast);
    }

    @SuppressWarnings("unchecked")
    private Expression makeEnumSet(TypeLiteral enumType, AST ast) {
        MethodInvocation newEnumSet = ast.newMethodInvocation();
        newEnumSet.setExpression(ast.newSimpleName("EnumSet"));
        newEnumSet.setName(ast.newSimpleName("noneOf"));
        newEnumSet.arguments().add(enumType);

        return newEnumSet;
    }

    @SuppressWarnings("unchecked")
    private Expression makeEnumMap(TypeLiteral enumType, AST ast) {
        ClassInstanceCreation newEnumMap = ast.newClassInstanceCreation();
        Type enumMap = ast.newSimpleType(ast.newName("EnumMap"));
        // makes the <> braces by default
        newEnumMap.setType(ast.newParameterizedType(enumMap));
        newEnumMap.arguments().add(enumType);

        return newEnumMap;
    }

    private static class EnumCollectionsVisitor extends ASTVisitor implements CustomLabelVisitor {

        public String enumNameToUse;

        public ClassInstanceCreation badConstructorUsage;

        private SimpleName badCollectionName;

        public boolean isMap;

        @Override
        public boolean visit(MethodInvocation node) {
            if (badConstructorUsage != null) {
                return false;
            }

            Expression methodReciever = node.getExpression();
            if (isEnumBasedMap(methodReciever) || isEnumBasedSet(methodReciever)) {
                try {
                    badCollectionName = findNameOfCollection(methodReciever);
                    isMap = isEnumBasedMap(methodReciever);
                    enumNameToUse = getEnumFromBindingOrNestedBinding(node.getExpression().resolveTypeBinding());
                    badConstructorUsage = findInitializerOfCollection(badCollectionName, node);
                } catch (EnumParsingException e) {
                    // reset, parsing out the enum usages went poorly
                    badCollectionName = null;
                    enumNameToUse = null;
                    badConstructorUsage = null;
                }
            }

            return true;
        }

        private ClassInstanceCreation findInitializerOfCollection(SimpleName collectionName, MethodInvocation startOfSearch)
                throws EnumParsingException {
            IBinding collectionBinding = collectionName.resolveBinding();
            // if collectionName does not refer to a variable, we won't be able to find
            // where it is being initialized
            if (collectionBinding instanceof IVariableBinding) {
                if (((IVariableBinding) collectionBinding).isField()) {
                    return findFieldInitialization(collectionName,
                            TraversalUtil.findClosestAncestor(startOfSearch, TypeDeclaration.class));
                }
                return findMethodInitialization(collectionName,
                        TraversalUtil.findClosestAncestor(startOfSearch, MethodDeclaration.class), false);
            }
            throw new EnumParsingException();
        }

        @SuppressWarnings("unchecked")
        private ClassInstanceCreation findMethodInitialization(SimpleName collectionName, MethodDeclaration methodDeclaration,
                boolean isField) throws EnumParsingException {
            // Look in this method for an initialization of the variable collectionName
            // This handles initialization of a field (i.e. something w/o a declaration)
            // and a local variable (i.e. something with a declaration)
            List<Statement> statements = methodDeclaration.getBody().statements();
            for (Statement statement : statements) {
                if (!isField && statement instanceof VariableDeclarationStatement) {
                    return findClassInstanceCreationInDeclaration(collectionName, (VariableDeclarationStatement) statement);
                } else if (isField && statement instanceof ExpressionStatement) {
                    return findClassInstanceCreationInAssignment(collectionName, (ExpressionStatement) statement);
                }
            }
            throw new EnumParsingException();
        }

        private ClassInstanceCreation findClassInstanceCreationInAssignment(SimpleName collectionName,
                ExpressionStatement statement) throws EnumParsingException {
            // this ExpressionStatement is expected to be a wrapped assignment
            Expression expression = statement.getExpression();
            if (expression instanceof Assignment) {
                Assignment assignment = (Assignment) expression;
                if (areNamesEqual(collectionName, findNameOfCollection(assignment.getLeftHandSide()))) {
                    if (assignment.getRightHandSide() instanceof ClassInstanceCreation) {
                        return (ClassInstanceCreation) assignment.getRightHandSide();
                    }
                }
            }
            throw new EnumParsingException();
        }

        @SuppressWarnings("unchecked")
        private ClassInstanceCreation findClassInstanceCreationInDeclaration(SimpleName collectionName,
                VariableDeclarationStatement statement) throws EnumParsingException {
            List<VariableDeclarationFragment> fragments = statement.fragments();
            for (VariableDeclarationFragment fragment : fragments) {
                if (areNamesEqual(collectionName, fragment.getName())) {
                    Expression initializer = fragment.getInitializer();
                    if (initializer instanceof ClassInstanceCreation) {
                        return (ClassInstanceCreation) initializer;
                    } else {
                        throw new EnumParsingException();
                    }
                }
            }
            throw new EnumParsingException();
        }

        private static boolean areNamesEqual(SimpleName firstName, SimpleName secondName) {
            // can't do collectionName.equals(fragment.getName()) because the SimpleName
            // equals method is just a pointer equality, so we have to compare the identifiers
            return firstName.getIdentifier().equals(secondName.getIdentifier());
        }

        @SuppressWarnings("unchecked")
        private ClassInstanceCreation findFieldInitialization(SimpleName collectionName, TypeDeclaration typeDeclaration)
                throws EnumParsingException {
            // this will look through all the fields for the declaration. If it wasn't
            // initialized at declaration, we'll try looking in the default constructor.
            for (FieldDeclaration field : typeDeclaration.getFields()) {
                List<VariableDeclarationFragment> fragments = field.fragments();
                for (VariableDeclarationFragment fragment : fragments) {

                    if (areNamesEqual(collectionName, fragment.getName())) {
                        Expression initializer = fragment.getInitializer();
                        // if there are cases other than "is ClassInstanceCreation"
                        // or "null", I can't think of them.
                        if (initializer instanceof ClassInstanceCreation) {
                            return (ClassInstanceCreation) initializer;
                        } else {
                            return lookInDefaultConstructor(collectionName, typeDeclaration);
                        }
                    }
                }
            }
            throw new EnumParsingException();
        }

        @SuppressWarnings("unchecked")
        private ClassInstanceCreation lookInDefaultConstructor(SimpleName collectionName, TypeDeclaration typeDeclaration)
                throws EnumParsingException {
            // Search all bodyDeclarations for a methodDeclaration with no arguments
            // i.e. default constructor
            List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
            for (BodyDeclaration declaration : bodyDeclarations) {
                if (declaration instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = ((MethodDeclaration) declaration);
                    if (methodDeclaration.isConstructor() && methodDeclaration.parameters().isEmpty()) {
                        return findMethodInitialization(collectionName, methodDeclaration, true);
                    }
                }
            }
            throw new EnumParsingException();
        }

        // Looks through either this binding or any nested arguments and returns the first enum found, if any
        // Does not throw an exception, but rather returns null. This is to facilitate checking multiple bindings.
        @Nullable
        private String getEnumFromBindingOrNestedBinding(ITypeBinding binding) {
            if (binding.isEnum()) {
                return binding.getName();
            }
            ITypeBinding[] argumentBindings = binding.getTypeArguments();
            if (argumentBindings == null) {
                return null;
            }
            for (int i = 0; i < argumentBindings.length; i++) {
                String name = getEnumFromBindingOrNestedBinding(argumentBindings[i]);
                if (name != null) {
                    return name;
                }
            }
            return null;
        }

        private boolean isEnumBasedSet(Expression expression) {
            // A bit hackish, but this looks at the name of the type and if there is also an enum in one of the TypeParameters
            ITypeBinding binding = expression.resolveTypeBinding();
            return binding.getQualifiedName().matches("java\\.util.*Set.*") && null != getEnumFromBindingOrNestedBinding(binding);
        }

        private boolean isEnumBasedMap(Expression expression) {
            // A bit hackish, but this looks at the name of the type and if there is also an enum in one of the TypeParameters
            ITypeBinding binding = expression.resolveTypeBinding();
            return binding.getQualifiedName().matches("java\\.util.*Map.*") && null != getEnumFromBindingOrNestedBinding(binding);
        }

        private SimpleName findNameOfCollection(Expression methodReciever) throws EnumParsingException {
            if (methodReciever instanceof SimpleName) {
                return (SimpleName) methodReciever;
            }
            if (methodReciever instanceof QualifiedName) {
                return ((QualifiedName) methodReciever).getName();
            }
            if (methodReciever instanceof FieldAccess) {
                return ((FieldAccess) methodReciever).getName();
            }
            throw new EnumParsingException();
        }

        @Override
        public String getLabelReplacement() {
            return badCollectionName.getIdentifier() + " to be an " + (isMap ? "EnumMap" : "EnumSet");
        }

    }

    private static class EnumParsingException extends Exception {

        private static final long serialVersionUID = -5995607690601671285L;

    }

}
