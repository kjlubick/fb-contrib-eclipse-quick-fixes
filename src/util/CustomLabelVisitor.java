package util;

import org.eclipse.jdt.core.dom.ASTVisitor;

public abstract class CustomLabelVisitor extends ASTVisitor {
    public abstract String getLabelReplacement();
}
