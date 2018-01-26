package me.hammerle.snuviscript.constants;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.math.Fraction;

public class ConstantNull extends InputProvider
{
    public static final ConstantNull NULL = new ConstantNull();
    
    private ConstantNull()
    {
    }
    
    @Override
    public Object get(Script sc)
    {
        return null;
    }
    
    @Override
    public Fraction getFraction(Script sc)
    {
        return null;
    }
    
    @Override
    public String getString(Script sc)
    {
        return "null";
    }   
    
    @Override
    public String toString()
    {
        return "null";
    }
    
    @Override
    public Variable getVariable(Script sc)
    {
        return null;
    }
    
    @Override
    public Object getArray(Script sc)
    {
        return null;
    }
}
