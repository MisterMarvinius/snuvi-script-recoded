package me.hammerle.snuviscript.code;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileRegistry {
    private static final Map<Integer, String> fileIdToName = new HashMap<>();

    public static void registerFiles(int startingFileId, java.util.List<String> filePaths) {
        clear(); // Clear previous mappings
        for(int i = 0; i < filePaths.size(); i++) {
            String fileName = new File(filePaths.get(i)).getName();
            // Remove .snuvi extension for cleaner display
            if(fileName.endsWith(".snuvi")) {
                fileName = fileName.substring(0, fileName.length() - 6);
            }
            fileIdToName.put(startingFileId + i, fileName);
        }
    }

    public static String getFileName(int fileId) {
        return fileIdToName.getOrDefault(fileId, "unknown");
    }

    public static void clear() {
        fileIdToName.clear();
    }
}
