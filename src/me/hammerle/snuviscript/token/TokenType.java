package me.hammerle.snuviscript.token;

public enum TokenType
{
    DOUBLE("double"),
    TRUE("true"),
    FALSE("false"),
    NULL("null"),
    TEXT("String"),
    LABEL("Label"),
    VAR("var"),
    
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
    BIT_OR_SET("|="), 
    COMMA(","), 
    OPEN_BRACKET("("), 
    CLOSE_BRACKET(")"), 
    OPEN_SQUARE_BRACKET("["), 
    CLOSE_SQUARE_BRACKET("]"), 
    OPEN_CURVED_BRACKET("{"), 
    CLOSE_CURVED_BRACKET("}"), 
    SEMICOLON(";"), 
    
    IF("if"),
    ELSE_IF("elseif"),
    ELSE("else"),
    FOR("for"),
    WHILE("while"),
    FUNCTION("function"),
    BREAK("break"),
    CONTINUE("continue"),
    RETURN("return"),
    TRY("try"),
    CATCH("catch"),
    
    END_OF_FILE("end_of_file");
    
    private final String name;
    
    TokenType(String name)
    {
        this.name = name;
    }

    @Override
    public String toString() 
    {
        return name;
    }
}
