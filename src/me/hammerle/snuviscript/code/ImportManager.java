package me.hammerle.snuviscript.code;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.tokenizer.Token;
import me.hammerle.snuviscript.tokenizer.Tokenizer;

public class ImportManager {
    // Global cache of compiled imports to avoid recompiling the same file
    private static final ConcurrentHashMap<String, CompiledImport> globalImportCache =
            new ConcurrentHashMap<>();

    // Per-script session tracking of what's been imported
    private final Set<String> importedInThisSession = new HashSet<>();
    private final String scriptDirectory;

    public ImportManager(String mainScriptPath) {
        // Determine base directory for relative imports
        File mainFile = new File(mainScriptPath);
        this.scriptDirectory = mainFile.getParent() != null ? mainFile.getParent() : ".";
    }

    /**
     * Import a script if not already imported in this session
     * @param importPath Path to import (relative to script directory)
     * @return CompiledImport containing functions, variables, and labels
     */
    public CompiledImport importScript(String importPath) {
        // Resolve relative path
        String absolutePath = resolveImportPath(importPath);

        // Check if already imported in this session
        if(importedInThisSession.contains(absolutePath)) {
            return globalImportCache.get(absolutePath);
        }

        // Check global cache first
        CompiledImport cached = globalImportCache.get(absolutePath);
        if(cached != null) {
            importedInThisSession.add(absolutePath);
            return cached;
        }

        // Compile and cache the import
        CompiledImport compiled = compileImport(absolutePath);
        globalImportCache.put(absolutePath, compiled);
        importedInThisSession.add(absolutePath);

        return compiled;
    }

    private String resolveImportPath(String importPath) {
        File importFile = new File(importPath);
        if(importFile.isAbsolute()) {
            return importFile.getAbsolutePath();
        }

        // Relative to script directory
        File resolved = new File(scriptDirectory, importPath);
        return resolved.getAbsolutePath();
    }

    private CompiledImport compileImport(String absolutePath) {
        try {
            InputStream stream = new FileInputStream(absolutePath);

            // Create temporary compiler for this import
            Tokenizer tokenizer = new Tokenizer();
            Compiler compiler = new Compiler();

            // Create a temporary import manager for nested imports
            ImportManager nestedImportManager = new ImportManager(absolutePath);
            compiler.setImportManager(nestedImportManager);

            // Compile the import
            Token[] tokens = tokenizer.tokenize(stream);
            return compiler.compileImport(tokens);

        } catch(FileNotFoundException ex) {
            throw new PreScriptException("Import file not found: " + absolutePath, -1);
        }
    }

    public boolean isImported(String importPath) {
        String absolutePath = resolveImportPath(importPath);
        return importedInThisSession.contains(absolutePath);
    }

    // Clear session imports (useful for testing or when script restarts)
    public void clearSessionImports() {
        importedInThisSession.clear();
    }

    // Static method to clear global cache if needed
    public static void clearGlobalCache() {
        globalImportCache.clear();
    }
}
