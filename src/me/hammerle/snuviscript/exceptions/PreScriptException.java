package me.hammerle.snuviscript.exceptions;

public class PreScriptException extends RuntimeException
{
    private final int startLine;
    private final int endLine;
    
    public PreScriptException(String message, int startLine, int endLine) 
    {
        super(message);
        this.endLine = endLine;
        this.startLine = startLine;
    }
    
    public PreScriptException(String message, int endLine) 
    {
        this(message, -1, endLine);
    }
    
    public int getStartLine()
    {
        return startLine;
    }

    public int getEndLine()
    {
        return endLine;
    }
}
