package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.addImports;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;

import com.mebigfatguy.fbcontrib.detect.CharsetIssues;

import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.apache.bcel.generic.Type;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;

import util.CustomLabelUtil;
import util.CustomLabelVisitor;

public class CharsetIssuesResolution extends BugResolution {

    private boolean isName;
    
    private String customizedLabel = null;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        isName = Boolean.parseBoolean(options.get("isName"));
    }
    
    @Override
    public String getLabel() {
        if (customizedLabel == null) {
            IMarker marker = getMarker();
            String labelReplacement = CustomLabelUtil.findLabelReplacement(marker, new CSILabelVisitor(isName));
            customizedLabel = super.getLabel().replace(CustomLabelUtil.PLACEHOLDER_STRING, labelReplacement);
        }
       return customizedLabel;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        CSIVisitorAndFixer visitor = new CSIVisitorAndFixer(isName, rewrite);
        node.accept(visitor);

        ASTNode badUseOfLiteral = visitor.getBadInvocation();
        ASTNode fixedUseOfStandardCharset = visitor.getFixedInvocation();

        rewrite.replace(badUseOfLiteral, fixedUseOfStandardCharset, null);
        addImports(rewrite, workingUnit, "java.nio.charset.StandardCharsets");
    }
    
    private final static class CSILabelVisitor extends CustomLabelVisitor {

        public CSILabelVisitor(boolean isName) {
            // TODO Auto-generated constructor stub
        }

        @Override
        public String getLabelReplacement() {
            return CustomLabelUtil.DEFAULT_REPLACEMENT;
        }
        
    }

    private final static class CSIVisitorAndFixer extends ASTVisitor {

        private Map<QTypeAndArgs, Object> csiConstructors;

        private Map<QTypeAndArgs, Object> csiMethods;

        private ASTNode fixedAstNode = null;

        private boolean needsToInvokeName; // should use StandardCharsets.UTF_8.name() instead of StandardCharsets

        private AST rootAstNode;

        private ASTRewrite rewrite;

        private ASTNode badConstructorInvocation;

        private ASTNode badMethodInvocation;

        public CSIVisitorAndFixer(boolean needsToInvokeName, ASTRewrite rewrite) {
            if (needsToInvokeName) {
                parseToTypeArgs(CharsetIssues.UNREPLACEABLE_ENCODING_METHODS);
            } else {
                parseToTypeArgs(CharsetIssues.REPLACEABLE_ENCODING_METHODS);
            }
            this.needsToInvokeName = needsToInvokeName;
            this.rootAstNode = rewrite.getAST();
            this.rewrite = rewrite;
        }


        public ASTNode getFixedInvocation() {
            return fixedAstNode;
        }

        public ASTNode getBadInvocation() {
            if (badConstructorInvocation != null) {
                return badConstructorInvocation;
            }
            return badMethodInvocation;
        }

        private void parseToTypeArgs(Map<String, ? extends Object> map) {
            this.csiConstructors = new HashMap<>();
            this.csiMethods = new HashMap<>();

            for (Entry<String, ? extends Object> entry : map.entrySet()) {
                QTypeAndArgs struct = new QTypeAndArgs(entry.getKey());
                if (struct.wasConstructor) {
                    csiConstructors.put(struct, entry.getValue());
                }
                else {
                    csiMethods.put(struct, entry.getValue());
                }
            }

        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(MethodInvocation node) {
            if (foundThingToReplace()) {
                return false;
            }
            QTypeAndArgs key = new QTypeAndArgs(node);

            if (csiMethods.containsKey(key)) {
                List<Expression> arguments = node.arguments();
                Integer indexVal = (Integer) csiMethods.get(key);
                int indexOfArgumentToReplace = (arguments.size() - indexVal) - 1;

                // if this was a constant string, resolveConstantExpressionValue() will be nonnull
                if (null != arguments.get(indexOfArgumentToReplace).resolveConstantExpressionValue()) {
                    this.badMethodInvocation = node;
                    fixedAstNode = makeFixedMethodInvocation(node, indexOfArgumentToReplace);
                    return false; // don't keep parsing
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private MethodInvocation makeFixedMethodInvocation(MethodInvocation node, int indexOfArgumentToReplace) {
            MethodInvocation newNode = rootAstNode.newMethodInvocation();
            newNode.setExpression((Expression) rewrite.createCopyTarget(node.getExpression()));
            newNode.setName(rootAstNode.newSimpleName(node.getName().getIdentifier()));

            List<Expression> newArgs = newNode.arguments();
            List<Expression> oldArgs = node.arguments();

            copyArgsAndReplaceWithCharset(oldArgs, newArgs, indexOfArgumentToReplace);
            return newNode;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean visit(ClassInstanceCreation node) {
            if (foundThingToReplace()) {
                return false;
            }
            QTypeAndArgs key = new QTypeAndArgs(node);

            if (csiConstructors.containsKey(key)) {
                List<Expression> arguments = node.arguments();
                Integer indexVal = (Integer) csiConstructors.get(key);
                int indexOfArgumentToReplace = (arguments.size() - indexVal) - 1;

                // if this was a constant string, resolveConstantExpressionValue() will be nonnull
                if (null != arguments.get(indexOfArgumentToReplace).resolveConstantExpressionValue()) {
                    this.badConstructorInvocation = node;
                    fixedAstNode = makeFixedConstructorInvocation(node, indexOfArgumentToReplace);
                    return false; // don't keep parsing
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private ASTNode makeFixedConstructorInvocation(ClassInstanceCreation node, int indexOfArgumentToReplace) {
            ClassInstanceCreation newNode = rootAstNode.newClassInstanceCreation();
            newNode.setType((org.eclipse.jdt.core.dom.Type) rewrite.createCopyTarget(node.getType()));

            List<Expression> newArgs = newNode.arguments();
            List<Expression> oldArgs = node.arguments();

            copyArgsAndReplaceWithCharset(oldArgs, newArgs, indexOfArgumentToReplace);
            return newNode;
        }

        private void copyArgsAndReplaceWithCharset(List<Expression> oldArgs, List<Expression> newArgs,
                int indexOfArgumentToReplace) {
            for (int i = 0; i < oldArgs.size(); i++) {
                if (i != indexOfArgumentToReplace) {
                    newArgs.add((Expression) rewrite.createCopyTarget(oldArgs.get(i)));
                } else {
                    newArgs.add(makeCharsetReplacement(oldArgs.get(indexOfArgumentToReplace)));
                }
            }
        }

        private Expression makeCharsetReplacement(Expression argument) {
            String stringLiteral = (String) argument.resolveConstantExpressionValue();
            stringLiteral = stringLiteral.replace('-', '_');
            QualifiedName qualifiedCharset = rootAstNode.newQualifiedName(rootAstNode.newName("StandardCharsets"),
                    rootAstNode.newSimpleName(stringLiteral));
            if (needsToInvokeName) {
                MethodInvocation charsetName = rootAstNode.newMethodInvocation();
                charsetName.setExpression(qualifiedCharset);
                charsetName.setName(rootAstNode.newSimpleName("name"));
                return charsetName;
            }
            return qualifiedCharset;
        }

        private boolean foundThingToReplace() {
            return this.fixedAstNode != null;
        }
    }

    private static class QTypeAndArgs {
        public boolean wasConstructor;

        final String qualifiedType;

        final List<String> argumentTypes = new ArrayList<String>();

        // expecting in form "java/io/InputStreamReader.<init>(Ljava/io/InputStream;Ljava/lang/String;)V"
        public QTypeAndArgs(String fullSignatureWithArgs) {
            int splitIndex = fullSignatureWithArgs.indexOf('(');
            String qualifiedTypeWithSlashes = fullSignatureWithArgs.substring(0, splitIndex);

            qualifiedType = qualifiedTypeWithSlashes.replace('/', '.');

            Type[] bcelTypes = Type.getArgumentTypes(fullSignatureWithArgs.substring(splitIndex));

            for (Type t : bcelTypes) {
                argumentTypes.add(t.toString()); // toString returns them in human-readable dot notation
            }

            wasConstructor = fullSignatureWithArgs.contains("<init>");
        }

        private QTypeAndArgs(String qualifiedName, List<Expression> arguments) {
            this.qualifiedType = qualifiedName;
            for (Expression type : arguments) {
                argumentTypes.add(type.resolveTypeBinding().getQualifiedName());
            }
        }

        @SuppressWarnings("unchecked")
        public QTypeAndArgs(ClassInstanceCreation node) {
            this(node.getType().resolveBinding().getQualifiedName() + ".<init>",
                    node.arguments());
            wasConstructor = true;
        }

        @SuppressWarnings("unchecked")
        public QTypeAndArgs(MethodInvocation node) {
            this(node.getExpression().resolveTypeBinding().getQualifiedName() + '.' + node.getName().getIdentifier(),
                    node.arguments());
            wasConstructor = false;
        }

        @Override
        public String toString() {
            return "QTypeAndArgs [wasConstructor=" + wasConstructor + ", qualifiedType=" + qualifiedType
                    + ", argumentTypes=" + argumentTypes + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((argumentTypes == null) ? 0 : argumentTypes.hashCode());
            result = prime * result + ((qualifiedType == null) ? 0 : qualifiedType.hashCode());
            return prime * result + (wasConstructor ? 1231 : 1237);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            QTypeAndArgs other = (QTypeAndArgs) obj;
            if (argumentTypes == null) {
                if (other.argumentTypes != null)
                    return false;
            } else if (!argumentTypes.equals(other.argumentTypes))
                return false;
            if (qualifiedType == null) {
                if (other.qualifiedType != null)
                    return false;
            } else if (!qualifiedType.equals(other.qualifiedType))
                return false;
            if (wasConstructor != other.wasConstructor)
                return false;
            return true;
        }

    }

}
