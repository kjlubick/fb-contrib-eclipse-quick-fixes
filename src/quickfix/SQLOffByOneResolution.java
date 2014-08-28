package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class SQLOffByOneResolution extends BugResolution {

    private boolean fixAll;
    
    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        fixAll = Boolean.parseBoolean(options.get("replaceAll"));
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        
        if (fixAll) {
            node = node.getParent();
        }
        
        
        SQLVisitor visitor = new SQLVisitor(rewrite);
        node.accept(visitor);

        for(int i = 0; i<visitor.badMethodInvocations.size(); i++) {
            MethodInvocation bad = visitor.badMethodInvocations.get(i);
            MethodInvocation fixed = visitor.fixedMethodInvocations.get(i);
            
            rewrite.replace(bad, fixed, null);
        }
        
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
            
            if (isValidSQLSetUpdateOrGet(node)) {
                badMethodInvocations.add(node);
                fixedMethodInvocations.add(makeFixedMethodInvocation(node));
            }
            
            return true;
        }


        private boolean isValidSQLSetUpdateOrGet(MethodInvocation node) {
            String typeName = node.getExpression().resolveTypeBinding().getQualifiedName();
            if (!types.contains(typeName)) {
                return false;
            }
            //ResultSet and PreparedStatement have lots of methods
            //we don't want to change the wrong ones
            String calledMethod = node.getName().getFullyQualifiedName();
            if (typeName.contains("PreparedStatement")) {
                //e.g. setInteger(int paramIndex, int val)
                return calledMethod.startsWith("set") && node.arguments().size() == 2;
            }

            List<?> args = node.arguments();
            if  (!(calledMethod.startsWith("get") && args.size() == 1) ||
                    (calledMethod.startsWith("update") && args.size() == 2)) {
                return false;
            }
            //there are two versions of resultSet.getString [and updateString]
            //getString(String colName) and getString(int colIndex)
            //we only want to update the latter
            return args.get(0) instanceof NumberLiteral;
        }


        @SuppressWarnings("unchecked")
        private MethodInvocation makeFixedMethodInvocation(MethodInvocation node) {
            MethodInvocation fixedMethodInvocation = rootAST.newMethodInvocation();
            fixedMethodInvocation.setExpression((Expression) rewrite.createCopyTarget(node.getExpression()));
            fixedMethodInvocation.setName((SimpleName) rewrite.createCopyTarget(node.getName()));
            
            List<Expression> oldArguments = node.arguments();
                   
            //we know from isValidSQLSetUpdateOrGet that the first arg is the numberLiteral
            NumberLiteral intArg = (NumberLiteral) oldArguments.get(0);
            
            String incrementedArg = Integer.toString(Integer.parseInt(intArg.getToken()) + 1);
            
            List<Expression> newArguments = fixedMethodInvocation.arguments();
            newArguments.add(rootAST.newNumberLiteral(incrementedArg));
            
            for(int i = 1; i < oldArguments.size(); i++) {
                newArguments.add((Expression) rewrite.createCopyTarget(oldArguments.get(i)));
            }
            
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
