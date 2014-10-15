package quickfix;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.QMethod;

public class ReturnValueIgnoreResolution extends BugResolution {
    
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
    
    private static class PrescanVisitor extends ApplicabilityVisitor {

        private static Map<QMethod, Boolean> supportsLocalFix = new HashMap<QMethod, Boolean>();
        
        static {
            supportsLocalFix.put(new QMethod("java.lang.String", "trim"), Boolean.TRUE);
            supportsLocalFix.put(new QMethod("java.lang.ProcessBuilder", "redirectErrorStream"), Boolean.FALSE);
        }
        
        private boolean applicable = true;
        
        @Override
        public boolean visit(MethodInvocation node) {
            
            QMethod qMethod = QMethod.make(node);
            
            if (supportsLocalFix.containsKey(qMethod)) {
                applicable = supportsLocalFix.get(qMethod);
            }
            
            return false;
        }
        
        
        @Override
        public boolean isApplicable() {
            return applicable;
        }
        
    }
    

}
