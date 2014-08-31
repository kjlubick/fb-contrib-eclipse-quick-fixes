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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelBugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.apache.bcel.generic.Type;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class CharsetIssuesResolution extends CustomLabelBugResolution {

    private boolean isName;

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    public void setOptions(@Nonnull Map<String, String> options) {
        isName = Boolean.parseBoolean(options.get("isName"));
    }

    @Override
    protected CustomLabelVisitor getLabelFixingVisitor() {
        return new CSIVisitorAndFixer(isName);
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

    private final static class CSIVisitorAndFixer extends CustomLabelVisitor {

        private Map<QTypeAndArgs, Object> csiConstructors;

        private Map<QTypeAndArgs, Object> csiMethods;

        private ASTNode fixedAstNode = null;

        private boolean needsToInvokeName; // should use StandardCharsets.UTF_8.name() instead of StandardCharsets

        private AST rootAstNode;

        private ASTRewrite rewrite;

        private ASTNode badConstructorInvocation;

        private ASTNode badMethodInvocation;

        private String literalValue = null;

        public CSIVisitorAndFixer(boolean needsToInvokeName, ASTRewrite rewrite) {
            this(needsToInvokeName);
            this.rootAstNode = rewrite.getAST();
            this.rewrite = rewrite;
        }

        public CSIVisitorAndFixer(boolean needsToInvokeName) { // for label traversing
            if (needsToInvokeName) {
                parseToTypeArgs(CharsetIssues.UNREPLACEABLE_ENCODING_METHODS);
            } else {
                parseToTypeArgs(CharsetIssues.REPLACEABLE_ENCODING_METHODS);
            }
            this.needsToInvokeName = needsToInvokeName;
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
                Object literalString = arguments.get(indexOfArgumentToReplace).resolveConstantExpressionValue();
                if (null != literalString) {
                    this.literalValue = literalString.toString();
                    this.badMethodInvocation = node;
                    fixedAstNode = makeFixedMethodInvocation(node, indexOfArgumentToReplace);
                    return false; // don't keep parsing
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private MethodInvocation makeFixedMethodInvocation(MethodInvocation node, int indexOfArgumentToReplace) {
            if (rootAstNode == null || rewrite == null) {
                return null;
            }
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
                Object literalString = arguments.get(indexOfArgumentToReplace).resolveConstantExpressionValue();
                if (null != literalString) {
                    this.literalValue = literalString.toString();
                    this.badConstructorInvocation = node;
                    fixedAstNode = makeFixedConstructorInvocation(node, indexOfArgumentToReplace);
                    return false; // don't keep parsing
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        private ASTNode makeFixedConstructorInvocation(ClassInstanceCreation node, int indexOfArgumentToReplace) {
            if (rootAstNode == null || rewrite == null) {
                return null;
            }
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
                    newArgs.add(makeCharsetReplacement());
                }
            }
        }

        private Expression makeCharsetReplacement() {
            if (literalValue != null) {
                String stringLiteral = literalValue.replace('-', '_');
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
            throw new RuntimeException("No String literal in CSI quickfix");
        }

        private boolean foundThingToReplace() {
            return this.fixedAstNode != null;
        }

        @Override
        public String getLabelReplacement() {
            return literalValue.replace('-', '_');
        }
    }

    // basically adds a bridge between fb-contrib's representation of this and the AST's view of them.
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
