package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.math.Fraction;
import me.hammerle.snuviscript.variable.Variable;
import me.hammerle.snuviscript.variable.ArrayVariable;
import me.hammerle.snuviscript.variable.LocalArrayVariable;

public class Function extends InputProvider
{
    private final BasicFunction function;
    private final InputProvider[] input;
    
    public Function(BasicFunction function, InputProvider[] input)
    {
        this.function = function;
        this.input = input;
    }

    @Override
    public String toString() 
    {
        StringBuilder sb = new StringBuilder(function.getName());
        sb.append("(");
        for(InputProvider in : input)
        {
            sb.append(in);
            sb.append(", ");
        }
        if(input.length > 0)
        {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Object get(Script sc) 
    {
        return function.execute(sc, input);
    }
    
    @Override
    public Object getArray(Script sc)
    {
        Object o = function.execute(sc, input);
        if(o instanceof ArrayVariable || o instanceof LocalArrayVariable)
        {
            return o;
        }
        return null;
    }
    
    @Override
    public Fraction getFraction(Script sc)
    {
        return (Fraction) function.execute(sc, input);
    }
    
    @Override
    public int getInt(Script sc)
    {
        return ((Number) function.execute(sc, input)).intValue();
    }
    
    @Override
    public double getDouble(Script sc) 
    {
        return ((Number) function.execute(sc, input)).doubleValue();
    }
    
    @Override
    public String getString(Script sc)
    {
        return String.valueOf(function.execute(sc, input));
    }
    
    @Override
    public boolean getBoolean(Script sc)
    {
        return (Boolean) function.execute(sc, input);
    }
    
    @Override
    public Variable getVariable(Script sc)
    {
        return (Variable) function.execute(sc, input);
    }
}
