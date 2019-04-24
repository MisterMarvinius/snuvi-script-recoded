package me.hammerle.snuviscript.compiler;

import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.token.Token;
import me.hammerle.snuviscript.token.TokenType;

public class Compiler
{
    private int index = 0;
    private Token[] tokens = null;
    
    public Compiler()
    {
    }
    
    public void checkSyntax(Token[] tokens)
    {
        this.tokens = tokens;
        index = 0;
        checkLine();
    }
    
    private Token consumeToken()
    {
        if(index >= tokens.length)
        {
            return null;
        }
        return tokens[index++];
    }
    
    private Token peekOrNullToken()
    {
        if(index >= tokens.length)
        {
            return null;
        }
        return tokens[index];
    }
    
    private Token peekToken()
    {
        if(index >= tokens.length)
        {
            throw new PreScriptException("missing token at end of file", -1);
        }
        return tokens[index];
    }
    
    private void consumeTokenAndCheck(TokenType... type)
    {
        Token t = consumeToken();
        if(t == null)
        {
            throw new PreScriptException("missing token at end of file " + type, -1);
        }
        for(TokenType ty : type)
        {
            if(ty == t.getType())
            {
                return;
            }
        }
        throw new PreScriptException("unexpected token " + t, t.getLine());
    }
    
    private void checkFunctionArguments()
    {
        TokenType type = peekToken().getType();
        if(type == TokenType.CLOSE_BRACKET)
        {
            consumeToken();
            return;
        }

        consumeTokenAndCheck(TokenType.LITERAL);

        while(true)
        {
            type = peekToken().getType();
            if(type == TokenType.CLOSE_BRACKET)
            {
                consumeToken();
                return;
            }
            consumeTokenAndCheck(TokenType.COMMA);
            consumeTokenAndCheck(TokenType.LITERAL);
        }
    }
    
    private void checkLine()
    {
        while(true)
        {
            Token t = peekOrNullToken();
            if(t == null || t.getType() == TokenType.CLOSE_CURVED_BRACKET)
            {
                break;
            }
            consumeToken();
            switch(t.getType())
            {
                case LITERAL:
                    switch(t.getData().toString())
                    {
                        case "function":
                            consumeTokenAndCheck(TokenType.LITERAL);
                            consumeTokenAndCheck(TokenType.OPEN_BRACKET);
                            checkFunctionArguments();
                            consumeTokenAndCheck(TokenType.OPEN_CURVED_BRACKET);
                            checkLine();
                            consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                            break;
                        case "for":
                            consumeTokenAndCheck(TokenType.OPEN_BRACKET);
                            checkArguments(TokenType.CLOSE_BRACKET);
                            consumeTokenAndCheck(TokenType.OPEN_CURVED_BRACKET);
                            checkLine();
                            consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                            break;
                        case "else":
                            consumeTokenAndCheck(TokenType.OPEN_CURVED_BRACKET);
                            checkLine();
                            consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                            break;
                        case "elseif":
                        case "while":
                        case "if":
                            consumeTokenAndCheck(TokenType.OPEN_BRACKET);
                            checkExpression();
                            consumeTokenAndCheck(TokenType.CLOSE_BRACKET);
                            consumeTokenAndCheck(TokenType.OPEN_CURVED_BRACKET);
                            checkLine();
                            consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                            break;
                        case "try":
                            consumeTokenAndCheck(TokenType.OPEN_CURVED_BRACKET);
                            checkLine();
                            consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                            Token token = consumeToken();
                            if(token.getType() != TokenType.LITERAL || !token.getData().equals("catch"))
                            {
                                throw new PreScriptException("try without catch", token.getLine());
                            }
                            consumeTokenAndCheck(TokenType.OPEN_CURVED_BRACKET);
                            checkLine();
                            consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                            break;
                        case "continue":
                        case "break":
                            consumeTokenAndCheck(TokenType.SEMICOLON);
                            break;
                        case "return":
                            if(peekToken().getType() == TokenType.SEMICOLON)
                            {
                                consumeToken();
                            }
                            else
                            {
                                checkExpression();
                                consumeTokenAndCheck(TokenType.SEMICOLON);
                            }
                            break;
                        default:
                            checkAfterLiteral(true);
                            consumeTokenAndCheck(TokenType.SEMICOLON);
                    }
                    break;
                case OPEN_CURVED_BRACKET:
                    checkLine();
                    consumeTokenAndCheck(TokenType.CLOSE_CURVED_BRACKET);
                    break;
                /*case DOLLAR:
                    checkVariable();
                    checkVariableOperation(true);
                    consumeTokenAndCheck(TokenType.SEMICOLON);
                    break;*/
                case LABEL:
                    consumeTokenAndCheck(TokenType.LITERAL, TokenType.NUMBER);
                    break;
                case SEMICOLON:
                    break;
                case INC:
                case DEC:
                    Token token = peekToken();
                    //if(token.getType() == TokenType.DOLLAR)
                    {
                        consumeToken();
                    }
                    checkVariable();
                    consumeTokenAndCheck(TokenType.SEMICOLON);
                    break;
                default:
                    throw new PreScriptException("unexpected token " + t, t.getLine());
            }
        }
    }  
    
    private void checkVariable()
    {
        consumeTokenAndCheck(TokenType.LITERAL);
        
        Token t = peekToken();
        if(t.getType() == TokenType.OPEN_SQUARE_BRACKET)
        {
            consumeToken();
            checkArguments(TokenType.CLOSE_SQUARE_BRACKET);
        }
    }
    
    private void checkAfterLiteral(boolean line)
    {
        Token t = peekToken();
        switch(t.getType())
        {
            case OPEN_BRACKET:
                consumeToken();
                checkArguments(TokenType.CLOSE_BRACKET);
                if(!line)
                {
                    checkCalc();
                }
                return;
            case OPEN_SQUARE_BRACKET:
                consumeToken();
                checkArguments(TokenType.CLOSE_SQUARE_BRACKET);
                checkVariableOperation(line);
                return; 
            case INC:
            case DEC:
                consumeToken();
                checkCalc();
                return; 
            case SET: 
            case ADD_SET:
            case SUB_SET: 
            case MUL_SET:
            case DIV_SET:
            case MOD_SET:
            case LEFT_SHIFT_SET:
            case RIGHT_SHIFT_SET: 
            case BIT_AND_SET:
            case BIT_XOR_SET: 
            case BIT_OR_SET:
                consumeToken();
                checkExpression();
                return;
            default:
                if(line)
                {
                    throw new PreScriptException("unexpected token " + t, t.getLine());
                }
        }
        
        checkCalc();
    }
    
    private void checkVariableOperation(boolean line)
    {
        Token t = peekToken();
        switch(t.getType())
        {
            case INC:
            case DEC:
            case SET: 
            case ADD_SET:
            case SUB_SET: 
            case MUL_SET:
            case DIV_SET:
            case MOD_SET:
            case LEFT_SHIFT_SET:
            case RIGHT_SHIFT_SET: 
            case BIT_AND_SET:
            case BIT_XOR_SET: 
            case BIT_OR_SET:
                consumeToken();
                checkExpression();
                break;
            default:
                if(line)
                {
                    throw new PreScriptException("unexpected token " + t, t.getLine());
                }
                else
                {
                    checkCalc();
                }
        }
    }
    
    private void checkCalc()
    {
        Token t = peekToken();
        switch(t.getType())
        {
            case MUL:
            case DIV:
            case MOD:
            case ADD:
            case SUB:
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
            case EQUAL:
            case NOT_EQUAL:
            case BIT_AND:
            case BIT_XOR:
            case BIT_OR:
            case AND:
            case OR:
                consumeToken();
                checkExpression();
                break;
        }
    }
    
    private void checkArguments(TokenType end)
    {
        Token t = peekToken();
        if(t.getType() == end)
        {
            consumeToken();
            return;
        }
        
        checkExpression();
        
        while(true)
        {
            t = peekToken();
            if(t.getType() == end)
            {
                consumeToken();
                return;
            }
            consumeTokenAndCheck(TokenType.COMMA);
            checkExpression();
        }
    }
    
    private void checkExpression()
    {
        Token t = consumeToken();
        switch(t.getType())
        {
            case SUB:
                checkExpression();
                break;
            case NUMBER:
            case STRING:
                checkCalc();
                break;
            case LITERAL:
                checkAfterLiteral(false);
                break;
            case OPEN_BRACKET:
                checkExpression();
                consumeTokenAndCheck(TokenType.CLOSE_BRACKET);
                checkCalc();
                break;
            /*case DOLLAR:
                checkVariable();
                checkVariableOperation(false);
                break;*/
            case LABEL:
                consumeTokenAndCheck(TokenType.LITERAL);
                break;
            case INC:
            case DEC:
                Token token = peekToken();
                //if(token.getType() == TokenType.DOLLAR)
                {
                    consumeToken();
                }
                checkVariable();
                checkCalc();
                break;
            case INVERT:
            case BIT_INVERT:
                checkExpression();
                break;          
            default:
                throw new PreScriptException("unexpected token " + t, t.getLine());
        }
    }
}