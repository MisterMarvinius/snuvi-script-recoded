package me.hammerle.snuviscript.token;

public class Token
{
    private final TokenType type;
    private final int line;
    
    public Token(TokenType type, int line)
    {
        this.type = type;
        this.line = line;
    }

    public TokenType getType()
    {
        return type;
    }
    
    public Object getData()
    {
        return null;
    }
    
    public int getLine()
    {
        return line;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append("(");
        sb.append(line);
        sb.append(", ");
        sb.append(type);
        if(getData() != null)
        {
            sb.append(", ");
            if(getData() instanceof String)
            {
                sb.append('"');
                sb.append(getData());
                sb.append('"');
            }
            else
            {
                sb.append(getData());
            }
        }
        sb.append(")");
        
        return sb.toString();
    }
}
