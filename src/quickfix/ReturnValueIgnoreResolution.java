package quickfix;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.ApplicabilityVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.CustomLabelVisitor;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import util.QMethod;

public class ReturnValueIgnoreResolution extends BugResolution {
    
    private final static String exceptionalSysOut = "System.out.println(\"Exceptional return value\");";

    private final static String descriptionForWrapIf = "Replace with <code><pre>if (YYY) {\n\t" + exceptionalSysOut
            + "\n}</pre></code>";

    private final static String descriptionForNegatedWrapIf = "Replace with <code><pre>if (!YYY) {\n\t" + exceptionalSysOut
            + "\n}</pre></code>";
    
    private final static String descriptionForNewLocal = "Makes a new local variable and assigns the result of the method call to it.";
    
    public final static String descriptionForStoreToSelf = "Stores the result of the method call back to the original method caller.";
    
    private String description;

    
    
    private enum TriStatus {
        UNRESOLVED, TRUE, FALSE
    }
        
    private enum QuickFixType {
        STORE_TO_NEW_LOCAL(descriptionForNewLocal), STORE_TO_SELF(descriptionForStoreToSelf),
        WRAP_WITH_IF(descriptionForWrapIf), WRAP_WITH_NEGATED_IF(descriptionForNegatedWrapIf);
        
        private String description;
        
        QuickFixType(String d) {
            description = d;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private QuickFixType quickFixType;

    @Override
    public void setOptions(Map<String, String> options) {
        quickFixType = QuickFixType.valueOf(options.get("resolutionType"));
    }

    @Override
    protected boolean resolveBindings() {
        return true;
    }
    
    @Override
    protected ASTVisitor getApplicabilityVisitor() {
        return new PrescanVisitor();
    }
    
    @Override
    protected ASTVisitor getLabelFixingVisitor() {
        return new PrescanVisitor();
    }
    
    @Override
    public String getDescription() {
        if (description == null) {
            String label = getLabel(); // force traversing, which fills in description
            if (description == null) {
                return label; // something funky is happening, description shouldn't be null
                              // We'll be safe and return label (which is not null)
            }
        }
        return description;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        // TODO Auto-generated method stub

    }
    
    private static Set<QMethod> supportsQuickFix = new HashSet<QMethod>();
    
    static {
        //TODO import bad_practice methods and doublecheck String and some other things I may have missed
        supportsQuickFix.add(new QMethod("java.io.File", "createNewFile"));
        supportsQuickFix.add(new QMethod("java.util.Iterator", "hasNext"));
        supportsQuickFix.add(new QMethod("java.security.MessageDigest", "digest"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.ReadWriteLock", "readLock"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.ReadWriteLock", "writeLock"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.Condition", "await"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.CountDownLatch", "await"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.Condition", "awaitUntil"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.Condition", "awaitNanos"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.Semaphore", "tryAcquire"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.Lock", "tryLock"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.Lock", "newCondition"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.locks.Lock", "tryLock"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.BlockingQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.ConcurrentLinkedQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.DelayQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.LinkedBlockingQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.LinkedList", "offer"));
        supportsQuickFix.add(new QMethod("java.util.Queue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.ArrayBlockingQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.SynchronousQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.PriorityQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.PriorityBlockingQueue", "offer"));
        supportsQuickFix.add(new QMethod("java.util.concurrent.BlockingQueue", "poll"));
        supportsQuickFix.add(new QMethod("java.util.Queue", "poll"));
        supportsQuickFix.add(new QMethod("java.lang.String", "getBytes"));
        supportsQuickFix.add(new QMethod("java.lang.String", "charAt"));
        supportsQuickFix.add(new QMethod("java.lang.String", "toString"));
        supportsQuickFix.add(new QMethod("java.lang.String", "length"));
        supportsQuickFix.add(new QMethod("java.lang.String", "matches"));
        supportsQuickFix.add(new QMethod("java.lang.String", "intern"));
        supportsQuickFix.add(new QMethod("java.lang.String", "<init>"));
        supportsQuickFix.add(new QMethod("java.lang.String", "trim"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "inflate"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "precision"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "toBigIntegerExact"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "longValueExact"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "intValueExact"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "shortValueExact"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "byteValueExact"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "<init>"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "intValue"));
        supportsQuickFix.add(new QMethod("java.math.BigDecimal", "stripZerosToMatchScale"));
        supportsQuickFix.add(new QMethod("java.math.BigInteger", "addOne"));
        supportsQuickFix.add(new QMethod("java.math.BigInteger", "subN"));
        supportsQuickFix.add(new QMethod("java.math.BigInteger", "<init>"));
        supportsQuickFix.add(new QMethod("java.net.InetAddress", "getByName"));
        supportsQuickFix.add(new QMethod("java.net.InetAddress", "getAllByName"));
        supportsQuickFix.add(new QMethod("java.lang.ProcessBuilder", "redirectErrorStream"));
        supportsQuickFix.add(new QMethod("java.sql.Statement", "executeQuery"));
        supportsQuickFix.add(new QMethod("java.sql.PreparedStatement", "executeQuery")); 
      
    }
    
    private class PrescanVisitor extends ASTVisitor implements ApplicabilityVisitor, CustomLabelVisitor {
        
        private TriStatus returnsSelf = TriStatus.UNRESOLVED;
        private String returnTypeOfMethod;
        private MethodInvocation badMethodInvocation;
        
        @Override
        public boolean visit(MethodInvocation node) {
            if (badMethodInvocation != null) {
                return false; // only need to go one layer deep. By definition,
                              // if the return value is ignored, it's not nested in anything
            }
            
            QMethod qMethod = QMethod.make(node);
            
            if (supportsQuickFix.contains(qMethod)) {
                badMethodInvocation = node;
                
                // look at the returned value and see if it equals the same type
                // as what the method is invoked on. 
                returnTypeOfMethod = node.resolveTypeBinding().getQualifiedName();
                
                if (qMethod.qualifiedTypeString.equals(returnTypeOfMethod)) {
                    returnsSelf = TriStatus.TRUE;
                } else {
                    returnsSelf = TriStatus.FALSE;
                }
            }
            
            return false;
        }
        
        
        @Override
        public boolean isApplicable() {
            switch (quickFixType) {
            case STORE_TO_NEW_LOCAL:
                return badMethodInvocation != null;
            case STORE_TO_SELF:
                return returnsSelf == TriStatus.TRUE;
            case WRAP_WITH_IF:
                return "boolean".equals(returnTypeOfMethod);
            case WRAP_WITH_NEGATED_IF:
                return "boolean".equals(returnTypeOfMethod);
            default:
                return false;
            }
        }


        @Override
        public String getLabelReplacement() {
            switch (quickFixType) {
            case STORE_TO_NEW_LOCAL:
            case STORE_TO_SELF:
            default:
                description = quickFixType.getDescription();
                return "";
            case WRAP_WITH_IF:
            case WRAP_WITH_NEGATED_IF:
                String methodSourceCode = badMethodInvocation != null ? badMethodInvocation.toString() : "[method call]";
                description = quickFixType.getDescription().replace("YYY", methodSourceCode);
                // it's okay to invoke toString() here because it's user facing, not actually being turned into code
                return methodSourceCode;
            }
        }
        
    }
    

}
