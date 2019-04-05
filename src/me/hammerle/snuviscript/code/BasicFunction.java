package me.hammerle.snuviscript.code;

public final class BasicFunction 
{
    private final String name;
    private final ExceptionBiFunction<Script, InputProvider[], Object> f;
    
    public BasicFunction(String name, ExceptionBiFunction<Script, InputProvider[], Object> f)
    {
        this.name = name;
        this.f = f;
    }
    
    public String getName()
    {
        return name;
    }
    
    public Object execute(Script sc, InputProvider[] input) throws Exception
    {
        sc.currentCommand = name;
        Object o = f.apply(sc, input);
        sc.currentCommand = name;
        return o;
    }
}
