package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.code.CompiledImport;
import me.hammerle.snuviscript.code.ImportManager;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.inputprovider.InputProvider;

public class Import extends Instruction {
    private final String importPath;
    private final ImportManager importManager;

    public Import(int line, String importPath, ImportManager importManager) {
        super(line); // No stack arguments
        this.importPath = importPath;
        this.importManager = importManager;
    }

    @Override
    public InputProvider execute(Script script, InputProvider[] args) throws Exception {
        CompiledImport imported = importManager.importScript(importPath);

        // Merge imported functions, variables, and labels into current script
        script.mergeFunctions(imported.getFunctions());
        script.mergeVariables(imported.getVariables());
        script.mergeLabels(imported.getLabels());

        return null; // Import statements don't return values
    }

    @Override
    public String getName() {
        return "import";
    }
}
