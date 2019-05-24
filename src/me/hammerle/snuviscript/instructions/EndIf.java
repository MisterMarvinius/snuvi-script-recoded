package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class EndIf extends Instruction
{
    public EndIf(int line)
    {
        super(line);
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception
    {
        sc.setIfState(true);
        return null;
    }

    @Override
    public String getName()
    {
        return "endif";
    }   

    @Override
    public String toString()
    {
        return getName();
    }
}
