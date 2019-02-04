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
        sc.currentCommand = name;
        Object o = f.apply(sc, input);
        sc.currentCommand = name;
        return o;
    }
}
