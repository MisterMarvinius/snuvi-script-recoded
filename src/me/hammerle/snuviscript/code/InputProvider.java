package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.math.Fraction;

public abstract class InputProvider 
{
    public Object get(Script sc)
    {
        throw new ClassCastException();
    }
    
    public Fraction getFraction(Script sc)
    {
        throw new ClassCastException();
    }
    
    public int getInt(Script sc)
    {
        throw new ClassCastException();
    }
    
    public double getDouble(Script sc)
    {
        throw new ClassCastException();
    }
    
    public String getString(Script sc)
    {
        throw new ClassCastException();
    }
    
    public boolean getBoolean(Script sc)
    {
        throw new ClassCastException();
    }
    
    public Variable getVariable(Script sc)
    {
        throw new ClassCastException();
    }
    
    public void set(Script sc, Object o)
    {
        throw new ClassCastException();
    }
    
    public Object getArray(Script sc)
    {
        return null;
    }
    
    public boolean isArray(Script sc)
    {
        return false;
    }
}
