package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.code.Script;

public class ConstantString extends InputProvider
{
    private final String s;
    
    public ConstantString(String s)
    {
        this.s = s;
    }
    
    @Override
    public Object get(Script sc) 
    {
        return s;
    }
    
    @Override
    public String getString(Script sc) 
    {
        return s;
    }

    @Override
    public String toString() 
    {
        return "\"" + s + "\"";
    }
}
