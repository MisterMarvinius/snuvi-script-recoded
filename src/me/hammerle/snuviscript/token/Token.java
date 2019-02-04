package me.hammerle.snuviscript.token;

public class Token 
{
    private final TokenType t;
    private final int line;
    private final Object o;
    
    public Token(TokenType t, int line, Object o)
    {
        this.t = t;
        this.line = line;
        this.o = o;
    }
    
    public Token(TokenType t, int line)
    {
        this(t, line, null);
    }
    
    public TokenType getToken()
    {
        return t;
    }
    
    public Object getData()
    {
        return o;
    }
    
    public int getLine()
    {
        return line;
    }

    @Override
    public String toString() 
    {
        if(o != null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(t);
            sb.append('(');
            sb.append(o);
            sb.append(')');
            return sb.toString();
        }
        return t.toString();
    }
}
