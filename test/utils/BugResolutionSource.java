package utils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

public interface BugResolutionSource {

    IMarkerResolution[] getResolutions(IMarker marker);

    boolean hasResolutions(IMarker marker);
    
}