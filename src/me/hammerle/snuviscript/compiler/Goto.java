package me.hammerle.snuviscript.compiler;

public class Goto extends Instruction
{
    private int jump;
    private final int arguments;
    
    public Goto(int line, int arguments)
    {
        super(line);
        this.arguments = arguments;
    }

    @Override
    public int getArguments()
    {
        return arguments;
    }
    
    @Override
    public void setJump(int value)
    {
        jump = value;
    }

    @Override
    public int getJump()
    {
        return jump;
    }
}
