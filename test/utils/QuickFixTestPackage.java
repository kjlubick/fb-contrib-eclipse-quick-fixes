package utils;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value="UWF_NULL_FIELD",justification="This is a struct class.")
public class QuickFixTestPackage {

    public static final int IGNORE_FIX = -1;

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
