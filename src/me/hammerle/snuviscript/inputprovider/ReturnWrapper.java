package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

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
    public String toString()
    {
        return String.format("ReturnWrapper(%s)", o);
    }
}
