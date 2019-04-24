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
        return type.getName();
    }
}
