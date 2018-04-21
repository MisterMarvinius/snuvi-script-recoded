package me.hammerle.snuviscript.variable;

import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.InputProvider;

public class Variable extends InputProvider
{
    protected final String name;
    private Object o;
    
    public Variable(String name)
    {
        this.name = name;
        this.o = null;
    }

    public String getName() 
    {
        return name;
    }
    
    @Override
    public String toString() 
    {
        return name;
    }
    
    @Override
    public Object get(Script sc)
    {
        return o;
    }
    
    @Override
    public double getDouble(Script sc)
    {
        return (double) o;
    }
    
    @Override
    public String getString(Script sc)
    {
        return String.valueOf(o);
    }
    
    @Override
    public boolean getBoolean(Script sc)
    {
        return (boolean) o;
    }
    
    @Override
    public Variable getVariable(Script sc) 
    {
        return this;
    }
    
    @Override
    public void set(Script sc, Object o)
    {
        this.o = o;
    }
    
    @Override
    public Object getArray(Script sc)
    {
        return o;
    }
}
