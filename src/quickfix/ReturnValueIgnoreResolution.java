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
    
    private static Set<String> immutableTypes = new HashSet<String>();
    private static Set<QMethod> shouldNotBeIgnored = new HashSet<QMethod>();
    
    
    static {
        immutableTypes.add("java.lang.String");
        immutableTypes.add("java.math.BigDecimal");
        immutableTypes.add("java.math.BigInteger");
        immutableTypes.add("java.sql.Connection");
        immutableTypes.add("java.net.InetAddress");  
        immutableTypes.add("jsr166z.forkjoin.ParallelArray");
        immutableTypes.add("jsr166z.forkjoin.ParallelLongArray");
        immutableTypes.add("jsr166z.forkjoin.ParallelDoubleArray");
        
        
        //TODO import bad_practice methods and doublecheck String and some other things I may have missed
        shouldNotBeIgnored.add(new QMethod("java.io.File", "createNewFile"));
        shouldNotBeIgnored.add(new QMethod("java.util.Iterator", "hasNext"));
        shouldNotBeIgnored.add(new QMethod("java.security.MessageDigest", "digest"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.ReadWriteLock", "readLock"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.ReadWriteLock", "writeLock"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.Condition", "await"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.CountDownLatch", "await"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.Condition", "awaitUntil"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.Condition", "awaitNanos"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.Semaphore", "tryAcquire"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.Lock", "tryLock"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.Lock", "newCondition"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.locks.Lock", "tryLock"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.BlockingQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.ConcurrentLinkedQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.DelayQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.LinkedBlockingQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.LinkedList", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.Queue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.ArrayBlockingQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.SynchronousQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.PriorityQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.PriorityBlockingQueue", "offer"));
        shouldNotBeIgnored.add(new QMethod("java.util.concurrent.BlockingQueue", "poll"));
        shouldNotBeIgnored.add(new QMethod("java.util.Queue", "poll"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "getBytes"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "charAt"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "toString"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "length"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "matches"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "intern"));
        shouldNotBeIgnored.add(new QMethod("java.lang.String", "<init>"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "inflate"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "precision"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "toBigIntegerExact"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "longValueExact"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "intValueExact"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "shortValueExact"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "byteValueExact"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "<init>"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "intValue"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigDecimal", "stripZerosToMatchScale"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigInteger", "addOne"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigInteger", "subN"));
        shouldNotBeIgnored.add(new QMethod("java.math.BigInteger", "<init>"));
        shouldNotBeIgnored.add(new QMethod("java.net.InetAddress", "getByName"));
        shouldNotBeIgnored.add(new QMethod("java.net.InetAddress", "getAllByName"));
        shouldNotBeIgnored.add(new QMethod("java.lang.ProcessBuilder", "redirectErrorStream"));
        shouldNotBeIgnored.add(new QMethod("java.sql.Statement", "executeQuery"));
        shouldNotBeIgnored.add(new QMethod("java.sql.PreparedStatement", "executeQuery")); 
      
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
            String returnType = node.resolveTypeBinding().getQualifiedName();
            
            //check for the special cases in shouldNotBeIgnored
            if (shouldNotBeIgnored.contains(qMethod) && !"void".equals(returnType)) { //TODO check for void return value
                badMethodInvocation = node;
                this.returnTypeOfMethod = returnType;
                
                // look at the returned value and see if it equals the same type
                // as what the method is invoked on. 
                if (qMethod.qualifiedTypeString.equals(returnTypeOfMethod)) {
                    returnsSelf = TriStatus.TRUE;
                } else {
                    returnsSelf = TriStatus.FALSE;
                }
            }
            
            //check for any immutableType methods that return something of the same type
            if (immutableTypes.contains(returnType) && qMethod.qualifiedTypeString.equals(returnType)) {
                returnsSelf = TriStatus.TRUE;
                
                badMethodInvocation = node;
                this.returnTypeOfMethod = returnType;
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
