package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class If extends Goto
{
    public If(int line)
    {
        super(line, 1);
    }
    
    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception
    {
        boolean b = o[0].getBoolean(sc);
        sc.setIfState(b);
        if(!b)
        {
            sc.jumpTo(getJump());
        }
        return null;
    }

    @Override
    public String getName()
    {
        return "if";
    }
}
