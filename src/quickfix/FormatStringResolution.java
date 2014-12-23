package quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public class FormatStringResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return true;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        ASTNode node = getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
        FormatVisitor visitor = new FormatVisitor();
        node.accept(visitor);

        StringLiteral newStringLiteral = rewrite.getAST().newStringLiteral();
        newStringLiteral.setLiteralValue(visitor.fixedString);
        rewrite.replace(visitor.badStringLiteral, newStringLiteral, null);
    }

    private static class FormatVisitor extends ASTVisitor {

        public StringLiteral badStringLiteral = null;

        public String fixedString = null;

        // The same code from Formatter.format
        // %[argument_index$][flags][width][.precision][t]conversion
        private static final String formatSpecifierRegex = "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

        private static Pattern fsPattern = Pattern.compile(formatSpecifierRegex);

        private static Set<String> integralSpecifiers = new HashSet<String>();

        private static Set<String> integralTypes = new HashSet<String>();

        private static Set<String> floatSpecifiers = new HashSet<String>();

        private static Set<String> floatTypes = new HashSet<String>();

        static {
            // From http://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
            integralSpecifiers.add("d");
            integralSpecifiers.add("o");
            integralSpecifiers.add("x");
            integralSpecifiers.add("X");
            integralTypes.add("int");
            integralTypes.add("long");
            integralTypes.add("short");
            integralTypes.add("byte");

            floatSpecifiers.add("e");
            floatSpecifiers.add("E");
            floatSpecifiers.add("f");
            floatSpecifiers.add("g");
            floatSpecifiers.add("G");
            floatSpecifiers.add("a");
            floatSpecifiers.add("A");
            floatTypes.addAll(integralTypes);
            floatTypes.add("float");
            floatTypes.add("double");

        }

        @Override
        public boolean visit(MethodInvocation node) {
            if (badStringLiteral != null) {
                return false;
            }

            @SuppressWarnings("unchecked")
            List<Expression> arguments = node.arguments();
            if (arguments.size() < 2) {
                return true;
            }

            Expression firstArg = arguments.get(0);
            if (firstArg instanceof StringLiteral) {
                badStringLiteral = (StringLiteral) firstArg;
                fixedString = correctStringFormat(badStringLiteral.getLiteralValue(), arguments);
                return false;
            }

            return true;
        }

        private String correctStringFormat(String original, List<Expression> arguments) {
            StringBuilder builder = new StringBuilder(original);
            Matcher m = fsPattern.matcher(original);
            int argumentIndex = 1;
            for (int i = 0, len = original.length(); i < len && argumentIndex < arguments.size();) {
                if (m.find(i)) {

                    String type = m.group(6); // 7th group is the thing like d in %d

                    String argumentType = arguments.get(argumentIndex).resolveTypeBinding().getQualifiedName();

                    if (("b".equalsIgnoreCase(type) && !"boolean".equals(argumentType)) ||
                            (integralSpecifiers.contains(type) && !integralTypes.contains(argumentType)) ||
                            (floatSpecifiers.contains(type) && !floatTypes.contains(argumentType))) {
                        builder.setCharAt(m.end() - 1, 's');
                    }
                    argumentIndex++;
                    i = m.end();
                } else {
                    // No more valid format specifiers.
                    break;
                }
            }
            return builder.toString().replace("\n", "%n"); // fix \n
        }

    }

}
