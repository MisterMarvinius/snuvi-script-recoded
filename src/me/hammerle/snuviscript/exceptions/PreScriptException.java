package me.hammerle.snuviscript.exceptions;

public class PreScriptException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int line;

    public PreScriptException(String message, int line) {
        super(message);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}
