package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static util.TraversalUtil.backtrackToBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class ArrayToStringResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        node = backtrackToBlock(node);
        ArrayToStringVisitor atsFinder = new ArrayToStringVisitor();
        node.accept(atsFinder);
        
        System.out.println(atsFinder.arrayExpressionsToResolve);
    }
    
    
    /*   && ("toString".equals(getNameConstantOperand()) && "()Ljava/lang/String;".equals(getSigConstantOperand())
                        || "append".equals(getNameConstantOperand())
                        && "(Ljava/lang/Object;)Ljava/lang/StringBuilder;".equals(getSigConstantOperand())
                        && "java/lang/StringBuilder".equals(getClassConstantOperand())
                        || "append".equals(getNameConstantOperand())
                        && "(Ljava/lang/Object;)Ljava/lang/StringBuffer;".equals(getSigConstantOperand())
                        && "java/lang/StringBuffer".equals(getClassConstantOperand()) || ("print".equals(getNameConstantOperand()) || "println".equals(getNameConstantOperand()))
                                && "(Ljava/lang/Object;)V".equals(getSigConstantOperand()))) {
    */
    
    private static class ArrayToStringVisitor extends ASTVisitor {
        
        public List<Expression> arrayExpressionsToResolve = new ArrayList<>();
                
        private static Set<String> methodsToCheck = new HashSet<>();
        
        static {
            methodsToCheck.add("PrintStream.println(Object)");
        }
        
        
        
        @Override
        public boolean visit(MethodInvocation node) {
            //for stringBuilder.append(array); and System.out.println(one);
            
            IMethodBinding methodBinding = node.resolveMethodBinding();
            methodBinding.get
            // TODO Auto-generated method stub
            return super.visit(node);
        }
        
        @Override
        public boolean visit(InfixExpression node) {
            //for "Hello" + array + ':' + otherArray
            // TODO Auto-generated method stub
            return super.visit(node);
        }
        
        
        
    }
    
//    private static class ResolutionBundle {
//        public Expression arrayExpression;
//
//        public ResolutionBundle(Expression arrayExpression) {
//            this.arrayExpression = arrayExpression;
//        }
//    }
    
    public String test(int[] one, Double two[]) {
        
        System.out.println(one);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Hello");
        sb.append(two);
        
        return sb.toString();
        
    }
    
    public void test2(int[] one, Double two[]) {

        String string = "Hello" + one + ':' + two;

        System.out.println(string);

    }
    
    public void test3(int[] one, Double two[]) {

        System.out.println("Hello" + one + ':' + two);

    }
    
   
    

}
