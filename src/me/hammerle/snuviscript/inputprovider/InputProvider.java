package me.hammerle.snuviscript.inputprovider;

import me.hammerle.snuviscript.code.Script;

public abstract class InputProvider 
{
    public Object get(Script sc)
    {
        throw new ClassCastException();
    }
    
    public byte getByte(Script sc)
    {
        return (byte) getDouble(sc);
    }
    
    public short getShort(Script sc)
    {
        return (short) getDouble(sc);
    }
    
    public int getInt(Script sc)
    {
        return (int) getDouble(sc);
    }
    
    public long getLong(Script sc)
    {
        return (long) getDouble(sc);
    }
    
    public float getFloat(Script sc)
    {
        return (float) getDouble(sc);
    }
    
    public double getDouble(Script sc)
    {
        throw new ClassCastException();
    }
    
    public String getString(Script sc)
    {
        throw new ClassCastException();
    }
    
    public boolean getBoolean(Script sc)
    {
        throw new ClassCastException();
    }
    
    public void set(Script sc, Object o)
    {
        throw new ClassCastException();
    }
}
