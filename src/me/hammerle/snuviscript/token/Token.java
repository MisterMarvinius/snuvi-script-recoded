package me.hammerle.snuviscript.token;

public enum Token
{
    VAR,
    INT,
    DOUBLE,
    LABEL,
    
    INC, // ++
    DEC, // --
    INVERT, // !
    BIT_INVERT, // ~
    MUL, // *
    DIV, // /
    MOD, // %
    ADD, // +
    SUB, // -
    LEFT_SHIFT, // <<
    RIGHT_SHIFT, // >>
    SMALLER, // <
    SMALLER_EQUAL, // <=
    GREATER, // >
    GREATER_EQUAL, // >=
    EQUAL, // ==
    NOT_EQUAL, // !=
    BIT_AND, // &
    BIT_XOR, // ^
    BIT_OR, // |
    AND, // &&
    OR, // ||
    SET, // =
    ADD_SET, // +=
    SUB_SET, // -=
    MUL_SET, // *=
    DIV_SET, // /=
    MOD_SET, // %=
    LEFT_SHIFT_SET, // <<=
    RIGHT_SHIFT_SET, // >>=
    BIT_AND_SET, // &=
    BIT_XOR_SET, // ^=
    BIT_OR_SET, // |=
    COMMA, // ,
    OPEN_BRACKET, // (
    CLOSE_BRACKET, // )
    OPEN_SQUARE_BRACKET, // [
    CLOSE_SQUARE_BRACKET, // ]
    OPEN_CURVED_BRACKET, // {
    CLOSE_CURVED_BRACKET, // }
    SEMICOLON, // ;
    
    IF,
    ELSE,
    FOR,
    WHILE,
    FUNCTION,
    BREAK,
    CONTINUE,
    RETURN,
    GOTO,
    GOSUB
}
