package quickfix;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.QMethod;

public class ReturnValueIgnoreResolution extends BugResolution {
    
    private enum TriStatus {
        UNRESOLVED, TRUE, FALSE
    }
    
    /* I'm thinking having a QMethod map to a condition of (returns same value or not) so I know if I can offer to store to same
     * I think I also want to modify the proper FindBugs plugin to check to see if it should add a resolution or not
     * 
        addMethodAnnotation("java.util.Iterator", "hasNext", "()Z", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);

        addMethodAnnotation("java.security.MessageDigest", "digest", "([B)[B", false,
        addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "readLock", "()Ljava/util/concurrent/locks/Lock;", false,
        addMethodAnnotation("java.util.concurrent.locks.ReadWriteLock", "writeLock", "()Ljava/util/concurrent/locks/Lock;",
        addMethodAnnotation("java.util.concurrent.locks.Condition", "await", "(JLjava/util/concurrent/TimeUnit;)Z", false,
        addMethodAnnotation("java.util.concurrent.CountDownLatch", "await", "(JLjava/util/concurrent/TimeUnit;)Z", false,
        addMethodAnnotation("java.util.concurrent.locks.Condition", "awaitUntil", "(Ljava/util/Date;)Z", false,
        addMethodAnnotation("java.util.concurrent.locks.Condition", "awaitNanos", "(J)J", false,
        addMethodAnnotation("java.util.concurrent.Semaphore", "tryAcquire", "(JLjava/util/concurrent/TimeUnit;)Z", false,
    addMethodAnnotation("java.util.concurrent.Semaphore", "tryAcquire", "()Z", false,
        addMethodAnnotation("java.util.concurrent.locks.Lock", "tryLock", "(JLjava/util/concurrent/TimeUnit;)Z", false,
        addMethodAnnotation("java.util.concurrent.locks.Lock", "newCondition", "()Ljava/util/concurrent/locks/Condition;", false,
        addMethodAnnotation("java.util.concurrent.locks.Lock", "tryLock", "()Z", false,
        addMethodAnnotation("java.util.concurrent.BlockingQueue", "offer",
        addMethodAnnotation("java.util.concurrent.BlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
       addMethodAnnotation("java.util.concurrent.ConcurrentLinkedQueue", "offer", "(Ljava/lang/Object;)Z", false,
        addMethodAnnotation("java.util.concurrent.DelayQueue", "offer", "(Ljava/lang/Object;)Z", false,
        addMethodAnnotation("java.util.concurrent.LinkedBlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
       addMethodAnnotation("java.util.LinkedList", "offer", "(Ljava/lang/Object;)Z", false,
         addMethodAnnotation("java.util.Queue", "offer", "(Ljava/lang/Object;)Z", false,
      addMethodAnnotation("java.util.concurrent.ArrayBlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
       addMethodAnnotation("java.util.concurrent.SynchronousQueue", "offer", "(Ljava/lang/Object;)Z", false,
       addMethodAnnotation("java.util.PriorityQueue", "offer", "(Ljava/lang/Object;)Z", false,
        addMethodAnnotation("java.util.concurrent.PriorityBlockingQueue", "offer", "(Ljava/lang/Object;)Z", false,
        addMethodAnnotation("java.util.concurrent.BlockingQueue", "poll", "(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;",
        addMethodAnnotation("java.util.Queue", "poll", "()Ljava/lang/Object;", false,
        addMethodAnnotation("java.lang.String", "getBytes", "(Ljava/lang/String;)[B", false,
        addMethodAnnotation("java.lang.String", "charAt", "(I)C", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation("java.lang.String", "toString", "()Ljava/lang/String;", false,
        addMethodAnnotation("java.lang.String", "length", "()I", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation("java.lang.String", "matches", "(Ljava/lang/String;)Z", false,
        addMethodAnnotation("java.lang.String", "intern", "()Ljava/lang/String;", false,
        addMethodAnnotation("java.lang.String", "<init>", "([BLjava/lang/String;)V", false,
        addMethodAnnotation("java.lang.String", "<init>", "(Ljava/lang/String;)V", false,
        addMethodAnnotation("java.lang.String", "<init>", "()V", false, CheckReturnValueAnnotation.CHECK_RETURN_VALUE_LOW);
        addMethodAnnotation("java.math.BigDecimal", "inflate", "()Ljava/math/BigInteger;", false,
        addMethodAnnotation("java.math.BigDecimal", "precision", "()I", false,
        addMethodAnnotation("java.math.BigDecimal", "toBigIntegerExact", "()Ljava/math/BigInteger;", false,
        addMethodAnnotation("java.math.BigDecimal", "longValueExact", "()J", false,
        addMethodAnnotation("java.math.BigDecimal", "intValueExact", "()I", false,
        addMethodAnnotation("java.math.BigDecimal", "shortValueExact", "()S", false,
        addMethodAnnotation("java.math.BigDecimal", "byteValueExact", "()B", false,
        addMethodAnnotation("java.math.BigDecimal", "<init>", "(Ljava/lang/String;)V", false,
        addMethodAnnotation("java.math.BigDecimal", "intValue", "()I", false,
         addMethodAnnotation("java.math.BigDecimal", "stripZerosToMatchScale", "(J)Ljava/math/BigDecimal;", false,
         addMethodAnnotation("java.math.BigInteger", "addOne", "([IIII)I", true,
         addMethodAnnotation("java.math.BigInteger", "subN", "([I[II)I", true,
         addMethodAnnotation("java.math.BigInteger", "<init>", "(Ljava/lang/String;)V", false,
        addMethodAnnotation("java.net.InetAddress", "getByName", "(Ljava/lang/String;)Ljava/net/InetAddress;", true,
         addMethodAnnotation("java.net.InetAddress", "getAllByName", "(Ljava/lang/String;)[Ljava/net/InetAddress;", true,
        addMethodAnnotation("java.lang.ProcessBuilder", "redirectErrorStream", "()Z", false,
         addMethodAnnotation("java.lang.ProcessBuilder", "redirectErrorStream", "()Z", false,
        addMethodAnnotation("java.lang.ProcessBuilder", "redirectErrorStream", "()Z", false,
         addMethodAnnotation(java.sql.Statement.class, "executeQuery", "(Ljava/lang/String;)Ljava/sql/ResultSet;", false,
        addMethodAnnotation(java.sql.PreparedStatement.class, "executeQuery", "()Ljava/sql/ResultSet;", false,
 

     */
        
    public enum QuickFixType {
        STORE_TO_NEW_LOCAL, STORE_TO_SELF;
//        private String shortName;
//
//        private QuickFixType(String shortName) {
//            this.shortName = shortName;
//        }
//
//        public static QuickFixType fromString(String shortName) {
//            for (QuickFixType color : QuickFixType.values()) {
//                if (color.shortName.equals(shortName)) {
//                    return color;
//                }
//            }
//            throw new IllegalArgumentException("Illegal quickFix name: " + shortName);
//        }
    }
    
    private QuickFixType storeToLocal;

    @Override
    public void setOptions(Map<String, String> options) {
        storeToLocal = QuickFixType.valueOf(options.get("resolutionType"));
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    protected ApplicabilityVisitor getApplicabilityVisitor() {
        return new PrescanVisitor();
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        // TODO Auto-generated method stub

    }
    
    private static Set<QMethod> supportsQuickFix = new HashSet<QMethod>();
    
    static {
        supportsQuickFix.add(new QMethod("java.lang.String", "trim"));
        supportsQuickFix.add(new QMethod("java.lang.ProcessBuilder", "redirectErrorStream"));
    }
    
    private class PrescanVisitor extends ApplicabilityVisitor {
        
        
        
        private TriStatus returnsSelf = TriStatus.UNRESOLVED;
        
        @Override
        public boolean visit(MethodInvocation node) {
            
            QMethod qMethod = QMethod.make(node);
            
            if (supportsQuickFix.contains(qMethod)) {
                
                // look at the returned value and see if it equals the same type
                // as what the method is invoked on. 
                if (qMethod.qualifiedTypeString.equals(
                        node.resolveTypeBinding().getQualifiedName())) {
                    returnsSelf = TriStatus.TRUE;
                } else {
                    returnsSelf = TriStatus.FALSE;
                }
                //returnsSelf = supportsQuickFix.get(qMethod);
            }
            
            return false;
        }
        
        
        @Override
        public boolean isApplicable() {
            switch (storeToLocal) {
            case STORE_TO_NEW_LOCAL:
                return true;
            case STORE_TO_SELF:
                return returnsSelf == TriStatus.TRUE;
            default:
                return false;
            }
        }
        
    }
    

}
