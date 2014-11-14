import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


public class ReturnValueIgnoredBugs {

    
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        
        if (!file.exists()) {
            if (!file.createNewFile()) {
                System.out.println("Exceptional return value");
            }
        }
        
        System.out.println(file.getAbsolutePath());
        
        if (file.delete()) {
            System.out.println("Exceptional return value");
        }
    }
    
    
    public String sanitize(String s) {
        if (s == null) {
            return "";
        }
        s = s.replace("\n", "").replace("\t", "");
        return s.trim();
    }
    
    public void task(ExecutorService es) {
        Future<?> local = es.submit(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Foo");
            }
        });
    }
}
