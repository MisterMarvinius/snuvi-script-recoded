package me.hammerle.snuviscript.compiler;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.variable.Variable;

public class ReturnWrapper extends InputProvider
{
    private Object o;
    
    public void setValue(Object o)
    {
        this.o = o;
    }

    @Override
    public Object get(Script sc) throws Exception
    {
        return o;
    }
    
    @Override
    public Object getArray(Script sc) throws Exception
    {
        return o;
    }
    
    @Override
    public double getDouble(Script sc) throws Exception
    {
        return (double) o;
    }
    
    @Override
    public String getString(Script sc) throws Exception
    {
        return String.valueOf(o);
    }
    
    @Override
    public boolean getBoolean(Script sc) throws Exception
    {
        return (Boolean) o;
    }
    
    @Override
    public Variable getVariable(Script sc) throws Exception
    {
        return (Variable) o;
    }
}
