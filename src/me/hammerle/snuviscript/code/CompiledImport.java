package me.hammerle.snuviscript.code;

import java.util.HashMap;
import me.hammerle.snuviscript.inputprovider.Variable;
import me.hammerle.snuviscript.instructions.Instruction;

public class CompiledImport {
    private final HashMap<String, Integer> functions;
    private final HashMap<String, Variable> variables;
    private final HashMap<String, Integer> labels;
    private final Instruction[] instructions;

    public CompiledImport(HashMap<String, Integer> functions,
            HashMap<String, Variable> variables,
            HashMap<String, Integer> labels,
            Instruction[] instructions) {
        this.functions = new HashMap<>(functions);
        this.variables = new HashMap<>(variables);
        this.labels = new HashMap<>(labels);
        this.instructions = instructions.clone();
    }

    public HashMap<String, Integer> getFunctions() {
        return functions;
    }

    public HashMap<String, Variable> getVariables() {
        return variables;
    }

    public HashMap<String, Integer> getLabels() {
        return labels;
    }

    public Instruction[] getInstructions() {
        return instructions;
    }
}
