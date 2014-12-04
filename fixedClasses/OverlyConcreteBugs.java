import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


public class OverlyConcreteBugs {
    
    @Override
    public String toString() {
        return "OverlyConcreteBugs";
    }
    
    public String getDisplay(Set<String> s, String a, String b) {
        if (s.contains(a)) {
            s.add(b);
        } else {
            s.add(a + b);
        }

        StringBuilder sb = new StringBuilder();
        Iterator<String> it = s.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
        }
        return sb.toString();
    }
    
    public void parse(ContentHandler dh, File f) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();

        xr.setContentHandler(dh);
        xr.parse(new InputSource(new FileInputStream(f)));
        
    }
    
    public void appendToList(List<String> list) {
        if (list.size() < 100) {
            list.add(toString());
        }
    }

}
