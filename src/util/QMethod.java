package util;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class QMethod {
    public final static String CONSTRUCTOR_METHOD = "<init>";
    
    public final String qualifiedTypeString; // the type that this method is invoked on (dot seperated)

    public final String invokedMethodString; // the name of the method invoked

    public QMethod(String qualifiedTypeString, String invokedMethodString) {
        this.qualifiedTypeString = qualifiedTypeString;
        this.invokedMethodString = invokedMethodString;
    }
    
    public static QMethod make(ClassInstanceCreation node) {
        String qtype = node.getType().resolveBinding().getQualifiedName();
        String method = CONSTRUCTOR_METHOD;
        return new QMethod(qtype, method);
    }

    public static QMethod make(MethodInvocation node) {
        String qtype = node.getExpression().resolveTypeBinding().getQualifiedName();
        String method = node.getName().getIdentifier();
        return new QMethod(qtype, method);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((invokedMethodString == null) ? 0 : invokedMethodString.hashCode());
        return prime * result + ((qualifiedTypeString == null) ? 0 : qualifiedTypeString.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        QMethod other = (QMethod) obj;
        if (invokedMethodString == null) {
            if (other.invokedMethodString != null)
                return false;
        } else if (!invokedMethodString.equals(other.invokedMethodString))
            return false;
        if (qualifiedTypeString == null) {
            if (other.qualifiedTypeString != null)
                return false;
        } else if (!qualifiedTypeString.equals(other.qualifiedTypeString))
            return false;
        return true;
    }


}
