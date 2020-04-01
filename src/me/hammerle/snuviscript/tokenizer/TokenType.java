package me.hammerle.snuviscript.tokenizer;

public enum TokenType {
    NUMBER("number"), STRING("string"), LITERAL("literal"), LABEL("label"),
    TRUE("true"), FALSE("false"), NULL("null"),
    OPEN_BRACKET("("), CLOSE_BRACKET(")"),
    OPEN_SQUARE_BRACKET("["), CLOSE_SQUARE_BRACKET("]"),
    OPEN_CURVED_BRACKET("{"), CLOSE_CURVED_BRACKET("}"),
    SEMICOLON(";"), COMMA(","),
    INC("++"), DEC("--"),
    INVERT("!"), BIT_INVERT("~"),
    MUL("*"), DIV("/"), MOD("%"), ADD("+"), SUB("-"),
    ADD_SET("+="), SUB_SET("-="), MUL_SET("*="), DIV_SET("/="), MOD_SET("%="),
    LEFT_SHIFT("<<"), RIGHT_SHIFT(">>"),
    LEFT_SHIFT_SET("<<="), RIGHT_SHIFT_SET(">>="), BIT_AND_SET("&="), BIT_XOR_SET("^="), BIT_OR_SET("|="),
    LESS("<"), LESS_EQUAL("<="), GREATER(">"), GREATER_EQUAL(">="), EQUAL("=="), NOT_EQUAL("!="),
    BIT_AND("&"), BIT_XOR("^"), BIT_OR("|"),
    AND("&&"), OR("||"), SET("="),
    IF("if"), ELSE("else"), ELSEIF("else if"), WHILE("while"), TRY("try"),
    CATCH("catch"), FOR("for"), FUNCTION("function"), BREAK("break"),
    CONTINUE("continue"), RETURN("return"),
    EOF("end of file");

    private final String name;

    private TokenType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
