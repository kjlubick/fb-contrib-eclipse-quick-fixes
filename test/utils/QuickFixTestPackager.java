package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.*;

public class QuickFixTestPackager {

    private final List<QuickFixTestPackage> packages = new ArrayList<>();

    public void addBugPatterns(String... expectedPatterns) {
        for (int i = 0; i < expectedPatterns.length; i++) {
            String pattern = expectedPatterns[i];
            if (packages.size() <= i) {
                packages.add(new QuickFixTestPackage());
            }
            packages.get(i).expectedPattern = pattern;
        }
    }

    /**
     * Validates and sorts the compiled QuickFixTestPackaages by pattern name,
     * then by line number
     * 
     * @return a sorted list of QuickFixTestPackages to be used in assertions.
     */
    public List<QuickFixTestPackage> asList() {
        validatePackages();
        Collections.sort(packages, new Comparator<QuickFixTestPackage>() {

            @Override
            public int compare(QuickFixTestPackage o1, QuickFixTestPackage o2) {
                if (o1.expectedPattern.equals(o2.expectedPattern)) {
                    return o1.lineNumber - o2.lineNumber;
                }
                return o1.expectedPattern.compareTo(o2.expectedPattern);
            }
        });
        return Collections.unmodifiableList(packages);
    }

    private void validatePackages() {
        assertNotEquals("Did you forget to add anything to the packager?", 0, packages.size());
        for (QuickFixTestPackage p : packages) {
            assertNotNull("Not all labels were initialized", p.expectedLabels);
            assertNotNull("Not all patterns were initialized", p.expectedPattern);
            assertNotEquals("Not all line numbers were initialized", -1, p.lineNumber);
        }
    }

    /*
     * Could be more than one at a given index, so they need to be specified individually
     */
    public void setExpectedLabels(int index, String... expectedLabels) {
        while (packages.size() <= index) {
            packages.add(new QuickFixTestPackage());
        }
        packages.get(index).expectedLabels = Arrays.asList(expectedLabels);

    }

    public void addExpectedLines(int... lineNumbers) {
        for (int i = 0; i < lineNumbers.length; i++) {
            int lineNumber = lineNumbers[i];
            if (packages.size() <= i) {
                packages.add(new QuickFixTestPackage());
            }
            packages.get(i).lineNumber = lineNumber;
        }
    }

}
