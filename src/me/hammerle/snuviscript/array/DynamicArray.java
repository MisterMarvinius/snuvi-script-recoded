package me.hammerle.snuviscript.array;

import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.variable.Variable;
import java.lang.reflect.Array;
import me.hammerle.snuviscript.code.SnuviUtils;
import me.hammerle.snuviscript.code.Script;
import me.hammerle.snuviscript.variable.LocalVariable;

public class DynamicArray extends InputProvider
{
    protected Variable var;
    protected InputProvider[] input;
    
    public DynamicArray(Variable var, InputProvider[] input)
    {
        this.var = var;
        this.input = input;
    }

    public int getLength(Script sc)
    {
        return Array.getLength(var.getArray(sc));
    }
    
    public void init(Script sc)
    {
        int[] dims = new int[input.length];
        for(int i = 0; i < dims.length; i++)
        {
            dims[i] = input[i].getInt(sc);
        }
        var.set(sc, Array.newInstance(Object.class, dims));
    }
    
    @Override
    public Object getArray(Script sc)
    {
        Object ob = var.getArray(sc);
        for(InputProvider in : input) 
        {
            ob = Array.get(ob, in.getInt(sc));
        }
        return ob;
    }
    
    public Object getLastArray(Script sc)
    {
        Object ob = var.getArray(sc);
        int end = input.length - 1;
        for(int j = 0; j < end; j++)
        {
            ob = Array.get(ob, input[j].getInt(sc));
        }
        return ob;
    }
    
    @Override
    public void set(Script sc, Object o) 
    {
        Array.set(getLastArray(sc), input[input.length - 1].getInt(sc), o);
    }

    @Override
    public Object get(Script sc) 
    {
        return Array.get(getLastArray(sc), input[input.length - 1].getInt(sc));
    }
    
    @Override
    public double getDouble(Script sc) 
    {
        return (double) get(sc);
    }
    
    @Override
    public String getString(Script sc) 
    {
        Object last = getLastArray(sc);
        int index = input[input.length - 1].getInt(sc);
        try
        {
            return String.valueOf(Array.get(last, index));
        }
        catch(IllegalArgumentException ex)
        {
            return SnuviUtils.getArrayString(Array.get(last, index));
        }
    }

    @Override
    public String toString() 
    {
        StringBuilder sb = new StringBuilder(var.getName());
        if(var instanceof LocalVariable)
        {
            sb.append("#");
            sb.append("L");
        }
        sb.append("[");
        for(InputProvider in : input)
        {
            sb.append(in);
            sb.append(", ");
        }
        if(input.length > 0)
        {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("]");
        return sb.toString();
    }
}
