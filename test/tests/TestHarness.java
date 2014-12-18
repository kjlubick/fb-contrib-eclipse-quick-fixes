package tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.BugCode;
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

import utils.BugResolutionSource;
import utils.QuickFixTestPackage;
import utils.TestingUtils;

public abstract class TestHarness {

    public static final String PROJECT_NAME = "fb-contrib-test-quick-fixes";

    public static final String BIN_FOLDER_NAME = "bin";

    public static final String SRC_FOLDER_NAME = "src";

    private static IJavaProject testProject;

    private static IProject testIProject;

    private BugResolutionSource resolutionSource;

    private Set<String> detectorsToReenable = new HashSet<String>();

    public static void loadFilesThatNeedFixing() throws CoreException, IOException {
        makeJavaProject();

        TestingUtils.copyBrokenFiles(new File("classesToFix/"), testIProject.getFolder(SRC_FOLDER_NAME));

        // Compiles the code
        testIProject.refreshLocal(IResource.DEPTH_INFINITE, null);
        testIProject.build(IncrementalProjectBuilder.FULL_BUILD, null);

        FindbugsPlugin.setProjectSettingsEnabled(testIProject, null, true);

        checkFBContribInstalled();

        TestingUtils.waitForUiEvents(100);
    }

    private static void checkFBContribInstalled() {
        // this was the first fb-contrib bug code
        BugCode knownFBContribBugCode = new BugCode("ISB", "Inefficient String Buffering");
        assertTrue(FindbugsPlugin.getKnownPatternTypes().contains(knownFBContribBugCode));
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

    public void setup() {
        detectorsToReenable.clear();
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

    protected void checkBugsAndPerformResolution(List<QuickFixTestPackage> packages, String testResource) {
        try {
            showEditorWindowForFile(testResource);
            scanUntilMarkers(testResource);
            assertBugPatternsMatch(packages, testResource);
            executeResolutions(packages, testResource);
            assertOutputAndInputFilesMatch(testResource);
        } catch (CoreException | IOException e) {
            e.printStackTrace();
            fail("Exception thrown while performing resolution on " + testResource);
        }

    }

    private void scanUntilMarkers(String testResource) throws CoreException {
        scanForBugs(testResource);
        for (int i = 0; i < 3; i++) {
            IMarker[] markers = TestingUtils.getAllMarkersInResource(testProject, testResource);
            if (markers.length > 0) {
                return;
            }
            System.out.println("Trying again for bugs...");
            clearMarkersAndBugs();
            scanForBugs(testResource);
            TestingUtils.waitForUiEvents(1000);
        }
        fail("Did not find any markers... tried 3 times");

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
        TestingUtils.waitForUiEvents(800);
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

    private void executeResolutions(List<QuickFixTestPackage> packages, String testResource) throws CoreException {
        // Some resolutions can be ignored. One example is a detector that has one or more sometimes
        // applicable quickfixes, but occasionally none apply. These are useful to include in the
        // test cases (e.g. DeadLocalStoreBugs.java), and need to be properly handled.
        // we keep a count of the ignored resolutions (QuickFixTestPackage.IGNORE_FIX) and correct
        // our progress using that
        int ignoredResolutions = 0;
        int pendingBogoFixes = 0;
        boolean skipNextScan = true;
        for (int resolutionsCompleted = 0; resolutionsCompleted < packages.size(); resolutionsCompleted++) {

            if (!skipNextScan) { // Refresh, rebuild, and scan for bugs again
                // We only need to do this after the first time, as we expect the file to have
                // been scanned and checked for consistency (see checkBugsAndPerformResolution)
                testIProject.refreshLocal(IResource.DEPTH_ONE, null);
                testIProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
                clearMarkersAndBugs();
                scanUntilMarkers(testResource);
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
            skipNextScan = !performResolution(p, nextNonIgnoredMarker);
            if (p.resolutionToExecute == QuickFixTestPackage.IGNORE_FIX) {
                ignoredResolutions++;
            } else if (p.resolutionToExecute == QuickFixTestPackage.FIXED_BY_ANOTHER_FIX) {
                pendingBogoFixes++;
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
    protected void setDetector(String dotSeperatedDetectorClass, boolean enabled) {
        DetectorFactory factory = DetectorFactoryCollection.instance().getFactoryByClassName(dotSeperatedDetectorClass);
        if (factory == null) {
            fail("Could not find a detector with class " + dotSeperatedDetectorClass);
        }
        if (!enabled) {
            detectorsToReenable.add(dotSeperatedDetectorClass);
        }
        FindbugsPlugin.getUserPreferences(testIProject).enableDetector(factory, enabled);
    }

    private void restoreDisabledDetectors() {
        for (String detector : detectorsToReenable) {
            setDetector(detector, true);
        }
    }

    /**
     * Set minimum warning priority threshold to be used for scanning
     * 
     * Project defaults to "Medium"
     * 
     * @param minPriority
     *            the priority threshold: one of "High", "Medium", or "Low"
     */
    protected void setPriority(String minPriority) {
        // short hand for seeing if minPriority is one of High, Medium or Low
        // Each of the three valid strings are padded to be 6 chars long with spaces
        // if minPriority is one of the valid strings, the index will be 0, 6 or 12
        // it's about 40% slower than either doing a set or an explicit 3- case, but takes
        // less than 1ms for 1000 iterations (in any event)
        if ("High  MediumLow   ".indexOf(minPriority.trim()) % 6 == 0) {
            FindbugsPlugin.getProjectPreferences(testIProject, false).getFilterSettings().setMinPriority(minPriority);
            return;
        }
        fail("minPriority [" + minPriority + "] must be one of \"High\", \"Medium\", or \"Low\"");
    }

    /**
     * Set minimum bugRank to show up for scanning
     * 
     * Project defaults to 15
     * 
     * @param minPriority
     *            the priority threshold: one of "High", "Medium", or "Low"
     */
    protected void setRank(int minRank) {
        if (minRank >= 1 && minRank <= 20) {
            FindbugsPlugin.getProjectPreferences(testIProject, false).getFilterSettings().setMinRank(minRank);
            return;
        }
        fail("minRank [" + minRank + "] must be between 1 and 20 inclusively");
    }

    public void tearDown() {
        restoreDisabledDetectors();

    }

}
