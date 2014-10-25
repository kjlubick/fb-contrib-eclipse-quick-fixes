import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.swt.widgets.Display;
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
    public static void loadFilesThatNeedFixing() throws CoreException, IOException {
        makeJavaProject();

        copyBrokenFiles(new File("classesToFix/"), testProject.getProject().getFolder(SRC_FOLDER_NAME));

    }
    
    private static void copyBrokenFiles(File dirOfBrokenClasses, IFolder targetFolder) throws CoreException, IOException {

        for(File fileToCopy : dirOfBrokenClasses.listFiles()) {
            
            IPath name = new Path(fileToCopy.getAbsolutePath());
            if (fileToCopy.isFile()) {
                URL url = fileToCopy.toURI().toURL();
                IFile file = targetFolder.getFile(name);
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

    @Test
    public void test() throws Exception{
        System.out.println("Hello test world");
        waitForUiEvents(20000);
    }
    
    private static void waitForUiEvents(long duration) {
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

    private static void makeJavaProject() throws CoreException {
        testProject = JavaProjectHelper.createJavaProject(PROJECT_NAME, BIN_FOLDER_NAME);
        JavaProjectHelper.addRTJar(testProject);
        JavaProjectHelper.addSourceContainer(testProject, SRC_FOLDER_NAME);
    }

}
