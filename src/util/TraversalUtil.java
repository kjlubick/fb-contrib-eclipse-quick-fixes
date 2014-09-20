package util;

import javax.annotation.Nonnull;

import org.eclipse.jdt.core.dom.ASTNode;

public class TraversalUtil {

    private TraversalUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> T findClosestAncestor(@Nonnull ASTNode node, @Nonnull Class<T> parentClass) {
        ASTNode parent = node.getParent();
        while (parent != null) {
            if (parent.getClass().equals(parentClass)) {
                return (T) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

}
