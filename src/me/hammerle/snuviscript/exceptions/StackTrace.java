package me.hammerle.snuviscript.exceptions;

import java.util.Stack;
import me.hammerle.snuviscript.instructions.Instruction;

public class StackTrace {
    private final String stackTrace;

    public StackTrace(int currentLine, Stack<Integer> stack, Instruction[] code) {
        if(stack == null || code == null) {
            stackTrace = String.valueOf(currentLine);
            return;
        }
        StringBuilder sb = new StringBuilder();
        stack.forEach(stackLine -> {
            int line = code[stackLine].getLine();
            int file = (line >> 24) & 0xFF;
            line &= 0xFFFFFF;
            sb.append(line).append("(").append(file).append(") > ");

        });
        int file = (currentLine >> 24) & 0xFF;
        currentLine &= 0xFFFFFF;
        sb.append(currentLine).append("(").append(file).append(")");

        stackTrace = sb.toString();
    }

    public StackTrace(int currentLine) {
        this(currentLine, null, null);
    }

    @Override
    public String toString() {
        return stackTrace;
    }
}
