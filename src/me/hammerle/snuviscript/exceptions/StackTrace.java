package me.hammerle.snuviscript.exceptions;

import java.util.Stack;
import me.hammerle.snuviscript.code.FileRegistry;
import me.hammerle.snuviscript.instructions.Instruction;

public class StackTrace {
    private final String stackTrace;

    public StackTrace(int currentLine, Stack<Integer> stack, Instruction[] code) {
        if(stack == null || code == null) {
            int file = (currentLine >> 24) & 0xFF;
            int line = currentLine & 0xFFFFFF;
            String fileName = FileRegistry.getFileName(file);
            stackTrace = String.format("%s:%d", fileName, line);
            return;
        }
        StringBuilder sb = new StringBuilder();
        stack.forEach(stackLine -> {
            int lineNum = code[stackLine].getLine();
            int file = (lineNum >> 24) & 0xFF;
            lineNum &= 0xFFFFFF;
            String fileName = FileRegistry.getFileName(file);
            sb.append(fileName).append(":").append(lineNum).append(" > ");
        });

        int file = (currentLine >> 24) & 0xFF;
        int line = currentLine & 0xFFFFFF;
        String fileName = FileRegistry.getFileName(file);
        sb.append(fileName).append(":").append(line);

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
