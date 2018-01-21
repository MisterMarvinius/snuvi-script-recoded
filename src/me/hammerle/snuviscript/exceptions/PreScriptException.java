package me.hammerle.snuviscript.exceptions;

public class PreScriptException extends RuntimeException
{
    public PreScriptException(String message, int line) 
    {
        super(message + " - line " + line);
    }
}
