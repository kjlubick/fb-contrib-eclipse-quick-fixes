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

import util.QMethodAndArgs;

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
        return new CSIVisitorAndFixer();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        CSIVisitorAndFixer visitor = new CSIVisitorAndFixer(rewrite);
        node.accept(visitor);

        ASTNode badUseOfLiteral = visitor.getBadInvocation();
        ASTNode fixedUseOfStandardCharset = visitor.getFixedInvocation();

        rewrite.replace(badUseOfLiteral, fixedUseOfStandardCharset, null);
        addImports(rewrite, workingUnit, "java.nio.charset.StandardCharsets");
    }

    private final class CSIVisitorAndFixer extends CustomLabelVisitor {

        private Map<QMethodAndArgs, Object> csiConstructors;

        private Map<QMethodAndArgs, Object> csiMethods;

        private ASTNode fixedAstNode = null;

        private boolean needsToInvokeName; // should use StandardCharsets.UTF_8.name() instead of StandardCharsets

        private AST rootAstNode;

        private ASTRewrite rewrite;

        private ASTNode badConstructorInvocation;

        private ASTNode badMethodInvocation;

        private String literalValue = null;

        public CSIVisitorAndFixer(ASTRewrite rewrite) {
            this();
            this.rootAstNode = rewrite.getAST();
            this.rewrite = rewrite;
        }

        public CSIVisitorAndFixer() { // for label traversing
            if (isName) {
                parseToTypeArgs(CharsetIssues.UNREPLACEABLE_ENCODING_METHODS);
            } else {
                parseToTypeArgs(CharsetIssues.REPLACEABLE_ENCODING_METHODS);
            }
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
                QMethodAndArgs struct = make(entry.getKey());
                if (QMethodAndArgs.CONSTRUCTOR_METHOD.equals(struct.invokedMethodString)) {
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
            QMethodAndArgs key = QMethodAndArgs.make(node);

            if (csiMethods.containsKey(key)) {
                List<Expression> arguments = node.arguments();
                Integer indexVal = (Integer) csiMethods.get(key);
                // converts from ith from the end to nth (from the beginning)
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
            QMethodAndArgs key = QMethodAndArgs.make(node);

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

        // expecting in form "java/io/InputStreamReader.<init>(Ljava/io/InputStream;Ljava/lang/String;)V"
        private QMethodAndArgs make(String fullSignatureWithArgs) {
            int firstSplitIndex = fullSignatureWithArgs.indexOf('.');
            int secondSplitIndex = fullSignatureWithArgs.indexOf('(');
            String qualifiedTypeWithSlashes = fullSignatureWithArgs.substring(0, firstSplitIndex);

            String qtype = qualifiedTypeWithSlashes.replace('/', '.');
            String method = fullSignatureWithArgs.substring(firstSplitIndex + 1, secondSplitIndex);

            List<String> argumentTypes = new ArrayList<>();

            Type[] bcelTypes = Type.getArgumentTypes(fullSignatureWithArgs.substring(secondSplitIndex));

            for (Type t : bcelTypes) {
                argumentTypes.add(t.toString()); // toString returns them in human-readable dot notation
            }

            return new QMethodAndArgs(qtype, method, argumentTypes);
        }
    }

}
