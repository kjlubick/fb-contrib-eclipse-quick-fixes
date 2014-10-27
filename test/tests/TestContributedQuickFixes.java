package tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.WorkItem;

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

    @BeforeClass
    public static void loadFilesThatNeedFixing() throws CoreException, IOException {
        makeJavaProject();

        IProject testIProject = testProject.getProject();
        TestingUtils.copyBrokenFiles(new File("classesToFix/"), testIProject.getFolder(SRC_FOLDER_NAME));

        // Compiles the code
        testIProject.refreshLocal(IResource.DEPTH_INFINITE, null);
        testIProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
    }

    private BugResolutionGenerator resolutionGenerator;

    private BugResolutionSource resolutionSource;

    @Before
    public void setup() {
        resolutionGenerator = new BugResolutionGenerator();
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

    private static void makeJavaProject() throws CoreException {
        testProject = JavaProjectHelper.createJavaProject(PROJECT_NAME, BIN_FOLDER_NAME);
        JavaProjectHelper.addRTJar17(testProject);
        JavaProjectHelper.addSourceContainer(testProject, SRC_FOLDER_NAME);
    }

    public void scanForBugs(String className) throws CoreException {
        final AtomicBoolean isWorking = new AtomicBoolean(true);
        FindBugsWorker worker = new FindBugsWorker(testProject.getProject(), new NullProgressMonitor() {
            @Override
            public void done() {
                isWorking.set(false);
            }
        });

        IJavaElement element = testProject.findElement(new Path(className));

        if (element != null) {
            // wait for the findBugsWorker to finish
            worker.work(Collections.singletonList(new WorkItem(element)));
            // half a second reduces the chance that the IMarkers haven't loaded yet
            // (see JavaProjectHelper discussion about performDummySearch for more info
            TestingUtils.waitForUiEvents(500);
            while (isWorking.get()) {
                TestingUtils.waitForUiEvents(100);
            }
        } else {
            fail("Could not find java class " + className);
        }

    }

    @Test
    public void testCharsetIssuesResolution() throws Exception {
        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.addExpectedLines(16, 23, 25, 30, 32, 34, // CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET
                40, 44, 48, 52, 57, 61); // CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET_NAME

        packager.addBugPatterns("CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET", "CSI_CHAR_SET_ISSUES_USE_STANDARD_CHARSET",
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

    private void checkBugsAndPerformResolution(List<QuickFixTestPackage> packages, String testResource) throws CoreException,
            JavaModelException, IOException, MalformedURLException {
        scanForBugs(testResource);

        IMarker[] markers = TestingUtils.getAllMarkersInResource(testProject, testResource);
        TestingUtils.sortMarkersByPatterns(markers);

        // packages and markers should now be lined up to match up one to one.
        assertEquals(packages.size(), markers.length);

        TestingUtils.assertBugPatternsMatch(packages, markers);
        TestingUtils.assertPresentLabels(packages, markers, resolutionSource);
        TestingUtils.assertLineNumbersMatch(packages, markers);
        TestingUtils.assertAllMarkersHaveResolutions(markers, resolutionSource);

        executeResolutions(packages, markers);

        File expectedFile = new File("fixedClasses", testResource);

        TestingUtils.assertOutputAndInputFilesMatch(expectedFile.toURI().toURL(),
                TestingUtils.elementFromProject(testProject, testResource));
    }

    private void executeResolutions(List<QuickFixTestPackage> packages, IMarker[] markers) {
        for (int i = 0; i < markers.length; i++) {
            QuickFixTestPackage qfPackage = packages.get(i);
            IMarker marker = markers[i];
            IMarkerResolution[] resolutions = resolutionSource.getResolutions(marker);
            assertTrue(resolutions.length > qfPackage.resolutionToExecute);
            resolutions[qfPackage.resolutionToExecute].run(marker);
        }
    }

}
