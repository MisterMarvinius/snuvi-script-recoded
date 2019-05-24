package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

public class ConstantBoolean extends InputProvider
{
    public static final ConstantBoolean TRUE = new ConstantBoolean(true);
    public static final ConstantBoolean FALSE = new ConstantBoolean(false);
    
    private final boolean b;
    
    private ConstantBoolean(boolean b)
    {
        this.b = b;
    }
    
    @Override
    public Object get(Script sc)
    {
        return b;
    }
    
    @Override
    public String getString(Script sc)
    {
        return String.valueOf(b);
    }
    
    @Override
    public boolean getBoolean(Script sc)
    {
        return b;
    }

    @Override
    public String toString()
    {
        return String.valueOf(b);
    }
}
