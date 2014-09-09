package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;

/**
 * A string representation of a method call and its arguments.
 * 
 * Usually used as a bridge between external libraries' datastructures.
 * 
 * It is suggested clients make a static factory
 * @author Kevin Lubick
 *
 */
public final class QMethodAndArgs {

    public final static String CONSTRUCTOR_METHOD = "<init>";
    public final String qualifiedTypeString;      //the type that this method is invoked on (dot seperated)
    public final String invokedMethodString;      //the name of the method invoked
    public final List<String> argumentTypes;      //dot separated argument types
    
    
    public QMethodAndArgs(String qualifiedTypeString, String invokedMethodString, List<String> argumentTypes) {
        this.qualifiedTypeString = qualifiedTypeString;
        this.invokedMethodString = invokedMethodString;
        this.argumentTypes = Collections.unmodifiableList(argumentTypes);
    }
    
    public static List<String> expressionsToTypeStrings(List<Expression> expressions) {
        List<String> list = new ArrayList<>();
        for (Expression type : expressions) {
            list.add(type.resolveTypeBinding().getQualifiedName());
        }
        return list;
    }
    
    @SuppressWarnings("unchecked")
    public static QMethodAndArgs make(ClassInstanceCreation node) {
        String qtype = node.getType().resolveBinding().getQualifiedName();
        String method = CONSTRUCTOR_METHOD;
        return new QMethodAndArgs(qtype, method, expressionsToTypeStrings(node.arguments()));
    }
    
    @SuppressWarnings("unchecked")
    public static QMethodAndArgs make(MethodInvocation node) {
        String qtype = node.getExpression().resolveTypeBinding().getQualifiedName();
        String method = node.getName().getIdentifier();
        return new QMethodAndArgs(qtype, method, expressionsToTypeStrings(node.arguments()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argumentTypes == null) ? 0 : argumentTypes.hashCode());
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
        QMethodAndArgs other = (QMethodAndArgs) obj;
        if (argumentTypes == null) {
            if (other.argumentTypes != null)
                return false;
        } else if (!argumentTypes.equals(other.argumentTypes))
            return false;
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
