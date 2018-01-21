package me.hammerle.snuviscript.constants;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.math.Fraction;

public class ConstantFraction extends InputProvider
{
    private final Fraction f;
    
    public ConstantFraction(Fraction f)
    {
        this.f = f;
    }
    
    @Override
    public Object get(Script sc)
    {
        return f;
    }
    
    @Override
    public Fraction getFraction(Script sc)
    {
        return f;
    }
    
    @Override
    public int getInt(Script sc)
    {
        return f.intValue();
    }
    
    @Override
    public double getDouble(Script sc)
    {
        return f.doubleValue();
    }
    
    @Override
    public String getString(Script sc)
    {
        return String.valueOf(f);
    }

    @Override
    public String toString() 
    {
        return String.valueOf(f);
    }
}
