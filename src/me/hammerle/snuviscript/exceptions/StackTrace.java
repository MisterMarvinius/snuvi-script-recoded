package me.hammerle.snuviscript.exceptions;

import java.util.Stack;

public class StackTrace {
    private final String stackTrace;

    public StackTrace(int currentLine, Stack<Integer> stack) {
        if(stack == null) {
            stackTrace = String.valueOf(currentLine);
            return;
        }
        StringBuilder sb = new StringBuilder();
        stack.forEach(line -> sb.append(line).append(" > "));
        sb.append(currentLine);
        stackTrace = sb.toString();
    }

    public StackTrace(int currentLine) {
        this(currentLine, null);
    }

    @Override
    public String toString() {
        return stackTrace;
    }
}
