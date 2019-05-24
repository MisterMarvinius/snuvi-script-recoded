package me.hammerle.snuviscript.tokenizer;

public class DataToken extends Token
{
    private final Object value;
    
    public DataToken(TokenType type, int line,  Object value)
    {
        super(type, line);
        this.value = value;
    }

    @Override
    public Object getData()
    {
        return value;
    }
}
