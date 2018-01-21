package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.math.Fraction;

public class SignInverter extends InputProvider
{
    private final InputProvider input;
    
    public SignInverter(InputProvider input)
    {
        this.input = input;
    }
    
    @Override
    public Object get(Script sc) 
    {
        return ((Fraction) input.get(sc)).invertSign();
    }
    
    @Override
    public Fraction getFraction(Script sc) 
    {
        return input.getFraction(sc).invertSign();
    }
    
    @Override
    public int getInt(Script sc) 
    {
        return input.getFraction(sc).invertSign().intValue();
    }
    
    @Override
    public double getDouble(Script sc) 
    {
        return input.getFraction(sc).invertSign().doubleValue();
    }
    
    @Override
    public String getString(Script sc) 
    {
        return String.valueOf(get(sc));
    }
    
    @Override
    public String toString() 
    {
        return "invertSign(" + input + ")";
    }
}
