package me.hammerle.snuviscript.code;

import java.util.function.BiFunction;

public final class BasicFunction 
{
    private final String name;
    private final BiFunction<Script, InputProvider[], Object> f;
    
    public BasicFunction(String name, BiFunction<Script, InputProvider[], Object> f)
    {
        this.name = name;
        this.f = f;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Object execute(Script sc, InputProvider[] input)
    {
        return f.apply(sc, input);
    }
}
