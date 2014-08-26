package util;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

public abstract class CustomLabelBugResolution extends BugResolution {
    
    protected String customizedLabel;

    @Override
    public String getLabel() {
        if (customizedLabel == null) {
            IMarker marker = getMarker();
            String labelReplacement = CustomLabelUtil.findLabelReplacement(marker, getLabelFixingVisitor());
            customizedLabel = super.getLabel().replace(CustomLabelUtil.PLACEHOLDER_STRING, labelReplacement);
        }
       return customizedLabel;
    }

    protected abstract CustomLabelVisitor getLabelFixingVisitor();

}
