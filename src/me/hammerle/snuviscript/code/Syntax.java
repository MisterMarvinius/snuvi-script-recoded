package me.hammerle.snuviscript.code;

public enum Syntax
{
    UNKNOWN(" ", 0, 0),
    MAYBE("", 0, 0),
    INC("++", 2, 1),
    POST_INC("p+", 2, 1),
    DEC("--", 2, 1),
    POST_DEC("p-", 2, 1),
    MUL("*", 3),
    DIV("/", 3),
    MOD("%", 3),
    ADD("+", 4),
    SUB("-", 4),
    UNARY_SUB(" ", 0, 1),
    LEFT_SHIFT("<<", 6),
    RIGHT_SHIFT(">>", 6),
    SMALLER("<", 7),
    SMALLER_EQUAL("<=", 7),
    GREATER(">", 7),
    GREATER_EQUAL(">=", 7),
    EQUAL("==", 8),
    NOT_EQUAL("!=", 8),
    BIT_AND("&", 9),
    BIT_XOR("^", 10),
    BIT_OR("|", 11),
    AND("&&", 12),
    OR("||", 13),
    SET("=", 15),
    ADD_SET("+=", 15),
    SUB_SET("-=", 15),
    MUL_SET("*=", 15),
    DIV_SET("/=", 15),
    MOD_SET("%=", 15),
    LEFT_SHIFT_SET("<<=", 15),
    RIGHT_SHIFT_SET(">>=", 15),
    BIT_AND_SET("&=", 15),
    BIT_XOR_SET("^=", 15),
    BIT_OR_SET("|=", 15);
    
    /*
        LEFT_SHIFT_SET("<<=", 15),
        RIGHT_SHIFT_SET(">>=", 15),
    */

    public static Syntax getSyntax(String s)
    {   
        int size = s.length();
        if(size > 0)
        {
            switch(s.charAt(0))
            {
                case '*':
                    if(size == 1)
                    {
                        return MUL;
                    }
                    else if(size == 2 && s.charAt(1) == '=')
                    {
                        return MUL_SET;
                    }
                    break;
                case '/':
                    if(size == 1)
                    {
                        return DIV;
                    }
                    else if(size == 2 && s.charAt(1) == '=')
                    {
                        return DIV_SET;
                    }
                    break;
                case '+':
                    if(size == 1)
                    {
                        return ADD;
                    }
                    else if(size == 2)
                    {
                        switch(s.charAt(1))
                        {
                            case '=':  return ADD_SET;
                            case '+':  return INC;
                        }
                    }
                    break;
                case '-':
                    if(size == 1)
                    {
                        return SUB;
                    }
                    else if(size == 2)
                    {
                        switch(s.charAt(1))
                        {
                            case '=':  return SUB_SET;
                            case '-':  return DEC;
                        }
                    }
                    break;
                case '^':
                    if(size == 1)
                    {
                        return BIT_XOR;
                    }
                    else if(size == 2 && s.charAt(1) == '=')
                    {
                        return BIT_XOR_SET;
                    }
                    break;
                case '<':
                    if(size == 1)
                    {
                        return SMALLER;
                    }
                    else if(size == 2)
                    {
                        switch(s.charAt(1))
                        {
                            case '<': return LEFT_SHIFT;
                            case '=': return SMALLER_EQUAL;
                        }
                    }
                    else if(size == 3 && s.charAt(1) == '<' && s.charAt(2) == '=')
                    {
                        return LEFT_SHIFT_SET;
                    }
                    break;
                case '>':
                    if(size == 1)
                    {
                        return GREATER;
                    }
                    else if(size == 2)
                    {
                        switch(s.charAt(1))
                        {
                            case '>': return RIGHT_SHIFT;
                            case '=': return GREATER_EQUAL;
                        }
                    }
                    else if(size == 3 && s.charAt(1) == '>' && s.charAt(2) == '=')
                    {
                        return RIGHT_SHIFT_SET;
                    }
                    break;
                case '!':
                    if(size == 1)
                    {
                        return MAYBE;
                    }
                    else if(size == 2 && s.charAt(1) == '=')
                    {
                        return NOT_EQUAL;
                    }
                    break;
                case '=':
                    if(size == 1)
                    {
                        return SET;
                    }
                    else if(size == 2 && s.charAt(1) == '=')
                    {
                        return EQUAL;
                    }
                    break;
                case '&':
                    if(size == 1)
                    {
                        return BIT_AND;
                    }
                    else if(size == 2)
                    {
                        switch(s.charAt(1))
                        {
                            case '&': return AND;
                            case '=': return BIT_AND_SET;
                        }
                    }
                    break;
                case '|':
                    if(size == 1)
                    {
                        return BIT_OR;
                    }
                    else if(size == 2)
                    {
                        switch(s.charAt(1))
                        {
                            case '|': return OR;
                            case '=': return BIT_OR_SET;
                        }
                    }
                    break;
                case '%':
                    if(size == 1)
                    {
                        return MOD;
                    }
                    else if(size == 2 && s.charAt(1) == '=')
                    {
                        return MOD_SET;
                    }
                    break;
            }
        }
        return UNKNOWN;
    }
    
    private int weight;
    private String function;
    private byte pars;

    Syntax(String function, int weight, int pars)
    {
        this.weight = weight;
        this.function = function;
        this.pars = (byte) pars;
    }   
    
    Syntax(String function, int weight)
    {
        this(function, weight, 2);
    }  

    public String getFunction() 
    {
        return function;
    }

    public int getWeight() 
    {
        return weight;
    }
    
    public byte getParameters()
    {
        return pars;
    }
}
