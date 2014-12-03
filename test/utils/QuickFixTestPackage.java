package utils;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "UWF_NULL_FIELD", justification = "This is a struct class.")
public class QuickFixTestPackage {

    /**
     * This indicates that we should skip this marker, usually because no quickfix applies.
     */
    public static final int IGNORE_FIX = -1;

    /**
     * This indicates that the next regular quickfix invocation will also make this marker go away.
     * An example is if you have two bugs in an if statement, but they are on different lines,
     * so two markers are reported. The preferred way to handle this is to fix both problems with
     * one invocation, so the first should be flagged as handled by the invocation of the second.
     */
    public static final int FIXED_BY_ANOTHER_FIX = -2;

    public String expectedPattern = null;

    public List<String> expectedLabels = null;

    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "This is a struct class.")
    public List<String> expectedDescriptions = null;

    public int lineNumber = -1;

    public int resolutionToExecute = 0; // default to first

    @Override
    public String toString() {
        return "QuickFixTestPackage [expectedPattern=" + expectedPattern + ", expectedLabels=" + expectedLabels + ", lineNumber="
                + lineNumber + ", resolutionToExecute=" + resolutionToExecute + ']';
    }

}
