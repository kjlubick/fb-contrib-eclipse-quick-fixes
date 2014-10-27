package utils;

import java.util.List;

public class QuickFixTestPackage {

    public String expectedPattern = null;

    public List<String> expectedLabels = null;

    public int lineNumber = -1;

    public int resolutionToExecute = 0; // default to first

    @Override
    public String toString() {
        return "QuickFixTestPackage [expectedPattern=" + expectedPattern + ", expectedLabels=" + expectedLabels + ", lineNumber="
                + lineNumber + ", resolutionToExecute=" + resolutionToExecute + ']';
    }

}
