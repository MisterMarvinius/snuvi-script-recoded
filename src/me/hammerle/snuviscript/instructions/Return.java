package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.inputprovider.ReturnWrapper;

public class Return extends Instruction
{
    private final int arguments;
    
    public Return(int line, int arguments)
    {
        super(line);
        this.arguments = arguments;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] o) throws Exception
    {
        if(o.length > 0)
        {
            ReturnWrapper wrapper = new ReturnWrapper();
            wrapper.setValue(o[0].get(sc));
            sc.handleReturn(wrapper);
        }
        else
        {
            sc.handleReturn(null);
        }
        return null;
    }

    @Override
    public int getArguments()
    {
        return arguments;
    }

    @Override
    public String getName()
    {
        return "return";
    }

    @Override
    public String toString()
    {
        return String.format("return(%d)", arguments);
    }
}
