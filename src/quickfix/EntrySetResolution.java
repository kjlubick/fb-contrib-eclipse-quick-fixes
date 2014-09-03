package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.TraversalUtil;

public class EntrySetResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        
        EntrySetResolutionVisitor visitor = new EntrySetResolutionVisitor();
        node.accept(visitor);
        
    }
    
    private static class EntrySetResolutionVisitor extends ASTVisitor {
        @Override
        public boolean visit(EnhancedForStatement node) {
            return true;
        }
        
        @Override
        public boolean visit(VariableDeclarationStatement node) {
            EnhancedForStatement forStatement = TraversalUtil.findClosestAncestor(node, EnhancedForStatement.class);
            System.out.println(forStatement);
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
