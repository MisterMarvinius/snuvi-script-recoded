package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public abstract class Instruction
{
    private final int line;
    private boolean noReturn = false;
    
    public Instruction(int line)
    {
        this.line = line;
    }
    
    public void setNoReturn()
    {
        noReturn = true;
    }
    
    public boolean shouldNotReturnValue()
    {
        return noReturn;
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
    
    public String getName()
    {
        return "";
    }
}
