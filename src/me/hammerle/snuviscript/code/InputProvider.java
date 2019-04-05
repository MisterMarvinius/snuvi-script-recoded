package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.variable.Variable;

public abstract class InputProvider 
{
    public Object get(Script sc) throws Exception
    {
        throw new ClassCastException();
    }
    
    public byte getByte(Script sc) throws Exception
    {
        return (byte) getDouble(sc);
    }
    
    public short getShort(Script sc) throws Exception
    {
        return (short) getDouble(sc);
    }
    
    public int getInt(Script sc) throws Exception
    {
        return (int) getDouble(sc);
    }
    
    public long getLong(Script sc) throws Exception
    {
        return (long) getDouble(sc);
    }
    
    public float getFloat(Script sc) throws Exception
    {
        return (float) getDouble(sc);
    }
    
    public double getDouble(Script sc) throws Exception
    {
        throw new ClassCastException();
    }
    
    public String getString(Script sc) throws Exception
    {
        throw new ClassCastException();
    }
    
    public boolean getBoolean(Script sc) throws Exception
    {
        throw new ClassCastException();
    }
    
    public Variable getVariable(Script sc) throws Exception
    {
        throw new ClassCastException();
    }
    
    public void set(Script sc, Object o) throws Exception
    {
        throw new ClassCastException();
    }
    
    public Object getArray(Script sc) throws Exception
    {
        return null;
    }
    
    public boolean isArray(Script sc)
    {
        return false;
    }
}
