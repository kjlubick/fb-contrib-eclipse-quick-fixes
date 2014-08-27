package util;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class CustomLabelUtil {
    public static final String DEFAULT_REPLACEMENT = "XXX";

    public static final String PLACEHOLDER_STRING = "YYY";

    // from BugResolution.java in findBugsEclipsePlugin
    private static ASTNode getNodeForMarker(IMarker marker) throws JavaModelException, ASTNodeNotFoundException {
        BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
        if (bug == null) {
            return null;
        }
        ICompilationUnit originalUnit = getCompilationUnit(marker);
        if (originalUnit == null) {
            return null;
        }

        CompilationUnit workingUnit = createWorkingCopy(originalUnit);

        return getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
    }

    // from BugResolution.java in findBugsEclipsePlugin
    @SuppressWarnings("deprecation")
    private static CompilationUnit createWorkingCopy(ICompilationUnit unit) throws JavaModelException {
        unit.becomeWorkingCopy(null);
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null);
    }

    // from BugResolution.java in findBugsEclipsePlugin
    private static ICompilationUnit getCompilationUnit(IMarker marker) {
        IResource res = marker.getResource();
        if (res instanceof IFile && res.isAccessible()) {
            IJavaElement element = JavaCore.create((IFile) res);
            if (element instanceof ICompilationUnit) {
                return (ICompilationUnit) element;
            }
        }
        return null;
    }

    public static String findLabelReplacement(IMarker marker, CustomLabelVisitor labelVisitor) {
        try {
            ASTNode node = getNodeForMarker(marker);
            if (node != null) {
                node.accept(labelVisitor);
                return labelVisitor.getLabelReplacement();
            }
            // Catch all exceptions (explicit) so that the label creation won't fail
            // FindBugs prefers this being explicit
        } catch (JavaModelException | ASTNodeNotFoundException | RuntimeException e) {
            return DEFAULT_REPLACEMENT;
        }
        return DEFAULT_REPLACEMENT;
    }
}
