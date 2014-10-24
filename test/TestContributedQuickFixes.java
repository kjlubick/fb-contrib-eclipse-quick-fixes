import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestContributedQuickFixes {
    
    public final static String PROJECT_NAME = "fb-contrib-test-quick-fixes";
    public final static String BIN_FOLDER_NAME = "bin";
    public final static String SRC_FOLDER_NAME = "src";
    private static IJavaProject testProject;
    
    @BeforeClass
    public static void loadFilesThatNeedFixing() throws CoreException {
        makeJavaProject();
        
        
    }
    
    @Test
    public void test() {
        System.out.println("Hello test world");
    }

    private static void makeJavaProject() throws CoreException {
        testProject = JavaProjectHelper.createJavaProject(PROJECT_NAME, BIN_FOLDER_NAME);
        JavaProjectHelper.addRTJar(testProject);
        JavaProjectHelper.addSourceContainer(testProject, SRC_FOLDER_NAME);
    }

}
