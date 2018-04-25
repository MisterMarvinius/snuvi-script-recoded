package me.hammerle.snuviscript.variable;

import java.util.HashMap;
import me.hammerle.snuviscript.code.InputProvider;
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
    
    @Override
    public Variable getVariable(Script sc)
    {
        HashMap<String, Variable> map = sc.getLocalVars();
        Variable v = map.get(name);
        if(v != null)
        {
            return v;
        }
        v = new Variable(name);
        map.put(name, v);
        return v;
    }
    
    @Override
    public void set(Script sc, Object o) 
    {
        getVariable(sc).set(sc, o);
    }
    
    @Override
    public Object getArray(Script sc)
    {
        return getVariable(sc).getArray(sc);
    }
}
