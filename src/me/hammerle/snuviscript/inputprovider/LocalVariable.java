package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.code.Script;

public class LocalVariable extends Variable
{
    public LocalVariable(String name)
    {
        super(name);
    }
    
    @Override
    public String toString() 
    {
        return name + "#L";
    }
    
    @Override
    public Object get(Script sc) 
    {
        return getVariable(sc).get(sc);
    }
    
    @Override
    public double getDouble(Script sc)
    {
        return getVariable(sc).getDouble(sc);
    }
    
    @Override
    public String getString(Script sc)
    {
        return getVariable(sc).getString(sc);
    }
    
    @Override
    public boolean getBoolean(Script sc)
    {
        return getVariable(sc).getBoolean(sc);
    }
    
    private Variable getVariable(Script sc)
    {
        return sc.getOrAddLocalVariable(name);
    }
    
    @Override
    public void set(Script sc, Object o) 
    {
        getVariable(sc).set(sc, o);
    }
}
