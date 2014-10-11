package util;

import javax.annotation.Nonnull;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;

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
    
    public static ASTNode backtrackToBlock(ASTNode node) {
        //finds top-most expression that is not a block
        while (!(node.getParent() == null || node.getParent() instanceof Block)) {
            node = node.getParent();
        }
        return node;
    }

}
