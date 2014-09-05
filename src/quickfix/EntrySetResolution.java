package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import util.TraversalUtil;

public class EntrySetResolution extends BugResolution {

    private ImportRewrite typeSource;

    private ASTRewrite rewrite;

    private AST ast;

    private Type keyType;

    private Type valueType;

    private SimpleName entryName;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    private Type getTypeFromTypeBinding(ITypeBinding typeBinding, AST ast) {
        return typeSource.addImport(typeBinding, ast);
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        this.typeSource = ImportRewrite.create(workingUnit, true); // these imports won't get added automatically
        this.rewrite = rewrite;
        this.ast = rewrite.getAST();
        EntrySetResolutionVisitor visitor = new EntrySetResolutionVisitor();
        node.accept(visitor);

        EnhancedForStatement replacement = makeReplacementForLoop(visitor);

        rewrite.replace(visitor.ancestorForLoop, replacement, null);

        addImports(rewrite, workingUnit, typeSource.getAddedImports());
        addImports(rewrite, workingUnit, "java.util.Map.Entry");
    }

    @SuppressWarnings("unchecked")
    private EnhancedForStatement makeReplacementForLoop(EntrySetResolutionVisitor visitor) {
        // this would be map.keySet().
        // We need this to get the type of map and get the variable name of map
        MethodInvocation oldLoopExpression = (MethodInvocation) visitor.ancestorForLoop.getExpression();

        // for(Parameter : Expression)
        EnhancedForStatement replacement = ast.newEnhancedForStatement();
        replacement.setParameter(makeEntrySetParameter(oldLoopExpression));
        replacement.setExpression(makeCallToEntrySet(oldLoopExpression));

        List<Statement> replacementBlockStatements = ((Block)replacement.getBody()).statements();
        
        //create new statement to replace the key object (e.g. the String s that used to be in the for each)
        replacementBlockStatements.add(makeNewKeyStatement(visitor));
        
        //replace the call to map.get() with a call to entry.getValue()
        replacementBlockStatements.add(makeNewValueStatement(visitor));
        
        // TODO transfer the rest of the statements in the old block
//        List<Statement> oldBlockStatements = ((Block)visitor.ancestorForLoop.getBody()).statements();
//        for(Statement statement : oldBlockStatements) {
//            if (statement.(visitor.badCallToMapGet))
//        }
        
        return replacement;
    }
    
    private VariableDeclarationStatement makeNewKeyStatement(EntrySetResolutionVisitor visitor) {
        VariableDeclarationFragment keyFragment = ast.newVariableDeclarationFragment();
        keyFragment.setName(copy(visitor.ancestorForLoop.getParameter().getName()));
        
        MethodInvocation entrySetKey = ast.newMethodInvocation();
        entrySetKey.setExpression(copy(this.entryName));
        entrySetKey.setName(ast.newSimpleName("getKey"));
        
        keyFragment.setInitializer(entrySetKey);
        
        VariableDeclarationStatement newKeyStatement = ast.newVariableDeclarationStatement(keyFragment);
        newKeyStatement.setType(copy(keyType));
        return newKeyStatement;
    }

    private VariableDeclarationStatement makeNewValueStatement(EntrySetResolutionVisitor visitor) {
        VariableDeclarationFragment valueFragment = ast.newVariableDeclarationFragment();
        valueFragment.setName(copy(visitor.badMapGetVariableFragment.getName()));
        
        MethodInvocation entrySetValue = ast.newMethodInvocation();
        entrySetValue.setExpression(copy(this.entryName));
        entrySetValue.setName(ast.newSimpleName("getValue"));
        
        valueFragment.setInitializer(entrySetValue);
        
        VariableDeclarationStatement newValueStatement = ast.newVariableDeclarationStatement(valueFragment);
        newValueStatement.setType(copy(valueType));
        return newValueStatement;
    }

    private SingleVariableDeclaration makeEntrySetParameter(MethodInvocation oldLoopExpression) {
        // this is the type of map, e.g. Map<String, Integer>
        ParameterizedType oldParamType = (ParameterizedType) getTypeFromTypeBinding(oldLoopExpression.getExpression()
                .resolveTypeBinding(), ast);

        // give it a base type of Map.Entry, then transfer the params
        ParameterizedType newParamType = ast.newParameterizedType(ast.newSimpleType(ast.newName("Map.Entry")));
        transferTypeArguments(oldParamType, newParamType);

        SingleVariableDeclaration loopParameter = ast.newSingleVariableDeclaration();
        loopParameter.setType(newParamType);
        this.entryName = ast.newSimpleName("entry");
        loopParameter.setName(entryName);
        return loopParameter;
    }

    private MethodInvocation makeCallToEntrySet(MethodInvocation expressionToCopyVariableFrom) {
        MethodInvocation initialization = ast.newMethodInvocation();
        // Expression.Name() We want to copy the expression and make a new name
        initialization.setExpression((Expression) rewrite.createCopyTarget(expressionToCopyVariableFrom.getExpression()));
        initialization.setName(ast.newSimpleName("entrySet"));
        return initialization;
    }

    @SuppressWarnings("unchecked")
    private void transferTypeArguments(ParameterizedType existingType, ParameterizedType newType) {
        List<Type> oldTypeArgs = existingType.typeArguments();

        int i = 0;
        while (!oldTypeArgs.isEmpty()) {
            // This is the only way I could find to copy the Types. rewrite.createCopyTarget didn't help
            // because the types seemed to be in a limbo between attached and not attached.
            // If I try to copy w/o deleting them from the original list, some sort of infite loop happens
            // on clone
            Type oldType = oldTypeArgs.get(0);
            oldType.delete();
            if (i == 0) {
                this.keyType = copy(oldType);
            } else if (i == 1) {
                this.valueType = copy(oldType);
            }
            //this version is okay to add w/o a clone, because it is detached.
            newType.typeArguments().add(oldType);
            i++;
        }
    }

    //Convenience method to copy nodes
    @SuppressWarnings("unchecked")
    private <T extends ASTNode> T copy(T original) {
        return (T) ASTNode.copySubtree(ast, original);
    }

    private static class EntrySetResolutionVisitor extends ASTVisitor {

        public EnhancedForStatement ancestorForLoop;
        public VariableDeclarationFragment badMapGetVariableFragment;

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            this.ancestorForLoop = TraversalUtil.findClosestAncestor(node, EnhancedForStatement.class);
            this.badMapGetVariableFragment = (VariableDeclarationFragment) node.fragments().get(0);
            return false;
        }
    }

    private void test() {
        Map<String, Integer> map = Collections.emptyMap();

        for (String s : map.keySet()) {
            Integer i = map.get(s);
            System.out.println(s + ": " + i);
        }

        // fixed
        for (Entry<String, Integer> entry : map.entrySet()) {
            String s = entry.getKey();
            Integer i = entry.getValue();
            System.out.println(s + ": " + i);
        }
        // alt fix
        Iterator<Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, Integer> entry = it.next();
            String s = entry.getKey();
            Integer i = entry.getValue();
            System.out.println(s + ": " + i);
        }
    }

}
