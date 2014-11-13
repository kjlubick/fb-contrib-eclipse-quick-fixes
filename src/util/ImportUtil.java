package util;

import java.util.ArrayList;

public class ImportUtil {

    public static String[] filterOutJavaLangImports(String[] addedImports) {
        
        ArrayList<String> filteredImports = new ArrayList<>();
        for(String oldImport:addedImports) {
            if (oldImport.startsWith("java.lang")) {
                continue;
            }
            filteredImports.add(oldImport);
        }
        return filteredImports.toArray(new String[filteredImports.size()]);
    }

}
