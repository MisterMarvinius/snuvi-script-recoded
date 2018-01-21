package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class JumpData extends InputProvider
{
    private int jump;
    
    public JumpData(int jump)
    {
        this.jump = jump;
    }

    @Override
    public int getInt(Script sc) 
    {
        return jump;
    }

    public void setRelativeJump(int jump) 
    {
        this.jump = jump - this.jump - 1;
    }

    @Override
    public String toString() 
    {
        return "jump_" + jump;
    }
}
