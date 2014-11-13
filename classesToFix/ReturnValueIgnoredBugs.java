import java.io.File;
import java.util.concurrent.ExecutorService;


public class ReturnValueIgnoredBugs {

    
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        
        if (!file.exists()) {
            file.createNewFile();
        }
        
        System.out.println(file.getAbsolutePath());
        
        file.delete();
    }
    
    
    public String sanitize(String s) {
        if (s == null) {
            return "";
        }
        s.replace("\n", "").replace("\t", "");
        return s.trim();
    }
    
    public void task(ExecutorService es) {
        es.submit(new Runnable() {
            
            @Override
            public void run() {
                System.out.println("Foo");
            }
        });
    }
}
