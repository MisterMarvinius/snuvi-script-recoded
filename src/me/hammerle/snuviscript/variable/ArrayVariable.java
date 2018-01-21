package me.hammerle.snuviscript.variable;

import me.hammerle.snuviscript.code.DataUtils;
import me.hammerle.snuviscript.code.Script;

public class ArrayVariable extends Variable
{
    public ArrayVariable(String name) 
    {
        super(name);
    }
    
    @Override
    public String getString(Script sc) 
    {
        return DataUtils.getArrayString(get(sc));
    }

    @Override
    public boolean isArray(Script sc) 
    {
        return true;
    }
}
