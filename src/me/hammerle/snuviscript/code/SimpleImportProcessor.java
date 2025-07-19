package me.hammerle.snuviscript.code;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class SimpleImportProcessor {
    private final Set<String> processedFiles = new HashSet<>();
    private final List<String> fileOrder = new ArrayList<>();

    public SimpleImportProcessor(String mainScriptPath) {
        // Constructor for future extensibility
    }

    public List<String> processImportsToFileList(String filePath) {
        String absolutePath = new File(filePath).getAbsolutePath();
        List<String> fileContents = new ArrayList<>();
        processImportsRecursive(absolutePath, fileContents);
        return fileContents;
    }

    public List<String> getFileOrder() {
        return new ArrayList<>(fileOrder);
    }

    private void processImportsRecursive(String absolutePath, List<String> fileContents) {
        if(processedFiles.contains(absolutePath)) {
            return;
        }

        processedFiles.add(absolutePath);

        String sourceCode = readFile(absolutePath);
        String[] lines = sourceCode.split("\\r?\\n");

        // First pass: process all imports
        for(String line : lines) {
            String trimmedLine = line.trim();
            if(trimmedLine.startsWith("import ")) {
                String importPath = extractImportPath(trimmedLine);
                if(importPath != null) {
                    String resolvedPath = resolveImportPath(importPath, absolutePath);
                    processImportsRecursive(resolvedPath, fileContents);
                }
            }
        }

        // Second pass: build the content without imports, preserving line numbers
        StringBuilder result = new StringBuilder();

        for(String line : lines) {
            String trimmedLine = line.trim();
            if(trimmedLine.startsWith("import ")) {
                // Add empty line to preserve line numbers
                result.append("\n");
            } else {
                result.append(line).append("\n");
            }
        }

        fileOrder.add(absolutePath);
        fileContents.add(result.toString());
    }

    private String extractImportPath(String importLine) {
        int firstQuote = importLine.indexOf('"');
        int lastQuote = importLine.lastIndexOf('"');

        if(firstQuote != -1 && lastQuote != -1 && firstQuote < lastQuote) {
            return importLine.substring(firstQuote + 1, lastQuote);
        }

        throw new PreScriptException("Invalid import syntax: " + importLine, -1);
    }

    private String resolveImportPath(String importPath, String currentFilePath) {
        File currentFile = new File(currentFilePath);
        String currentDir = currentFile.getParent();

        File importFile = new File(importPath);
        if(importFile.isAbsolute()) {
            return importFile.getAbsolutePath();
        }

        File resolved = new File(currentDir, importPath);
        return resolved.getAbsolutePath();
    }

    private String readFile(String filePath) {
        try {
            StringBuilder content = new StringBuilder();
            try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        } catch(IOException ex) {
            throw new PreScriptException("Cannot read file: " + filePath + " - " + ex.getMessage(),
                    -1);
        }
    }
}
