package tests;
import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
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

        TestingUtils.copyBrokenFiles(new File("classesToFix/"), testProject.getProject().getFolder(SRC_FOLDER_NAME));

    }
    
    private static void makeJavaProject() throws CoreException {
        testProject = JavaProjectHelper.createJavaProject(PROJECT_NAME, BIN_FOLDER_NAME);
        JavaProjectHelper.addRTJar17(testProject);
        JavaProjectHelper.addSourceContainer(testProject, SRC_FOLDER_NAME);
    }

    @Test
    public void test() throws Exception{
        System.out.println("Hello test world");
        TestingUtils.waitForUiEvents(40000);
    }

}
