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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;

import util.TraversalUtil;

public class EntrySetResolution extends BugResolution {

    private ImportRewrite typeSource;


    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    private Type getTypeFromTypeBinding(ITypeBinding typeBinding, AST ast){
        return typeSource.addImport(typeBinding, ast);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        this.typeSource = ImportRewrite.create(workingUnit, true);      //these imports won't get added automatically
        
        EntrySetResolutionVisitor visitor = new EntrySetResolutionVisitor();
        node.accept(visitor);
        
        AST ast = rewrite.getAST();
        
        //visitor.ancestorForLoop;
        EnhancedForStatement replacement = ast.newEnhancedForStatement();
        
       // Type keyType = visitor.ancestorForLoop.getParameter().getType();
        
        MethodInvocation oldLoopExpression = (MethodInvocation)visitor.ancestorForLoop.getExpression();
        ParameterizedType mapType = (ParameterizedType) getTypeFromTypeBinding(oldLoopExpression.getExpression().resolveTypeBinding(),ast);
        
        ParameterizedType newType = ast.newParameterizedType(ast.newSimpleType(ast.newName("Map.Entry")));
        
        List<Type> oldTypeArgs = mapType.typeArguments();
        
        while(!oldTypeArgs.isEmpty()) {
            Type oldType = oldTypeArgs.get(0);
            oldType.delete();
            newType.typeArguments().add(oldType);
        }
        
        SingleVariableDeclaration loopParameter = ast.newSingleVariableDeclaration();
        loopParameter.setType(newType);
        loopParameter.setName(ast.newSimpleName("entry"));
        
        replacement.setParameter(loopParameter);
        
        MethodInvocation initialization = ast.newMethodInvocation();
        
        
        initialization.setExpression((Expression) rewrite.createCopyTarget(oldLoopExpression.getExpression()));
        initialization.setName(ast.newSimpleName("entrySet"));
        
        replacement.setExpression(initialization);
        
        rewrite.replace(visitor.ancestorForLoop, replacement, null);
        
        
        addImports(rewrite, workingUnit, typeSource.getAddedImports());
        addImports(rewrite, workingUnit, "java.util.Map.Entry");
    }
    
    private static class EntrySetResolutionVisitor extends ASTVisitor {
        
        public EnhancedForStatement ancestorForLoop;

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            this.ancestorForLoop = TraversalUtil.findClosestAncestor(node, EnhancedForStatement.class);


            return true;
        }
    }
    
    
    private void test() {
        Map<String,Integer> map = Collections.emptyMap();
        
        for(String s: map.keySet()) {
            Integer i = map.get(s);
            System.out.println(s+": "+i);
        }
        
        //fixed
        for(Entry<String, Integer> entry: map.entrySet()) {
            String s = entry.getKey();
            Integer i = entry.getValue();
            System.out.println(s+": "+i);
        }
        //alt fix
        Iterator<Entry<String, Integer>> it = map.entrySet().iterator();
        while(it.hasNext()) {
            Entry<String, Integer> entry = it.next();
            String s = entry.getKey();
            Integer i = entry.getValue();
            System.out.println(s+": "+i);
        }
    }

}
