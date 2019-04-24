package me.hammerle.snuviscript.token;

public enum TokenType
{
    UNKNOWN(""),
    NUMBER("number"),
    STRING("string"),
    LITERAL("literal"),
    
    OPEN_BRACKET("("),
    CLOSE_BRACKET(")"),
    OPEN_SQUARE_BRACKET("["),
    CLOSE_SQUARE_BRACKET("]"),
    OPEN_CURVED_BRACKET("{"),
    CLOSE_CURVED_BRACKET("}"),
    
    DOLLAR("$"),
    LABEL("@"),
    
    ONE_LINE_COMMENT("//"),
    COMMENT("/*"),
    
    SEMICOLON(";"),
    COMMA(","),
    INC("++"),
    DEC("--"),
    INVERT("!"),
    BIT_INVERT("~"),
    MUL("*"),
    DIV("/"),
    MOD("%"),
    ADD("+"),
    SUB("-"),
    LEFT_SHIFT("<<"),
    RIGHT_SHIFT(">>"),
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">="),
    EQUAL("=="),
    NOT_EQUAL("!="),
    BIT_AND("&"),
    BIT_XOR("^"),
    BIT_OR("|"),
    AND("&&"),
    OR("||"),
    SET("="),
    ADD_SET("+="),
    SUB_SET("-="),
    MUL_SET("*="),
    DIV_SET("/="),
    MOD_SET("%="),
    LEFT_SHIFT_SET("<<="),
    RIGHT_SHIFT_SET(">>="),
    BIT_AND_SET("&="),
    BIT_XOR_SET("^="),
    BIT_OR_SET("|=");
        
    private final String name;

    private TokenType(String name)
    {
        this.name = name;
    }   

    public String getName() 
    {
        return name;
    }
    
    public int getLength()
    {
        return name.length();
    }
    
    public static TokenType getMatching(char c1, char c2, char c3)
    {
        switch(c1)
        {
            case '(': return OPEN_BRACKET;
            case ')': return CLOSE_BRACKET;
            case '[': return OPEN_SQUARE_BRACKET;
            case ']': return CLOSE_SQUARE_BRACKET;
            case '{': return OPEN_CURVED_BRACKET;
            case '}': return CLOSE_CURVED_BRACKET;
            case '$': return DOLLAR;
            case '@': return LABEL;
            case ';': return SEMICOLON;
            case ',': return COMMA;
            case '~': return BIT_INVERT;
            case '+': return c2 == '=' ? ADD_SET : (c2 == '+' ? INC : ADD);
            case '-': return c2 == '=' ? SUB_SET : (c2 == '-' ? DEC : SUB);
            case '!': return c2 == '=' ? NOT_EQUAL : INVERT;
            case '=': return c2 == '=' ? EQUAL : SET;
            case '*': return c2 == '=' ? MUL_SET : MUL;
            case '/': 
                switch(c2)
                {
                    case '/': return ONE_LINE_COMMENT;
                    case '*': return COMMENT;
                    case '=': return DIV_SET;
                }
                return DIV;
            case '%': return c2 == '=' ? MOD_SET : MOD;
            case '&': return c2 == '=' ? BIT_AND_SET : (c2 == '&' ? AND : BIT_AND);
            case '|': return c2 == '=' ? BIT_OR_SET : (c2 == '|' ? OR : BIT_OR);
            case '^': return c2 == '=' ? BIT_XOR_SET : BIT_XOR;
            case '<': 
                switch(c2)
                {
                    case '<': return c3 == '=' ? LEFT_SHIFT_SET : LEFT_SHIFT;
                    case '=': return LESS_EQUAL;
                }
                return LESS;
            case '>': 
                switch(c2)
                {
                    case '>': return c3 == '=' ? RIGHT_SHIFT_SET : RIGHT_SHIFT;
                    case '=': return GREATER_EQUAL;
                }
                return GREATER;    
        }
        return UNKNOWN;
    }
}
