package me.hammerle.snuviscript.variable;

import java.util.HashMap;
import me.hammerle.snuviscript.code.SnuviUtils;
import me.hammerle.snuviscript.code.Script;

public class LocalArrayVariable extends LocalVariable
{
    public LocalArrayVariable(String name) 
    {
        super(name);
    }
    
    @Override
    public String getString(Script sc) 
    {
        return SnuviUtils.getArrayString(get(sc));
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
        v = new ArrayVariable(name);
        map.put(name, v);
        return v;
    }
    
    @Override
    public String toString() 
    {
        return name + "#L[]";
    }
    
    @Override
    public boolean isArray(Script sc) 
    {
        return true;
    }
}
