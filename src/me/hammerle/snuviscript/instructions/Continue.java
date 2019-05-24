package me.hammerle.snuviscript.instructions;

public class Continue extends Goto
{
    public Continue(int line)
    {
        super(line, 0);
    }
    
    @Override
    public String getName()
    {
        return "continue";
    }
}
