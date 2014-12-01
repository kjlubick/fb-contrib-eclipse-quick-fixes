package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
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
    
    @SuppressWarnings("unchecked")
    private Expression makeEnumConstructor(EnumCollectionsVisitor visitor, ASTRewrite rewrite) {
        AST ast = rewrite.getAST();
        TypeLiteral enumType = ast.newTypeLiteral();
        Name enumName = ast.newName(visitor.enumNameToUse);
        enumType.setType(ast.newSimpleType(enumName));
        
        if (visitor.isMap) {
            ClassInstanceCreation newEnumMap = ast.newClassInstanceCreation();
            
            Type enumMap = ast.newSimpleType(ast.newName("EnumMap"));
            
            //makes the <> braces by default
            newEnumMap.setType(ast.newParameterizedType(enumMap));
            newEnumMap.arguments().add(enumType);
            
            return newEnumMap;
        } 
        
        MethodInvocation newEnumSet = ast.newMethodInvocation();
        newEnumSet.setExpression(ast.newSimpleName("EnumSet"));
        newEnumSet.setName(ast.newSimpleName("noneOf"));
        newEnumSet.arguments().add(enumType);
  
        return newEnumSet;
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
                badCollectionName = findName(methodReciever);
                isMap = isEnumBasedMap(methodReciever);
                enumNameToUse = getEnumFromBindingOrNestedBinding(node.resolveTypeBinding());
                badConstructorUsage = findConstructor(badCollectionName, node);
            }
            
            return true;
        }

        private ClassInstanceCreation findConstructor(SimpleName collectionName, MethodInvocation node) {
            IBinding collectionBinding = collectionName.resolveBinding();
            if (collectionBinding instanceof IVariableBinding) {
                if (((IVariableBinding) collectionBinding).isField()) {
                    return findFieldInitialization(collectionName, TraversalUtil.findClosestAncestor(node, TypeDeclaration.class));
                } 
                return findMethodInitialization(collectionName, TraversalUtil.findClosestAncestor(node, MethodDeclaration.class));
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        private ClassInstanceCreation findMethodInitialization(SimpleName collectionName, MethodDeclaration methodDeclaration) {
            List<Statement> statements = methodDeclaration.getBody().statements();
            for(Statement statement: statements) {
                if (statement instanceof VariableDeclarationStatement) {
                    List<VariableDeclarationFragment> fragments = ((VariableDeclarationStatement) statement).fragments();
                    for(VariableDeclarationFragment fragment: fragments) {
                        if (collectionName.getIdentifier().equals(fragment.getName().getIdentifier())) {
                            Expression initializer = fragment.getInitializer();
                            if (initializer instanceof ClassInstanceCreation) {// I can't think of a common way for this to
                                return (ClassInstanceCreation) initializer;     //to be non-null yet not be a CIC
                            } else {
                                return null;
                            }
                        }
                    }
                }
            }
            return null;
        }

        private ClassInstanceCreation findFieldInitialization(SimpleName collectionName, TypeDeclaration typeDeclaration) {
            
            for(FieldDeclaration field:typeDeclaration.getFields()) {
                @SuppressWarnings("unchecked")
                List<VariableDeclarationFragment> fragments = field.fragments();
                for(VariableDeclarationFragment fragment: fragments) {
                    if (collectionName.getIdentifier().equals(fragment.getName().getIdentifier())) {
                        Expression initializer = fragment.getInitializer();
                        if (initializer instanceof ClassInstanceCreation) {// I can't think of a common way for this to
                            return (ClassInstanceCreation) initializer;     //to be non-null yet not be a CIC
                        } else {
                            return lookInDefaultConstructor(collectionName, typeDeclaration);
                        }
                    }
                }
            }
            return null;
        }
        @SuppressWarnings("unchecked")
        private ClassInstanceCreation lookInDefaultConstructor(SimpleName collectionName, TypeDeclaration typeDeclaration) {
            List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
            for(BodyDeclaration declaration:bodyDeclarations) {
                if (declaration instanceof MethodDeclaration) {
                    MethodDeclaration methodDeclaration = ((MethodDeclaration) declaration);
                    if (methodDeclaration.isConstructor() && methodDeclaration.parameters().size() == 0) {
                        return findMethodInitialization(collectionName, methodDeclaration);
                    }
                }
            }
            return null;
        }

        // Looks through either this binding or any nested arguments and returns the first enum found, if any
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
            ITypeBinding binding = expression.resolveTypeBinding();
            return binding.getQualifiedName().matches("java\\.util.*Set") && null != getEnumFromBindingOrNestedBinding(binding);
        }

        private boolean isEnumBasedMap(Expression expression) {
            ITypeBinding binding = expression.resolveTypeBinding();
            return binding.getQualifiedName().matches("java\\.util.*Map") && null != getEnumFromBindingOrNestedBinding(binding);
        }

        private SimpleName findName(Expression methodReciever) {
            if (methodReciever instanceof SimpleName) {
                return (SimpleName) methodReciever;
            }
            if (methodReciever instanceof QualifiedName) {
                return ((QualifiedName) methodReciever).getName();
            }
            if (methodReciever instanceof FieldAccess) {
                return ((FieldAccess) methodReciever).getName();
            }
            // XXX Probably a problem
            return null;
        }

        @Override
        public String getLabelReplacement() {
            return badCollectionName.getIdentifier() + " to be an " + (isMap ? "EnumMap" : "EnumSet");
        }
        
    }

}
