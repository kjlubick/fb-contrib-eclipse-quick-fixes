package utils;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A utility class to concisely generate lists of QuickFixTestPackage
 * (via <code>asList()</code>) to be used to effectively assert bug patterns
 * generated match expected behavior.
 * 
 * A typical use looks like:
 * 
 * <pre>
 * {@code
 * QuickFixTestPackager packager = new QuickFixTestPackager();
 * packager.addExpectedLines(13, 17, ... /* line numbers * /);
 * packager.addExpectedBugPatterns("FOO_BUG_PATTERN", "FOO_OTHER_PATTERN", ... /* bugPatterns * /);
 * packager.setExpectedLabels(0, "Replace with WIDGET", "Swap order of arguments", .../* expected labels * /);
 * packager.setExpectedLabels(1, /* expected labels * /);
 * packager.setExpectedLabels(2, /* expected labels * /);
 * //...
 * doTest(packager.asList());  //will junit.fail if any components are not fully specified
 * }
 * </pre>
 * 
 * 
 * @author Kevin Lubick
 * 
 */
public class QuickFixTestPackager {

    private final List<QuickFixTestPackage> packages = new ArrayList<>();

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
                    if (o1.lineNumber != o2.lineNumber) {
                        return o1.lineNumber - o2.lineNumber;
                    }
                    if (o1.resolutionToExecute < 0) {
                        return -5; // on the same line, do ignore fixes first
                    }
                    return o1.resolutionToExecute - o2.resolutionToExecute;
                }
                return o1.expectedPattern.compareTo(o2.expectedPattern);
            }
        });
        return Collections.unmodifiableList(packages);
    }

    private void validatePackages() {
        assertNotEquals("Did you forget to add anything to the packager?", 0, packages.size());
        for (int i = 0; i < packages.size(); i++) {
            QuickFixTestPackage p = packages.get(i);
            assertNotNull("Not all labels were initialized", p.expectedLabels);
            if (p.expectedDescriptions == null) {
                p.expectedDescriptions = p.expectedLabels;
            } else {
                assertEquals("The number of descriptions shoud match the number of labels",
                        p.expectedLabels.size(), p.expectedDescriptions.size());
            }
            assertNotNull("Not all patterns were initialized", p.expectedPattern);
            assertNotEquals("Not all line numbers were initialized [" + i + "] ", -1, p.lineNumber);
        }
    }

    private void addBlankPackagesToIndex(int indexOfPackageToChange) {
        // if package does not exist, create empty packages
        while (packages.size() <= indexOfPackageToChange) {
            packages.add(new QuickFixTestPackage());
        }
    }

    /**
     * Sets the expected labels for the bug marker at the given index.
     * If the package does not exist, it will be created.
     * 
     * Since each bug marker can have more than one resolution, these might need
     * to be specified by index for resolutions that offer custom labels.
     * 
     * The order should not matter for assertion, as that is not well defined.
     * 
     * @param index
     *            the index of the package to update.
     * @param expectedLabels
     *            one or more labels to be associated
     */
    public void setExpectedLabels(int index, String... expectedLabels) { // there is no "add" options because the underlying lists are fixed sized
        addBlankPackagesToIndex(index);
        // set the labels
        packages.get(index).expectedLabels = Arrays.asList(expectedLabels);

    }

    /**
     * A convenience form of setExpectedLabels, if all labels will be the same.
     * 
     * Sets all created packages to have the one or more specified
     * expectedLabels.
     * 
     * @param expectedLabels
     */
    public void fillExpectedLabels(String... expectedLabels) { // there is no "add" options because the underlying lists are fixed sized
        for (QuickFixTestPackage p : packages) {
            // make a separate list to avoid cross contamination of modification
            p.expectedLabels = Arrays.asList(expectedLabels);
        }
    }

    /**
     * Sets the expected descriptions for the bug marker at the given index.
     * If the package does not exist, it will be created.
     * 
     * If the description is not specified, it will default to the label (which is what
     * the implementation does)
     * 
     * Since each bug marker can have more than one resolution, these might need
     * to be specified by index for resolutions that offer custom descriptions.
     * 
     * The order should match the specified labels.
     * 
     * @param indexOfPackageToChange
     * @param expectedDescriptions
     */
    public void setExpectedDescriptions(int indexOfPackageToChange, String... expectedDescriptions) {
        addBlankPackagesToIndex(indexOfPackageToChange);

        packages.get(indexOfPackageToChange).expectedDescriptions = Arrays.asList(expectedDescriptions);
    }

    /**
     * A convenience form of setExpectedDescriptions, if all descriptions will be the same.
     * 
     * Sets all created packages to have the one or more specified
     * expectedDescriptions.
     * 
     * @param expectedDescriptions
     */
    public void fillExpectedDescriptions(String... expectedDescriptions) {
        for (QuickFixTestPackage p : packages) {
            // make a separate list to avoid cross contamination of modification
            p.expectedDescriptions = Arrays.asList(expectedDescriptions);
        }
    }

    /**
     * Sets the expected line numbers, assigning the first pattern
     * to the first package, the second pattern to the second package
     * and so on.
     * 
     * If any packages do not exist, they will be created.
     * 
     * @param lineNumbers
     *            the list of line numbers to be used
     */
    public void setExpectedLines(int... lineNumbers) {
        for (int i = 0; i < lineNumbers.length; i++) {
            int lineNumber = lineNumbers[i];
            if (packages.size() <= i) {
                packages.add(new QuickFixTestPackage());
            }
            packages.get(i).lineNumber = lineNumber;
        }
    }

    /**
     * Sets the expected bugPatterns, assigning the first pattern
     * to the first package, the second pattern to the second package
     * and so on.
     * 
     * If any packages do not exist, they will be created.
     * 
     * @param expectedPatterns
     *            the list of patterns to be used
     */
    public void setExpectedBugPatterns(String... expectedPatterns) {
        for (int i = 0; i < expectedPatterns.length; i++) {
            String pattern = expectedPatterns[i];
            if (packages.size() <= i) {
                packages.add(new QuickFixTestPackage());
            }
            packages.get(i).expectedPattern = pattern;
        }
    }

    /**
     * A convenience form of setExpectedBugPatterns,
     * if all BugPatterns will be the same.
     * 
     * Sets all created packages to have the specified pattern
     * 
     * @param expectedPattern
     *            the bug pattern that all packages should get
     */
    public void fillExpectedBugPatterns(String expectedPattern) {
        for (QuickFixTestPackage p : packages) {
            p.expectedPattern = expectedPattern;
        }
    }

    /**
     * Sets the fix to implement for a given package.
     * 
     * @param indexOfPackageToChange
     * @param indexOfFixToImplement
     */
    public void setFixToPerform(int indexOfPackageToChange, int indexOfFixToImplement) {
        addBlankPackagesToIndex(indexOfPackageToChange);
        // set the fixToImplement
        packages.get(indexOfPackageToChange).resolutionToExecute = indexOfFixToImplement;

    }
}
