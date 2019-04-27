package me.hammerle.snuviscript.compiler;

public class UserFunction extends Goto
{
    private final String[] vars;
    private final String name;
    
    public UserFunction(int line, String name, String[] vars)
    {
        super(line, 0);
        this.vars = vars;
        this.name = name;
    }

    @Override
    public String[] getVars()
    {
        return vars;
    }

    @Override
    public String getName()
    {
        return name;
    }
}
