package utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;

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
    
    
    
}
