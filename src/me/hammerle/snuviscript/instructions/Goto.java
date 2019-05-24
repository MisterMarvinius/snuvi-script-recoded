package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

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
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception
    {
        sc.jumpTo(jump);
        return null;
    }

    @Override
    public int getArguments()
    {
        return arguments;
    }
    
    public void setJump(int value)
    {
        jump = value;
    }

    public int getJump()
    {
        return jump;
    }
    
    @Override
    public String getName()
    {
        return "goto";
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
        sb.append(jump);
        sb.append(")");
        return sb.toString();
    }
}
