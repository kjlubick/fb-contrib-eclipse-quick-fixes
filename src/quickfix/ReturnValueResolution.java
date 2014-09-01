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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.CustomLabelBugResolution;
import util.CustomLabelVisitor;

public class ReturnValueResolution extends CustomLabelBugResolution {
    
    private final static String labelForBoolean = "Replace with if (YYY) {}";
    private final static String labelForBooleanNot = "Replace with if (!YYY) {}";
    
    
    private final static String exceptionalSysOut = "System.out.println(\"Exceptional return value\");";
    private final static String descriptionForBoolean = "Replace with <code><pre>if (YYY) {\n\t"+exceptionalSysOut+"\n}</pre></code>";
    private final static String descriptionForBooleanNot = "Replace with <code><pre>if (!YYY) {\n\t"+exceptionalSysOut+"\n}</pre></code>";
    
    private boolean isNegated;
    
    private String methodSourceCodeForReplacement;
    
    private String description;
    
    @Override
    public String getDescription() {
        if (description == null) {
            String label = getLabel();     //force traversing, which fills in description
            if (description == null) {
                return label;       //something funky is happening, 
            } 
        }
        return description;
    }

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
        
        ASTNode fixedStatement = makeFixedExpression(rewrite, rvrFinder);
        
        if (fixedStatement != null && rvrFinder.badMethodInvocation != null) {
            //we have to call getParent() to get the statement. 
            //If we simply replace the MethodInvocation with the ifStatement (or whatever),
            //we get an extra semicolon on the end.
            rewrite.replace(rvrFinder.badMethodInvocation.getParent(), fixedStatement, null);
        }
    }
    
    private ASTNode makeFixedExpression(ASTRewrite rewrite, ReturnValueResolutionVisitor rvrFinder) {
        
        AST rootNode = rewrite.getAST();
        
        if ("boolean".equals(rvrFinder.returnType)) {
            IfStatement ifStatement = rootNode.newIfStatement();
            
            PrefixExpression negation = rootNode.newPrefixExpression();
            negation.setOperator(PrefixExpression.Operator.NOT);
            negation.setOperand((Expression) rewrite.createMoveTarget(rvrFinder.badMethodInvocation));
            ifStatement.setExpression(negation);
            
            //the block surrounds the inner statement with {}
            Block thenBlock = rootNode.newBlock();
            Statement thenStatement = makeExceptionalStatement(rootNode);
            thenBlock.statements().add(thenStatement);
            ifStatement.setThenStatement(thenBlock);
            
            return ifStatement;
        }
        
        return null;
    }

    @SuppressWarnings("unchecked")
    private Statement makeExceptionalStatement(AST rootNode) {
        //makes a statement `System.out.println("Exceptional return value");`
        QualifiedName sysout = rootNode.newQualifiedName(rootNode.newSimpleName("System"), rootNode.newSimpleName("out"));
        StringLiteral literal = rootNode.newStringLiteral();
        literal.setLiteralValue("Exceptional return value");
        
        MethodInvocation expression = rootNode.newMethodInvocation();
        expression.setExpression(sysout);
        expression.setName(rootNode.newSimpleName("println"));
        expression.arguments().add(literal);
        return rootNode.newExpressionStatement(expression );
    }

    @Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        return new ReturnValueResolutionVisitor(isNegated);
    }

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
    
    private class ReturnValueResolutionVisitor extends CustomLabelVisitor{
        public String returnType;
        public ASTNode badMethodInvocation;
        
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
            methodSourceCodeForReplacement = node.toString();
            badMethodInvocation = node;
            return false;
        }

        @Override
        public String getLabelReplacement() {
            //sets up label and description
            if ("boolean".equals(returnType)) {
                if (isNegated) {
                    description = descriptionForBooleanNot.replace("YYY", methodSourceCodeForReplacement);
                    return labelForBooleanNot.replace("YYY", methodSourceCodeForReplacement);
                }
                description = descriptionForBoolean.replace("YYY", methodSourceCodeForReplacement);
                return labelForBoolean.replace("YYY", methodSourceCodeForReplacement);
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
        
        if (!f.createNewFile()) {
            System.out.println("Exceptional return value");
        }
        
        
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
