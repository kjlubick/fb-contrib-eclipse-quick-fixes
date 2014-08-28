package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class SQLOffByOneResolution extends BugResolution {

    private boolean fixAll;
    
    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        
        SQLVisitor visitor = new SQLVisitor(rewrite);
        node.accept(visitor);

    }
    
    private static class SQLVisitor extends ASTVisitor {
        
        public List<MethodInvocation> badMethodInvocations = new ArrayList<>();
        public List<MethodInvocation> fixedMethodInvocations = new ArrayList<>();
        private ASTRewrite rewrite;
        private AST rootAST;
        
        
        private static final Set<String> types = new HashSet<>();
        static{
            types.add("java.sql.PreparedStatement");
            types.add("java.sql.ResultSet");
        }
        
        
        public SQLVisitor(ASTRewrite rewrite) {
            this.rewrite = rewrite;
            this.rootAST = rewrite.getAST();
        }


        @Override
        public boolean visit(MethodInvocation node) {
            
            if (types.contains(node.getExpression().resolveTypeBinding().getQualifiedName())) {
                badMethodInvocations.add(node);
                fixedMethodInvocations.add(makeFixedMethodInvocation(node));
            }
            
            return true;
        }


        private MethodInvocation makeFixedMethodInvocation(MethodInvocation node) {
            MethodInvocation fixedMethodInvocation = rootAST.newMethodInvocation();
            fixedMethodInvocation.setExpression((Expression) rewrite.createMoveTarget(node.getExpression()));
            fixedMethodInvocation.setName((SimpleName) rewrite.createMoveTarget(node.getName()));
            
            
            
            return fixedMethodInvocation;
        }
    }
    
    
    public void query(PreparedStatement ps) throws SQLException {
        ps.setString(0, "foo");
        ps.setString(1, "bar");
        ps.setInt(2, 42);
    }
    
    
    public void getquery(ResultSet rs) throws SQLException {
        System.out.println(rs.getInt(0));
        System.out.println(rs.getString(1));
        System.out.println(rs.getInt(2));
    }

}
