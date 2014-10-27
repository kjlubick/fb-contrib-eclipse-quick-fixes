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

        try (Reader r = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8)) {
            char[] c = new char[1000];
            System.out.println(r.read(c));
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] bytes = "test".getBytes(StandardCharsets.ISO_8859_1);

        String oddlyConstructedString = new String(bytes, StandardCharsets.US_ASCII);

        System.out.println(oddlyConstructedString);
        

        oddlyConstructedString = new String(bytes, 0, 10, StandardCharsets.UTF_16);
        
        bytes = oddlyConstructedString.getBytes(StandardCharsets.UTF_16LE);
        
        oddlyConstructedString = new String(bytes, StandardCharsets.UTF_16BE);
        
        System.out.println(oddlyConstructedString + bytes.length);
    }
    
    public void testReplaceWithCharsetName(File f) throws FileNotFoundException, UnsupportedEncodingException {
        try (PrintWriter pw = new PrintWriter(f, StandardCharsets.UTF_8.name())) {
            pw.println("Hello world");
        }

        try (Scanner s = new Scanner(f, StandardCharsets.UTF_16.name())) {
            System.out.println(s.nextLine());
        }
        
        try (PrintWriter pw = new PrintWriter(f, StandardCharsets.UTF_16LE.name())) {
            pw.println("Hello world");
        }

        try (Scanner s = new Scanner(f, StandardCharsets.UTF_16BE.name())) {
            System.out.println(s.nextLine());
        }
        
        try (PrintWriter pw =
                new PrintWriter(f, StandardCharsets.US_ASCII.name())) {
            pw.println("Hello world");
        }

        try (Scanner s = new Scanner(f, StandardCharsets.ISO_8859_1.name())) {
            System.out.println(s.nextLine());
        }
    }
    
    

}
