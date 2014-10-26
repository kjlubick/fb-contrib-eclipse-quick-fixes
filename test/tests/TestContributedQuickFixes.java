package tests;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.view.explorer.ResourceChangeListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
    
    private static void makeJavaProject() throws CoreException {
        testProject = JavaProjectHelper.createJavaProject(PROJECT_NAME, BIN_FOLDER_NAME);
        JavaProjectHelper.addRTJar17(testProject);
        JavaProjectHelper.addSourceContainer(testProject, SRC_FOLDER_NAME);
    }
    
    
    public void scanForBugs(String className) throws CoreException {
        final AtomicBoolean isWorking = new AtomicBoolean(true);
        FindBugsWorker worker = new FindBugsWorker(testProject.getProject(), new NullProgressMonitor(){
            @Override
            public void done() {
                System.out.println("got done message");
                isWorking.set(false);
            }
        });
        
        IJavaElement element = testProject.findElement(new Path(className));
    
        if (element != null) {
            worker.work(Collections.singletonList(new WorkItem(element)));
            TestingUtils.waitForUiEvents(500);
            while (isWorking.get()) {
                TestingUtils.waitForUiEvents(100);
            }
        } else {
            fail("Could not find java class "+className);
        }
        
    }

    @Test
    public void test() throws Exception {
        System.out.println("Beginning scan");
        scanForBugs("CharsetIssuesBugs.java");
        System.out.println("Scan completed");
        //TestingUtils.waitForUiEvents(40000);
        assertTrue(true);
    }

}
