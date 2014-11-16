package tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolutionGenerator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PartInitException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import quickfix.DeadShadowStoreResolution;
import quickfix.InsecureRandomResolution;
import quickfix.ReturnValueIgnoreResolution;
import utils.BugResolutionSource;
import utils.QuickFixTestPackage;
import utils.QuickFixTestPackager;
import utils.TestingUtils;

@RunWith(JUnit4.class)
public class TestContributedQuickFixes {

    public final static String PROJECT_NAME = "fb-contrib-test-quick-fixes";

    public final static String BIN_FOLDER_NAME = "bin";

    public final static String SRC_FOLDER_NAME = "src";

    private static IJavaProject testProject;

    private static IProject testIProject;

    private BugResolutionSource resolutionSource;
    
    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            System.out.println("Failed");
          //  TestingUtils.waitForUiEvents(20_000);
        }

        @Override
        protected void succeeded(Description description) {
            System.out.println("Passed");
           }
       };

    @BeforeClass
    public static void loadFilesThatNeedFixing() throws CoreException, IOException {
        makeJavaProject();

        TestingUtils.copyBrokenFiles(new File("classesToFix/"), testIProject.getFolder(SRC_FOLDER_NAME));

        // Compiles the code
        testIProject.refreshLocal(IResource.DEPTH_INFINITE, null);
        testIProject.build(IncrementalProjectBuilder.FULL_BUILD, null);

        FindbugsPlugin.setProjectSettingsEnabled(testIProject, null, true);
        
        TestingUtils.waitForUiEvents(100);
    }

    private static void clearMarkersAndBugs() throws CoreException {
        MarkerUtil.removeMarkers(testIProject);
        FindbugsPlugin.getBugCollection(testIProject, null, false).clearBugInstances();
    }

    private static void makeJavaProject() throws CoreException {
        testProject = JavaProjectHelper.createJavaProject(PROJECT_NAME, BIN_FOLDER_NAME);
        JavaProjectHelper.addRTJar17(testProject);
        JavaProjectHelper.addSourceContainer(testProject, SRC_FOLDER_NAME);
        testProject.setOption("org.eclipse.jdt.core.formatter.tabulation.char", JavaCore.SPACE);
        testIProject = testProject.getProject();
    }

    @Before
    public void setup() {
        final BugResolutionGenerator resolutionGenerator = new BugResolutionGenerator();
        // we wrap this in case the underlying generator interface changes.
        resolutionSource = new BugResolutionSource() {
            @Override
            public IMarkerResolution[] getResolutions(IMarker marker) {
                return resolutionGenerator.getResolutions(marker);
            }

            @Override
            public boolean hasResolutions(IMarker marker) {
                return resolutionGenerator.hasResolutions(marker);
            }
        };
    }

    private void checkBugsAndPerformResolution(List<QuickFixTestPackage> packages, String testResource) {
        try {
            showEditorWindowForFile(testResource);
            scanForBugs(testResource);
            assertBugPatternsMatch(packages, testResource);
            executeResolutions(packages, testResource);
            assertOutputAndInputFilesMatch(testResource);
        } catch (CoreException | IOException e) {
            e.printStackTrace();
            fail("Exception thrown while performing resolution on " + testResource);
        }

    }

    private void showEditorWindowForFile(String testResource) throws JavaModelException, PartInitException {
        JavaUI.openInEditor(TestingUtils.elementFromProject(testProject, testResource), true, true);
    }

    /**
     * Blocks until FindBugs has finished scanning
     * 
     * @param className
     *            file in this project to scan
     * @throws CoreException
     */
    private void scanForBugs(String className) throws CoreException {
        IJavaElement element = testProject.findElement(new Path(className));
        if (element == null)
        {
            fail("Could not find java class " + className);
            return;
        }
        final AtomicBoolean isWorking = new AtomicBoolean(true);
        FindBugsWorker worker = new FindBugsWorker(testProject.getProject(), new NullProgressMonitor() {
            @Override
            public void done() {
                isWorking.set(false);
            }
        });

        worker.work(Collections.singletonList(new WorkItem(element)));
        // wait for the findBugsWorker to finish
        // 500ms reduces the chance that the IMarkers haven't loaded yet and the tests will fail unpredictably
        // (see JavaProjectHelper discussion about performDummySearch for more info)
        TestingUtils.waitForUiEvents(500);
        while (isWorking.get()) {
            TestingUtils.waitForUiEvents(100);
        }
    }

    private void assertBugPatternsMatch(List<QuickFixTestPackage> packages, String testResource) throws JavaModelException {
        IMarker[] markers = TestingUtils.getAllMarkersInResource(testProject, testResource);
        TestingUtils.sortMarkersByPatterns(markers);

        // packages and markers should now be lined up to match up one to one.
        assertEquals(packages.size(), markers.length);

        TestingUtils.assertBugPatternsMatch(packages, markers);
        TestingUtils.assertLabelsAndDescriptionsMatch(packages, markers, resolutionSource);
        TestingUtils.assertLineNumbersMatch(packages, markers);
        TestingUtils.assertAllMarkersHaveResolutions(markers, resolutionSource);
    }

    private void executeResolutions(List<QuickFixTestPackage> packages, String testResource) throws CoreException
    {
        // Some resolutions can be ignored. One example is a detector that has one or more sometimes
        // applicable quickfixes, but occasionally none apply. These are useful to include in the
        // test cases (e.g. DeadLocalStoreBugs.java), and need to be properly handled.
        // we keep a count of the ignored resolutions (QuickFixTestPackage.IGNORE_FIX) and correct
        // our progress using that
        int ignoredResolutions = 0;
        int pendingBogoFixes = 0;
        boolean skipNextScan = true;
        for (int resolutionsCompleted = 0; resolutionsCompleted < packages.size(); resolutionsCompleted++) {

            if (skipNextScan) { // Refresh, rebuild, and scan for bugs again
                // We only need to do this after the first time, as we expect the file to have
                // been scanned and checked for consistency (see checkBugsAndPerformResolution)
                testIProject.refreshLocal(IResource.DEPTH_ONE, null);
                testIProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
                clearMarkersAndBugs();
                scanForBugs(testResource);
            }
            skipNextScan = false;
            
            System.out.println(resolutionsCompleted);

            IMarker[] markers = getSortedMarkersFromFile(testResource);

            assertEquals("Bug marker number was different than anticipated.  "
                    + "Check to see if another bug marker was introduced by fixing another.",
                    packages.size() - (resolutionsCompleted),
                    markers.length - ignoredResolutions - pendingBogoFixes);

            IMarker nextNonIgnoredMarker = markers[ignoredResolutions + pendingBogoFixes]; // Bug markers we ignore float to the "top" of the stack
                                                                        // ignoredResolutions can act as an index for that
            
            QuickFixTestPackage p = packages.get(resolutionsCompleted);
            performResolution(p, nextNonIgnoredMarker);
            if (p.resolutionToExecute == QuickFixTestPackage.IGNORE_FIX) {
                ignoredResolutions++;
                skipNextScan = true;
            } else if (p.resolutionToExecute == QuickFixTestPackage.FIXED_BY_ANOTHER_FIX) {
                pendingBogoFixes++;
                skipNextScan = true;
            }
            else {
                pendingBogoFixes = 0;
            }
        }
    }

    private IMarker[] getSortedMarkersFromFile(String fileName) throws JavaModelException {
        IJavaElement fileToScan = TestingUtils.elementFromProject(testProject, fileName);
        IMarker[] markers = TestingUtils.getAllMarkersInResource(fileToScan.getCorrespondingResource());
        // the markers get sorted first by bug name, then by line number, just like
        // the QuickFixTestPackages, so we have a reliable way to fix them
        // so we can assert properties more accurately.
        TestingUtils.sortMarkersByPatterns(markers);
        return markers;
    }

    private boolean performResolution(QuickFixTestPackage qfPackage, IMarker marker) {
        if (qfPackage.resolutionToExecute < 0) {
            return false; // false means the marker should be ignored.
        }
        // This doesn't actually click on the bug marker, but it programmatically
        // gets the same resolutions.
        IMarkerResolution[] resolutions = resolutionSource.getResolutions(marker);
        assertTrue("I wanted to execute resolution #" + qfPackage.resolutionToExecute + " of " + qfPackage +
                " but there were only " + resolutions.length + " to choose from."
                , resolutions.length > qfPackage.resolutionToExecute);

        // the order isn't guaranteed, so we have to check the labels.
        @SuppressFBWarnings("NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
        String resolutionToDo = qfPackage.expectedLabels.get(qfPackage.resolutionToExecute);
        for (IMarkerResolution resolution : resolutions) {
            if (resolution.getLabel().equals(resolutionToDo)) {
                resolution.run(marker);
            }
        }
        return true; // a resolution was performed and we expect the bug marker to disappear.
    }

    private void assertOutputAndInputFilesMatch(String testResource) throws JavaModelException, IOException {
        File expectedFile = new File("fixedClasses", testResource);
        IJavaElement actualFile = TestingUtils.elementFromProject(testProject, testResource);
        TestingUtils.assertOutputAndInputFilesMatch(expectedFile.toURI().toURL(), actualFile);
    }

    /**
     * Enables or disables a detector. This can only disable an entire detector (class-level),
     * not just one bug pattern.
     * 
     * @param dotSeperatedDetectorClass
     *            A string with the dot-separated-class of the detector to enable/disable
     * @param enabled
     */
    private void setDetector(String dotSeperatedDetectorClass, boolean enabled) {
        DetectorFactory factory = DetectorFactoryCollection.instance().getFactoryByClassName(dotSeperatedDetectorClass);
        FindbugsPlugin.getUserPreferences(testIProject).enableDetector(factory, enabled);
    }

    /**
     * Set minimum warning priority threshold to be used for scanning
     * 
     * Project defaults to "Medium"
     * 
     * @param minPriority
     *            the priority threshold: one of "High", "Medium", or "Low"
     */
    private void setPriority(String minPriority) {
        FindbugsPlugin.getProjectPreferences(testIProject, false).getFilterSettings().setMinPriority(minPriority);
    }

    /**
     * Set minimum bugRank to show up for scanning
     * 
     * Project defaults to 15
     * 
     * @param minPriority
     *            the priority threshold: one of "High", "Medium", or "Low"
     */
    private void setRank(int minRank) {
        FindbugsPlugin.getProjectPreferences(testIProject, false).getFilterSettings().setMinRank(minRank);
    }

    @Test
    public void testCharsetIssuesResolution() throws Exception {
        // CharsetIssuesResolution.java
        setPriority("Medium");
        setRank(15);
        // disables NP_NULL_PARAM_DEREF_NONVIRTUAL which happens because the rtstubs17.jar
        // defines the constants as null
        setDetector("edu.umd.cs.findbugs.detect.FindNullDeref", false);

        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.setExpectedLines(16, 23, 25, 30, 32, 34, // CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET
                40, 44, 48, 52, 57, 61); // CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME

        packager.setExpectedBugPatterns("CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET",
                "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET",
                "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET",
                "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME",
                "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME",
                "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME");

        packager.setExpectedLabels(0, "Replace with StandardCharset.UTF_8");
        packager.setExpectedLabels(1, "Replace with StandardCharset.ISO_8859_1");
        packager.setExpectedLabels(2, "Replace with StandardCharset.US_ASCII");
        packager.setExpectedLabels(3, "Replace with StandardCharset.UTF_16");
        packager.setExpectedLabels(4, "Replace with StandardCharset.UTF_16LE");
        packager.setExpectedLabels(5, "Replace with StandardCharset.UTF_16BE");

        packager.setExpectedLabels(6, "Replace with StandardCharset.UTF_8.name()");
        packager.setExpectedLabels(7, "Replace with StandardCharset.UTF_16.name()");
        packager.setExpectedLabels(8, "Replace with StandardCharset.UTF_16LE.name()");
        packager.setExpectedLabels(9, "Replace with StandardCharset.UTF_16BE.name()");
        packager.setExpectedLabels(10, "Replace with StandardCharset.US_ASCII.name()");
        packager.setExpectedLabels(11, "Replace with StandardCharset.ISO_8859_1.name()");

        checkBugsAndPerformResolution(packager.asList(), "CharsetIssuesBugs.java");
    }

    @Test
    public void testUseCharacterParameterizedMethodResolution() throws Exception {
        // StringToCharResolution.java
        setPriority("Medium");
        setRank(20);
        // this pops up when fixing the bug on line 31 (not the fixes fault, but the tests)
        setDetector("com.mebigfatguy.fbcontrib.detect.InefficientStringBuffering", false);
        QuickFixTestPackager packager = new QuickFixTestPackager();

        packager.setExpectedLines(8, 13, 19, 23, 27, 31, 35, 39);
        packager.fillExpectedBugPatterns("UCPM_USE_CHARACTER_PARAMETERIZED_METHOD");

        packager.setExpectedLabels(0, "Replace with the char equivalent method call",
                "Use StringBuilder for String concatenation");
        packager.setExpectedLabels(1, "Replace with the char equivalent method call",
                "Use StringBuilder for String concatenation");
        packager.setExpectedLabels(2, "Use StringBuilder for String concatenation"); // char equivalent won't work
        packager.setExpectedLabels(3, "Use StringBuilder for String concatenation"); // char equivalent won't work
        packager.setExpectedLabels(4, "Replace with the char equivalent method call",
                "Use StringBuilder for String concatenation");
        packager.setExpectedLabels(5, "Replace with the char equivalent method call",
                "Use StringBuilder for String concatenation");
        packager.setExpectedLabels(6, "Replace with the char equivalent method call"); // not a concatenation
        packager.setExpectedLabels(7, "Replace with the char equivalent method call"); // not a concatenation

        packager.setFixToPerform(5, 1);

        checkBugsAndPerformResolution(packager.asList(), "SingleLengthStringBugs.java");
    }

    @Test
    public void testEntrySetResolution() throws Exception {
        // EntrySetResolution.java
        setPriority("Medium");
        setRank(18);

        QuickFixTestPackager packager = new QuickFixTestPackager();

        packager.setExpectedLines(10, 16, 23, 30);
        packager.fillExpectedBugPatterns("WMI_WRONG_MAP_ITERATOR");

        packager.fillExpectedLabels("Replace with a foreach loop using entrySet()");
        
        packager.setExpectedDescriptions(0, "for(Map.Entry&lt;String,Integer&gt; entry : map.entrySet()) {<br/>String key = entry.getKey();<br/>Integer tempVar = entry.getValue();<br/>...<br/>}");
        packager.setExpectedDescriptions(1, "for(Map.Entry&lt;String,Integer&gt; entry : map.entrySet()) {<br/>String key = entry.getKey();<br/>Integer i = entry.getValue();<br/>...<br/>}");
        packager.setExpectedDescriptions(2, "for(Map.Entry&lt;String,List<Integer>&gt; entry : map.entrySet()) {<br/>String key = entry.getKey();<br/>List<Integer> someVal = entry.getValue();<br/>...<br/>}");
        packager.setExpectedDescriptions(3, "for(Map.Entry&lt;String,Set<String>&gt; entry : map.entrySet()) {<br/>String key = entry.getKey();<br/>Set<String> tempVar = entry.getValue();<br/>...<br/>}");
        

        checkBugsAndPerformResolution(packager.asList(), "WrongMapIteratorBugs.java");
        
    }

    @Test
    public void testDeadShadowStoreResolution() throws Exception {
        // DeadShadowStoreResolution.java
        setPriority("Medium");
        setRank(15);
        
        setDetector("edu.umd.cs.findbugs.detect.FindDeadLocalStores", true);

        QuickFixTestPackager packager = new QuickFixTestPackager();

        packager.setExpectedLines(12, 34, 39, 42);
        packager.setExpectedBugPatterns("DLS_DEAD_LOCAL_STORE_SHADOWS_FIELD", "DLS_DEAD_LOCAL_STORE_SHADOWS_FIELD",
                "DLS_DEAD_LOCAL_STORE", "DLS_DEAD_LOCAL_STORE");
        packager.setExpectedLabels(0, "Prefix assignment to store to field");
        packager.setExpectedLabels(1, "Prefix assignment to store to field");
        packager.setExpectedLabels(2, "Prefix assignment like DeadLocalStoreBugs.this.className");
        packager.setExpectedLabels(3); // no resolutions, it doesn't apply
        packager.fillExpectedDescriptions(DeadShadowStoreResolution.DSS_DESC);
        packager.setExpectedDescriptions(3); //no descriptions either
        
        packager.setFixToPerform(3, QuickFixTestPackage.IGNORE_FIX);
        checkBugsAndPerformResolution(packager.asList(), "DeadLocalStoreBugs.java");
    }
    
    
    @Test
    public void testLiteralStringComparisonResolution() throws Exception {
        // LiteralStringComparisonResolution.java
        setPriority("Medium");
        setRank(10);

        QuickFixTestPackager packager = new QuickFixTestPackager();

        packager.setExpectedLines(6, 12, 13, 15);
        packager.fillExpectedBugPatterns("LSC_LITERAL_STRING_COMPARISON");
        packager.fillExpectedLabels("Swap string variable and string literal");
        
        packager.setFixToPerform(1, QuickFixTestPackage.FIXED_BY_ANOTHER_FIX); //the last fix will fix all three problems
        packager.setFixToPerform(2, QuickFixTestPackage.FIXED_BY_ANOTHER_FIX); // I ignore 1 and 2 because that integrates with the framework

        checkBugsAndPerformResolution(packager.asList(), "LiteralStringComparisonBugs.java");
    }

    @Test
    public void testInsecureRandomResolution() throws Exception {
        // InsecureRandomResolution.java
        setPriority("Low");
        setRank(20);

        QuickFixTestPackager packager = new QuickFixTestPackager();

        packager.setExpectedLines(6, 8);
        packager.fillExpectedBugPatterns("MDM_RANDOM_SEED");
        packager.fillExpectedLabels("Initialize with seed from SecureRandom", "Replace using a SecureRandom object");
        packager.fillExpectedDescriptions(InsecureRandomResolution.GENERATE_SEED_DESC,
                InsecureRandomResolution.SECURE_RENAME_DESC);
        packager.setFixToPerform(0, 0);
        packager.setFixToPerform(1, 1);

        checkBugsAndPerformResolution(packager.asList(), "InsecureRandomBugs.java");
    }
    
    @Test
    public void testNeedlessBoxingResolution() throws Exception {
        // NeedlessBoxingResolution.java
        setPriority("Low");
        setRank(20);
        setDetector("edu.umd.cs.findbugs.detect.UnreadFields", false);
        setDetector("edu.umd.cs.findbugs.detect.DumbMethods", false);  //some overlap with the parse
        setDetector("com.mebigfatguy.fbcontrib.detect.FinalParameters", false);
        // disables NP_NULL_PARAM_DEREF_NONVIRTUAL which happens because the rtstubs17.jar
        // defines the constants (like Boolean.False) as null
        setDetector("edu.umd.cs.findbugs.detect.FindNullDeref", false);
        setDetector("edu.umd.cs.findbugs.detect.LoadOfKnownNullValue", false);

        QuickFixTestPackager packager = new QuickFixTestPackager();

        packager.setExpectedLines(10, 18, 19, 20, 21, 22, 23, 24, 30, 31, 32, 33, 39);

        packager.setExpectedBugPatterns("NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION", 
                "NAB_NEEDLESS_BOXING_PARSE", "NAB_NEEDLESS_BOXING_PARSE", "NAB_NEEDLESS_BOXING_PARSE", "NAB_NEEDLESS_BOXING_PARSE",
                "NAB_NEEDLESS_BOXING_PARSE", "NAB_NEEDLESS_BOXING_PARSE", "NAB_NEEDLESS_BOXING_PARSE",
                "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION", "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION",
                "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION", "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION",
                "NAB_NEEDLESS_BOXING_PARSE");
        
        packager.setExpectedLabels(0, "Replace with Boolean.TRUE");
        packager.setExpectedLabels(1, "Replace with Boolean.parseBoolean(data)");
        packager.setExpectedLabels(2, "Replace with Byte.parseByte(data)");
        packager.setExpectedLabels(3, "Replace with Short.parseShort(data)");
        packager.setExpectedLabels(4, "Replace with Integer.parseInt(data)");
        packager.setExpectedLabels(5, "Replace with Long.parseLong(data)");
        packager.setExpectedLabels(6, "Replace with Float.parseFloat(data)");
        packager.setExpectedLabels(7, "Replace with Double.parseDouble(data)");
        packager.setExpectedLabels(8, "Replace with false");
        packager.setExpectedLabels(9, "Replace with true");
        packager.setExpectedLabels(10, "Replace with Boolean.FALSE");
        packager.setExpectedLabels(11, "Replace with Boolean.TRUE");
        packager.setExpectedLabels(12, "Replace with Integer.parseInt(num)");
        
        checkBugsAndPerformResolution(packager.asList(), "NeedlessBoxingBugs.java");
    }
    
    @Test
    public void testBigDecimalConstructorResolution() throws Exception {
        // BigDecimalConstructorResolution.java
        setDetector("com.mebigfatguy.fbcontrib.detect.SillynessPotPourri", false);    //these have duplicate bugs
        setRank(10);
        setPriority("Medium");
        
        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.setExpectedLines(7, 11);
        
        packager.fillExpectedBugPatterns("DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE");
        
        packager.setExpectedLabels(0, "Replace with BigDecimal.valueOf(1.23456)",
                "Replace with new BigDecimal(\"1.23456\")");
        packager.setExpectedLabels(1, "Replace with BigDecimal.valueOf(1.234567)",
                "Replace with new BigDecimal(\"1.234567\")");
        
        packager.setFixToPerform(0, 0);
        packager.setFixToPerform(1, 1);
        
        checkBugsAndPerformResolution(packager.asList(), "BigDecimalStringBugs.java");
    }
    
    @Test
    public void testReturnValueIgnoreResolution() throws Exception {
        // ReturnValueIgnoreResolution
        
        setRank(19);
        setPriority("Low");
        
        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.setExpectedLines(12, 17, 25, 30);
        
        packager.setExpectedBugPatterns("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
                "RV_RETURN_VALUE_IGNORED", "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE");
        
        String ifString = ReturnValueIgnoreResolution.descriptionForWrapIf.replace("YYY","file.createNewFile()");
        String ifNotString = ReturnValueIgnoreResolution.descriptionForNegatedWrapIf
                .replace("YYY","file.createNewFile()");
        
        packager.setExpectedLabels(0, "Replace with if (file.createNewFile()) {}",
                "Replace with if (!file.createNewFile()) {}", "Store result to a local");
        packager.setExpectedDescriptions(0, ifString, ifNotString, ReturnValueIgnoreResolution.descriptionForNewLocal);
        
        ifString = ReturnValueIgnoreResolution.descriptionForWrapIf.replace("YYY","file.delete()");
        ifNotString = ReturnValueIgnoreResolution.descriptionForNegatedWrapIf
                .replace("YYY","file.delete()");
        
        packager.setExpectedLabels(1, "Replace with if (file.delete()) {}",
                "Replace with if (!file.delete()) {}", "Store result to a local");
        packager.setExpectedDescriptions(1, ifString, ifNotString, ReturnValueIgnoreResolution.descriptionForNewLocal);
        
        
        packager.setExpectedLabels(2, "Store result to a local", "Store result back to self");
        packager.setExpectedDescriptions(2, ReturnValueIgnoreResolution.descriptionForNewLocal,
                ReturnValueIgnoreResolution.descriptionForStoreToSelf);
        packager.setExpectedLabels(3, "Store result to a local");
        packager.setExpectedDescriptions(3, ReturnValueIgnoreResolution.descriptionForNewLocal);
        
        packager.setFixToPerform(0, 1);
        packager.setFixToPerform(1, 0);
        packager.setFixToPerform(2, 1);
        
        checkBugsAndPerformResolution(packager.asList(), "ReturnValueIgnoredBugs.java");
    }
    
    @Test
    public void testArraysToStringResolution() throws Exception {
        // ArraysToStringResolution.java
        setRank(10);
        setPriority("Medium");
        
        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.setExpectedLines(7, 11, 11, 18);
        
        packager.fillExpectedBugPatterns("DMI_INVOKING_TOSTRING_ON_ARRAY");
        packager.fillExpectedLabels("Wrap array with Arrays.toString()");
        
        packager.setFixToPerform(1, QuickFixTestPackage.FIXED_BY_ANOTHER_FIX); //we'll have a 2 for one fix on line 20
        
        checkBugsAndPerformResolution(packager.asList(), "ArraysToStringBugs.java");
    }
    
    
}
