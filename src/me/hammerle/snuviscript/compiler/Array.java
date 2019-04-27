package me.hammerle.snuviscript.compiler;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.variable.Variable;

public class Array extends Instruction
{
    private final int arguments;
    private final ReturnWrapper wrapper = new ReturnWrapper();
    private final Variable v;
    
    public Array(int line, int arguments, Variable v)
    {
        super(line);
        this.arguments = arguments;
        this.v = v;
    }
    
    @Override
    public InputProvider execute(Script sc, InputProvider[] in) throws Exception
    {
        Object o = v.get(sc);
        for(InputProvider ip : in)
        {
            o = java.lang.reflect.Array.get(o, ip.getInt(sc));
        }
        wrapper.setValue(o);
        return wrapper;
    }

    @Override
    public int getArguments()
    {
        return arguments;
    }
}
