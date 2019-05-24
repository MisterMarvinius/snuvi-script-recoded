package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.code.Script;

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
    public String getString(Script sc)
    {
        return "null";
    }   
    
    @Override
    public String toString()
    {
        return "null";
    }
}
