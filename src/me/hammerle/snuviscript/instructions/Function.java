package me.hammerle.snuviscript.instructions;

import me.hammerle.snuviscript.inputprovider.ReturnWrapper;
import me.hammerle.snuviscript.code.NamedFunction;
import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class Function extends Instruction
{
    private final NamedFunction function;
    private final int arguments;
    private final ReturnWrapper wrapper = new ReturnWrapper();
    
    public Function(int line, int arguments, NamedFunction function)
    {
        super(line);
        this.function = function;
        this.arguments = arguments;
    }

    @Override
    public InputProvider execute(Script sc, InputProvider[] in) throws Exception
    {
        Object o = function.execute(sc, in);
        if(o == Void.TYPE || shouldNotReturnValue())
        {
            return null;
        }
        wrapper.setValue(o);
        return wrapper;
    }

    @Override
    public int getArguments()
    {
        return arguments;
    }

    @Override
    public String toString()
    {
        return String.format("use %s(%d)", function.getName(), arguments);
    }
}
