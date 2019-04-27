package me.hammerle.snuviscript.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import me.hammerle.snuviscript.code.FunctionLoader;
import me.hammerle.snuviscript.code.ISnuviLogger;
import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.constants.ConstantBoolean;
import me.hammerle.snuviscript.constants.ConstantDouble;
import me.hammerle.snuviscript.constants.ConstantNull;
import me.hammerle.snuviscript.constants.ConstantString;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.token.Token;
import me.hammerle.snuviscript.token.TokenType;
import static me.hammerle.snuviscript.token.TokenType.*;
import me.hammerle.snuviscript.variable.Variable;

public class Compiler
{
    private final ISnuviLogger logger;
    private int index = 0;
    private Token[] tokens = null;
    private final ArrayList<Instruction> instr = new ArrayList<>();
    private final HashMap<String, Integer> labels = new HashMap<>();
    private final HashMap<String, Variable> vars = new HashMap<>();

    public Compiler(ISnuviLogger logger)
    {
        this.logger = logger;
    }
    
    private void addConstant(int line, InputProvider ip)
    {
        instr.add(new Constant(line, ip));
    }
    
    private void addFunction(int line, int args, String name)
    {
        instr.add(new Function(line, args, FunctionLoader.getFunction(name)));
    }
    
    private boolean match(TokenType... types)
    {
        for(TokenType type : types)
        {
            if(check(type))
            {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type)
    {
        if(isAtEnd())
        {
            return false;
        }
        return peek().getType() == type;
    }

    private Token advance()
    {
        if(!isAtEnd())
        {
            index++;
        }
        return previous();
    }

    private boolean isAtEnd()
    {
        return peek().getType() == EOF;
    }

    private Token peek()
    {
        return tokens[index];
    }

    private Token previous()
    {
        return tokens[index - 1];
    }
    
    private Token consume(TokenType type) 
    {
        if(check(type))
        {
            return advance();
        }
        throw new PreScriptException(String.format("exptected %s got %s", type, peek().getType()), peek().getLine());
    }  

    public Script2 compile(Token[] tokens)
    {
        this.tokens = tokens;
        index = 0;
        instr.clear();
        labels.clear();
        vars.clear();

        while(!isAtEnd())
        {
            line();
        }
        
        for(Instruction i : instr)
        {
            logger.print(i.toString(), null, null, null, null, -1);
        }
        
        return null;
    }
    
    private void line()
    {
        int oldIndex = index;
        Token t = advance();
        switch(t.getType())
        {
            case IF: handleIf(); break;
            case LABEL: labels.put(previous().getData().toString(), instr.size()); break;
            case SEMICOLON: break;
            case FOR: handleFor(); break;
            case BREAK: 
                instr.add(new Break(previous().getLine())); 
                consume(SEMICOLON);
                break;
            case CONTINUE:
                instr.add(new Continue(previous().getLine()));
                consume(SEMICOLON);
                break;
            case FUNCTION: handleUserFunction(); break;
            case RETURN: handleReturn(); break;
            case WHILE: handleWhile(); break;
            case TRY: handleTry(); break;
            default:
                index = oldIndex;
                expression();
                consume(SEMICOLON);
        }
    }
    
    private void handleIf()
    {
        Token t = previous();
        consume(OPEN_BRACKET);
        expression();
        instr.add(new If(t.getLine()));
        consume(CLOSE_BRACKET);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        handleElseIf();
    }
    
    private void handleElseIf()
    {
        while(match(ELSEIF))
        {
            Token t = previous();
            consume(OPEN_BRACKET);
            expression();
            instr.add(new Else(t.getLine()));
            consume(CLOSE_BRACKET);
            consume(OPEN_CURVED_BRACKET);
            while(!match(CLOSE_CURVED_BRACKET))
            {
                line();
            }
        }
        handleElse();
    }

    private void handleElse()
    {
        if(match(ELSE))
        {
            instr.add(new Else(previous().getLine()));
            consume(OPEN_CURVED_BRACKET);
            while(!match(CLOSE_CURVED_BRACKET))
            {
                line();
            }
        }
    }
    
    private void handleFor()
    {
        Token t = previous();
        consume(OPEN_BRACKET);    
        if(!match(SEMICOLON))
        {
            expression();
            consume(SEMICOLON);
        }
        if(!match(SEMICOLON))
        {
            expression();
            consume(SEMICOLON);
        }
        if(!match(CLOSE_BRACKET))
        {
            expression();
            consume(CLOSE_BRACKET);
        }
        instr.add(new For(t.getLine()));
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
    }
    
    private void handleUserFunction()
    {
        consume(LITERAL);
        Token t = previous();
        consume(OPEN_BRACKET);
        ArrayList<String> list = new ArrayList<>();
        if(!match(CLOSE_BRACKET))
        {
            while(true)
            {
                consume(LITERAL);
                list.add(previous().getData().toString());
                if(match(CLOSE_BRACKET))
                {
                    break;
                }
                consume(COMMA);
            }
        }  
        instr.add(new UserFunction(t.getLine(), t.getData().toString(), list.toArray(new String[list.size()])));
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
    }
    
    private void handleReturn()
    {
        if(!match(SEMICOLON))
        {
            expression();
            consume(SEMICOLON);
        }
    }
    
    private void handleWhile()
    {
        Token t = previous();
        consume(OPEN_BRACKET);
        expression();
        instr.add(new While(t.getLine()));
        consume(CLOSE_BRACKET);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
    }
    
    private void handleTry()
    {
        instr.add(new Try(previous().getLine()));
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        consume(CATCH);
        instr.add(new Catch(previous().getLine()));
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
    }

    private void expression()
    {
        assignment();
    }
    
    private void assignment()
    {
        logicalOr();
        if(match(SET, ADD_SET, SUB_SET, MUL_SET, DIV_SET, MOD_SET, LEFT_SHIFT_SET, 
                RIGHT_SHIFT_SET, BIT_AND_SET, BIT_XOR_SET, BIT_OR_SET))
        {
            Token t = previous();
            assignment();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }   
    
    private void logicalOr()
    {
        logicalAnd();
        while(match(OR))
        {
            Token t = previous();
            logicalAnd();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }  
    
    private void logicalAnd()
    {
        equality();
        while(match(AND))
        {
            Token t = previous();
            equality();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }  

    private void equality()
    {
        comparison();
        while(match(EQUAL, NOT_EQUAL))
        {
            Token t = previous();
            comparison();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }

    private void comparison()
    {
        addition();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL))
        {
            Token t = previous();
            addition();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }

    private void addition()
    {
        multiplication();
        while(match(SUB, ADD))
        {
            Token t = previous();
            multiplication();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }

    private void multiplication()
    {
        unary();
        while(match(DIV, MUL))
        {
            Token t = previous();
            unary();
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }

    private void unary()
    {
        if(match(INVERT, BIT_INVERT, SUB, INC, DEC))
        {
            Token t = previous();
            unary();
            addFunction(t.getLine(), 1, t.getType().getName());
            return;
        }
        postUnary();
    }
    
    private void postUnary()
    {
        primary();
        while(match(INC, DEC))
        {
            Token t = previous();
            addFunction(t.getLine(), 1, "p" + t.getType().getName());
        }
    }

    private void primary()
    {
        Token t = advance();
        switch(t.getType())
        {
            case FALSE: addConstant(t.getLine(), ConstantBoolean.FALSE); return;
            case TRUE: addConstant(t.getLine(), ConstantBoolean.FALSE); return;
            case NULL: addConstant(t.getLine(), ConstantNull.NULL); return;
            case STRING: addConstant(t.getLine(), new ConstantString(t.getData().toString())); return;
            case LABEL: addConstant(t.getLine(), new ConstantString(t.getData().toString().substring(1))); return;
            case NUMBER: addConstant(t.getLine(), new ConstantDouble((Double) t.getData())); return;
            case OPEN_BRACKET:
                expression();
                consume(CLOSE_BRACKET);
                return;
            case LITERAL:
                if(match(OPEN_SQUARE_BRACKET))
                {
                    handleArray(t);
                }
                else if(match(OPEN_BRACKET))
                {
                    handleFunction(t);
                }
                else
                {
                    addConstant(t.getLine(), getVariable(t.getData().toString()));
                }
                return;
        }
        throw new PreScriptException(String.format("unexpected token %s", t.getType()), t.getLine());
    }
    
    public void handleFunction(Token t)
    {
        int args = 0;
        if(peek().getType() != CLOSE_BRACKET)
        {
            while(true)
            {
                args++;
                expression();
                if(match(CLOSE_BRACKET))
                {
                    break;
                }
                consume(COMMA);
            }
        }
        else
        {
            consume(CLOSE_BRACKET);
        }
        addFunction(t.getLine(), args, t.getData().toString());
    }
    
    public void handleArray(Token t)
    {
        if(peek().getType() == CLOSE_SQUARE_BRACKET)
        {
            throw new PreScriptException("empty array access", peek().getLine());
        }
        int args = 0;
        while(true)
        {
            args++;
            expression();
            if(match(CLOSE_SQUARE_BRACKET))
            {
                break;
            }
            consume(COMMA);
        }
        instr.add(new Array(t.getLine(), args, getVariable(t.getData().toString())));
    }
    
    private Variable getVariable(String name)
    {
        Variable v = vars.get(name);
        if(v != null)
        {
            return v;
        }
        v = new Variable(name);
        vars.put(name, v);
        return v;
    }
}