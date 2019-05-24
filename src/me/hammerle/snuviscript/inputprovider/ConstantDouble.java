package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.code.SnuviUtils;

public class ConstantDouble extends InputProvider
{
    private final double d;
    
    public ConstantDouble(double f)
    {
        this.d = f;
    }
    
    @Override
    public Object get(Script sc)
    {
        return d;
    }
    
    @Override
    public double getDouble(Script sc)
    {
        return d;
    }
    
    @Override
    public String getString(Script sc)
    {
        return SnuviUtils.toString(d);
    }

    @Override
    public String toString() 
    {
        return SnuviUtils.toString(d);
    }
}
