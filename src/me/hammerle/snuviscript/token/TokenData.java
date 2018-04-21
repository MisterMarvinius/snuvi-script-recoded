package me.hammerle.snuviscript.token;

public class TokenData 
{
    private final Token t;
    private final int line;
    private final Object o;
    
    public TokenData(Token t, int line, Object o)
    {
        this.t = t;
        this.line = line;
        this.o = o;
    }
    
    public TokenData(Token t, int line)
    {
        this(t, line, null);
    }

    @Override
    public String toString() 
    {
        StringBuilder sb = new StringBuilder();
        sb.append(t);
        sb.append('(');
        sb.append(line);
        if(o != null)
        {
            sb.append(", ");
            sb.append(o);
        }
        sb.append(')');
        return sb.toString();
    }
}
