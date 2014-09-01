package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.CustomLabelBugResolution;
import util.CustomLabelVisitor;

public class ReturnValueResolution extends CustomLabelBugResolution {
    
    private final static String labelForBoolean = "Replace with if (YYY) {}";
    private final static String labelForBooleanNot = "Replace with if (!YYY) {}";
    private boolean isNegated;

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        isNegated = Boolean.parseBoolean(options.get("isNegated"));
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        
        ReturnValueResolutionVisitor rvrFinder = new ReturnValueResolutionVisitor(isNegated);
        node.accept(rvrFinder);
        
        Expression fixedExpression = makeFixedExpression(rewrite, rvrFinder);
        
    }
    
    private Expression makeFixedExpression(ASTRewrite rewrite, ReturnValueResolutionVisitor rvrFinder) {
        
//        AST rootNode = rewrite.getAST();
//        
//        if ()
        
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        return new ReturnValueResolutionVisitor(isNegated);
    }

    
    private static class ReturnValueResolutionVisitor extends CustomLabelVisitor{
        private static Set<String> supportedMethods = new HashSet<>(13);
        static {
            //from CheckReturnAnnotationDatabase, from the FindBugs project
            supportedMethods.add("createNewFile");
            supportedMethods.add("delete");
            supportedMethods.add("mkdir");
            supportedMethods.add("mkdirs");
            supportedMethods.add("renameTo");
            supportedMethods.add("setLastModified");
            supportedMethods.add("setReadOnly");
            supportedMethods.add("setWritable");
            supportedMethods.add("await");
            supportedMethods.add("awaitUntil");
            supportedMethods.add("awaitNanos");
            supportedMethods.add("offer");
            supportedMethods.add("submit");
        }
        
        public String returnType;
        public MethodInvocation badMethodInvocation;
        private String labelReplacement;
        private boolean isNegated;
        
        public ReturnValueResolutionVisitor(boolean isNegated) {
            this.isNegated = isNegated;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (returnType != null) {
                return false;   //no need to search further, we've already found one
            }
            if (!supportedMethods.contains(node.getName().getFullyQualifiedName())) {
                return true;    //keep searching
            }
            
            returnType = node.resolveTypeBinding().getName();
            //string of method invocation for label
            labelReplacement = node.toString();
            badMethodInvocation = node;
            return false;
        }

        @Override
        public String getLabelReplacement() {
            if ("boolean".equals(returnType)) {
                if (isNegated) {
                    return labelForBooleanNot.replace("YYY", labelReplacement);
                }
                return labelForBoolean.replace("YYY", labelReplacement);
            } else {
                System.out.println("I don't know how to handle "+returnType);
                return "Sorry, no quickfix yet";
            }
        }
        
    }
    
    
    
    private ExecutorService thing = Executors.newSingleThreadExecutor();
    
    public void main(String[] args) throws IOException {
        File f = new File("test.txt");
        f.createNewFile();
        
        args[0].trim();
        
        args[1].getBytes(StandardCharsets.UTF_8);
        
        args[2].substring(3);
        
        Callable<String> call = new Callable<String>() {

            @Override
            public String call() throws Exception {
                return "foo";
            }
        };
        
        
        thing.submit(call);
        
        thing.shutdown();
    }

}
