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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.ui.IMarkerResolution;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
            scanForBugs(testResource);  
            assertBugPatternsMatch(packages, testResource);   
            executeResolutions(packages, testResource); 
            assertOutputAndInputFilesMatch(testResource);
        } 
        catch (CoreException | IOException e) {
            e.printStackTrace();
            fail("Exception thrown while performing resolution on "+testResource);
        }
        
    
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

        // wait for the findBugsWorker to finish
        worker.work(Collections.singletonList(new WorkItem(element)));
        // half a second reduces the chance that the IMarkers haven't loaded yet
        // and the tests will fail unpredictably
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
        TestingUtils.assertPresentLabels(packages, markers, resolutionSource);
        TestingUtils.assertLineNumbersMatch(packages, markers);
        TestingUtils.assertAllMarkersHaveResolutions(markers, resolutionSource);
    }

    private void executeResolutions(List<QuickFixTestPackage> packages, String testResource) throws CoreException 
    {  
        for (int i = 0; i < packages.size(); i++) {
            
            if (i != 0) {   // Refresh, rebuild, and scan for bugs again
                // We only need to do this after the first time, as we expect the file to have
                // been scanned and checked for consistency (see checkBugsAndPerformResolution)
                testIProject.refreshLocal(IResource.DEPTH_ONE, null);
                testIProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
                clearMarkersAndBugs();
                scanForBugs(testResource);   
            }
            
            IMarker[] markers = getSortedMarkersFromFile(testResource);
            
            assertEquals("Bug marker number was different than anticipated "
                    + "Check to see if another bug marker was introduced by fixing another.",
                    packages.size() - i, markers.length);
            
            performResolution(packages.get(i), markers[0]);       // resolve the first bug marker
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

    private void performResolution(QuickFixTestPackage qfPackage, IMarker marker) {
        // This doesn't actually click on the bug marker, but it programmatically
        // does the same thing
        IMarkerResolution[] resolutions = resolutionSource.getResolutions(marker);
        
        assertTrue("I wanted to execute resolution #" + qfPackage.resolutionToExecute +
                " but there were only " + resolutions.length +" to choose from."
                ,resolutions.length > qfPackage.resolutionToExecute);
        //the order isn't guarenteed, so we have to check the labels.
        String resolutionToDo = qfPackage.expectedLabels.get(qfPackage.resolutionToExecute);
        for(IMarkerResolution resolution: resolutions) {
            if (resolution.getLabel().equals(resolutionToDo)) {
                resolution.run(marker);
            }
        }
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
     *            A string with the dot-seperated-class of the detector to enable/disable
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
    public void testUseCharacterParameterizedMethod() throws Exception {
        // StringToCharResolution.java
        setPriority("Medium");
        setRank(20);
        //this pops up when fixing the bug on line 31 (not the fixes fault, but the tests)
        setDetector("com.mebigfatguy.fbcontrib.detect.InefficientStringBuffering", false);
        //setDetector(dotSeperatedDetectorClass, enabled);
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
        packager.setExpectedLabels(6, "Replace with the char equivalent method call"); 
        packager.setExpectedLabels(7, "Replace with the char equivalent method call"); 
        
        packager.setFixToPerform(5,1);

        checkBugsAndPerformResolution(packager.asList(), "SingleLengthStringBugs.java");
    }

}
