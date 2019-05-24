package me.hammerle.snuviscript.instructions;

public class Break extends Goto
{
    public Break(int line)
    {
        super(line, 0);
    }

    @Override
    public String getName()
    {
        return "break";
    }
}
