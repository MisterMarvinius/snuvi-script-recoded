package me.hammerle.snuviscript.compiler;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Script;

public abstract class Instruction
{
    private final static String[] VARS = new String[0];
    private final int line;
    
    public Instruction(int line)
    {
        this.line = line;
    }

    public int getLine()
    {
        return line;
    }
    
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception
    {
        return null;
    }
    
    public int getArguments()
    {
        return 0;
    }
    
    public void setJump(int value)
    {
    }
    
    public int getJump()
    {
        return 0;
    }
    
    public String[] getVars()
    {
        return VARS;
    }
    
    public String getName()
    {
        return "";
    }
}
