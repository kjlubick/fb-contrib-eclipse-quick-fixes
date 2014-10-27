package utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

public class TestingUtils {

    private TestingUtils() {
        // private constructor, static utils
    }

    public static void copyBrokenFiles(File dirOfBrokenClasses, IFolder targetFolder) throws CoreException, IOException {
    
        for(File fileToCopy : dirOfBrokenClasses.listFiles()) {
            if (fileToCopy.isFile()) {
                URL url = fileToCopy.getAbsoluteFile().toURI().toURL();
                IFile file = targetFolder.getFile(new Path(fileToCopy.getName()));
                if (!file.exists()) {
                    file.create(url.openStream(), true, null);
                } else {
                    file.setContents(url.openStream(), true, false, null);
                }
            } else {    //is directory, make a recursive call to copy
                if (!(fileToCopy.isHidden() || fileToCopy.getName().startsWith("."))) {
                    IFolder newFolder = targetFolder.getFolder(fileToCopy.getName());
                    if (newFolder.exists()) {
                        newFolder.delete(true, null);  //force deletion
                    }
                    newFolder.create(true, true, null); //force a local folder
                    
                    copyBrokenFiles(fileToCopy, newFolder);
                } else {
                    System.out.println("Skipping hidden folder "+fileToCopy);
                }
            }
        }
    }
    
    public static void sortMarkersByPatterns(IMarker[] markers) {
        Arrays.sort(markers, new Comparator<IMarker>() {

            @Override
            public int compare(IMarker marker1, IMarker marker2) {
                String pattern1 = MarkerUtil.getBugPatternString(marker1);
                String pattern2 = MarkerUtil.getBugPatternString(marker2);
                if (pattern1 != null) {
                    if (pattern1.equals(pattern2)) {
                        return MarkerUtil.findPrimaryLineForMaker(marker1) -
                                MarkerUtil.findPrimaryLineForMaker(marker2);
                    }
                    return pattern1.compareTo(pattern2);
                }
                try {
                    fail("A marker did not have a bug pattern string "+marker1.getAttributes());
                } catch (CoreException e) {
                    e.printStackTrace();
                    fail("Core exception");
                }
                return 0;
            }
        });
    }
    
    public static void waitForUiEvents(long duration) {
        long start = System.currentTimeMillis();
        long sleepTime = duration > 30? 30 : duration;
        while (true) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handleAllUiEvents();
            if(System.currentTimeMillis() - start > duration){
                break;
            }
        }
    }

    private static void handleAllUiEvents() {
        while (Display.getDefault().readAndDispatch()) {
            //do nothing, handle UI Events
        }
    }

    public static IMarker[] getAllMarkersInResource(IResource resource) {
        return MarkerUtil.getAllMarkers(resource);
    }

    public static IMarker[] getAllMarkersInResource(IJavaProject testProject, String fileName) throws JavaModelException {
        return getAllMarkersInResource(elementFromProject(testProject, fileName).getCorrespondingResource());
    }

    public static IJavaElement elementFromProject(IJavaProject project, String fileName) throws JavaModelException {
        return project.findElement(new Path(fileName));
    }

    public static void assertBugPatternsMatch(List<QuickFixTestPackage> packages, IMarker[] markers) {
        for (int i = 0; i < packages.size(); i++) {
            String actualBugpattern = MarkerUtil.getBugPatternString(markers[i]);
            assertEquals("Bug Pattern should match" , packages.get(i).expectedPattern, actualBugpattern);
        }
    }

    public static void assertPresentLabels(List<QuickFixTestPackage> packages, IMarker[] markers, BugResolutionSource resolutionSource) {
        for (int i = 0; i < packages.size(); i++) {
            IMarker marker = markers[i];
            List<String> expectedLabels = new ArrayList<>(packages.get(i).expectedLabels);
            IMarkerResolution[] resolutions = resolutionSource.getResolutions(marker);

            assertEquals("The expected number of resolutions availible was wrong", expectedLabels.size(), resolutions.length);

            for (int j = 0; j < resolutions.length; j++) {
                BugResolution resolution = (BugResolution) resolutions[j];
                String label = resolution.getLabel();
                assertTrue("Should not have seen label: "+label, expectedLabels.contains(label));
                expectedLabels.remove(label);
            }
        }
    }
    
    public static void assertLineNumbersMatch(List<QuickFixTestPackage> packages, IMarker[] markers) {
        for (int i = 0; i < packages.size(); i++) {
            int lineNumber = MarkerUtil.findPrimaryLineForMaker(markers[i]);
            assertEquals("Line number should match" , packages.get(i).lineNumber, lineNumber);
        }
    }
    
    public static void assertAllMarkersHaveResolutions(IMarker[] markers, BugResolutionSource resolutionSource) {
        for (int i = 0; i < markers.length; i++) {
            IMarker marker = markers[i];
            assertTrue("no resolution for: " + MarkerUtil.getBugPatternString(marker),
                    resolutionSource.hasResolutions(marker));
        }
    }
    
    public static void assertOutputAndInputFilesMatch(URL expectedFile, IJavaElement actualFile) throws IOException, JavaModelException {
        if (actualFile instanceof ICompilationUnit) {
            ICompilationUnit compilationUnit = (ICompilationUnit) actualFile;

            String expectedSource = readFileContents(expectedFile);
            assertEquals("Input and output should match", expectedSource, compilationUnit.getSource());
        } else {
            fail("The specified 'actual' file is not a file, but something else " + actualFile);
        }
    }

    private static String readFileContents(URL url) throws IOException {
        // Using a StringWriter here because it's about the same performance
        // as StringBuilder (http://stackoverflow.com/questions/2980805/string-assembly-by-stringbuilder-vs-stringwriter-and-printwriter)
        // and it handles the url streams better (the int to char conversion, specifically)
        StringWriter writer = new StringWriter(100);
        try (InputStream input = url.openStream();){
            int nextChar;
            while ((nextChar = input.read()) != -1) {
                writer.write(nextChar);
            }
        } 
        return writer.toString();
    }

}
