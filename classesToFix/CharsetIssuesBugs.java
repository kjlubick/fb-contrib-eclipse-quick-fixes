import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class CharsetIssuesBugs {

    public void fooReplaceWithCharset(String fileName) throws UnsupportedEncodingException {

        try (Reader r = new InputStreamReader(new FileInputStream(fileName), "UTF-8")) {
            char[] c = new char[1000];
            System.out.println(r.read(c));
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] bytes = "test".getBytes("ISO-8859-1");

        String oddlyConstructedString = new String(bytes, "US-ASCII");

        System.out.println(oddlyConstructedString);
        

        oddlyConstructedString = new String(bytes, 0, 10, "UTF-16");
        
        bytes = oddlyConstructedString.getBytes("UTF-16LE");
        
        oddlyConstructedString = new String(bytes, "UTF-16BE");
        
        System.out.println(oddlyConstructedString + bytes.length);
    }
    
    public void testReplaceWithCharsetName(File f) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter pw = new PrintWriter(f, "UTF-8")) {
            pw.println("Hello world");
        }

        try (Scanner s = new Scanner(f, "UTF-16")) {
            System.out.println(s.nextLine());
        }
        
        try (PrintWriter pw = new PrintWriter(f, "UTF-16LE")) {
            pw.println("Hello world");
        }

        try (Scanner s = new Scanner(f, "UTF-16BE")) {
            System.out.println(s.nextLine());
        }
        
        try (PrintWriter pw =
                new PrintWriter(f, "US-ASCII")) {
            pw.println("Hello world");
        }

        try (Scanner s = new Scanner(f,
                "ISO-8859-1")) {
            System.out.println(s.nextLine());
        }
    }
    
    

}
