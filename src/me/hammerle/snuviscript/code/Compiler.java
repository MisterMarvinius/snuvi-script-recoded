package me.hammerle.snuviscript.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import me.hammerle.snuviscript.inputprovider.InputProvider;
import me.hammerle.snuviscript.inputprovider.ConstantBoolean;
import me.hammerle.snuviscript.inputprovider.ConstantDouble;
import me.hammerle.snuviscript.inputprovider.ConstantNull;
import me.hammerle.snuviscript.inputprovider.ConstantString;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import me.hammerle.snuviscript.tokenizer.Token;
import me.hammerle.snuviscript.tokenizer.TokenType;
import static me.hammerle.snuviscript.tokenizer.TokenType.*;
import me.hammerle.snuviscript.inputprovider.LocalVariable;
import me.hammerle.snuviscript.inputprovider.Variable;
import me.hammerle.snuviscript.instructions.Array;
import me.hammerle.snuviscript.instructions.Break;
import me.hammerle.snuviscript.instructions.Catch;
import me.hammerle.snuviscript.instructions.Constant;
import me.hammerle.snuviscript.instructions.Continue;
import me.hammerle.snuviscript.instructions.Else;
import me.hammerle.snuviscript.instructions.ElseIf;
import me.hammerle.snuviscript.instructions.EndIf;
import me.hammerle.snuviscript.instructions.For;
import me.hammerle.snuviscript.instructions.Function;
import me.hammerle.snuviscript.instructions.Goto;
import me.hammerle.snuviscript.instructions.If;
import me.hammerle.snuviscript.instructions.IfGoto;
import me.hammerle.snuviscript.instructions.Instruction;
import me.hammerle.snuviscript.instructions.Return;
import me.hammerle.snuviscript.instructions.Try;
import me.hammerle.snuviscript.instructions.UserFunction;
import me.hammerle.snuviscript.instructions.While;

public class Compiler
{
    private int index = 0;
    private Token[] tokens = null;
    private final ArrayList<Instruction> instr = new ArrayList<>();
    
    private HashMap<String, Integer> labels = null;
    private HashMap<String, HashMap<String, Integer>> localLabels = null;
    private HashMap<String, Variable> vars = null;
    private final HashMap<String, LocalVariable> localVars = new HashMap<>();
    private HashMap<String, Integer> functions = null;
    
    private final Stack<Break> breakStack = new Stack<>();
    private final Stack<Continue> continueStack = new Stack<>();
    
    private String inFunction = null;
    private boolean lineExpression = false;

    private void addConstant(int line, InputProvider ip)
    {
        instr.add(new Constant(line, ip));
    }
    
    private void addFunction(int line, int args, String name)
    {
        instr.add(new Function(line, args, FunctionRegistry.getFunction(name)));
    }
    
    private void addGoto(int line, int jump)
    {
        Goto g = new Goto(line, 0);
        g.setJump(jump);
        instr.add(g);
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
        throw new PreScriptException(String.format("expected %s got %s", type, peek().getType()), peek().getLine());
    }  

    private void noReturnForLastFunction()
    {
        instr.get(instr.size() - 1).setNoReturn();
    }
    
    public Instruction[] compile(Token[] tokens, HashMap<String, Integer> labels, 
            HashMap<String, Variable> vars, HashMap<String, Integer> functions,
            HashMap<String, HashMap<String, Integer>> localLabels)
    {
        this.tokens = tokens;
        index = 0;
        instr.clear();
        this.labels = labels;
        this.localLabels = localLabels;
        this.vars = vars;
        this.functions = functions;
        localVars.clear();
        inFunction = null;

        while(!isAtEnd())
        {
            line();
        }
        
        this.tokens = null;
        this.labels = null;
        this.vars = null;
        this.functions = null;
        localVars.clear();
        
        Instruction[] code = instr.toArray(new Instruction[instr.size()]);
        instr.clear();
        return code;
    }
    
    private void line()
    {
        int oldIndex = index;
        Token t = advance();
        switch(t.getType())
        {
            case LABEL: handleLabel(); break;
            case IF: handleIf(); break;
            case SEMICOLON: break;
            case FOR: handleFor(); break;
            case BREAK: 
                Break b = new Break(previous().getLine());
                breakStack.add(b);
                instr.add(b); 
                consume(SEMICOLON);
                break;
            case CONTINUE:
                Continue c = new Continue(previous().getLine());
                continueStack.add(c);
                instr.add(c);
                consume(SEMICOLON);
                break;
            case FUNCTION: handleUserFunction(); break;
            case RETURN: handleReturn(); break;
            case WHILE: handleWhile(); break;
            case TRY: handleTry(); break;
            default:
                index = oldIndex;
                lineExpression = false;
                expression();
                if(!lineExpression)
                {
                    throw new PreScriptException("missing statement", t.getLine());
                }
                consume(SEMICOLON);
        }
        noReturnForLastFunction();
    }
    
    private void handleLabel()
    {
        String name = previous().getData().toString();
        name = name.substring(1); // cut off @ at start
        if(inFunction != null)
        {
            HashMap<String, Integer> llabel = localLabels.get(inFunction);
            if(llabel == null)
            {
                llabel = new HashMap<>();
                localLabels.put(inFunction, llabel);
            }
            llabel.put(name, instr.size() - 1);
        }
        else
        {
            labels.put(name, instr.size() - 1);
        }
    }
    
    private void handleIf()
    {
        Token t = previous();
        consume(OPEN_BRACKET);
        expression();
        If i = new If(t.getLine());
        instr.add(i);
        consume(CLOSE_BRACKET);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        i.setJump(instr.size() - 1);
        handleElseIf();
        instr.add(new EndIf(instr.get(instr.size() - 1).getLine()));
    }
    
    private void handleElseIf()
    {
        while(match(ELSEIF))
        {
            Token t = previous();
            consume(OPEN_BRACKET);
            expression();
            ElseIf e = new ElseIf(t.getLine());
            instr.add(e);
            consume(CLOSE_BRACKET);
            consume(OPEN_CURVED_BRACKET);
            while(!match(CLOSE_CURVED_BRACKET))
            {
                line();
            }
            e.setJump(instr.size() - 1);
        }
        handleElse();
    }

    private void handleElse()
    {
        if(match(ELSE))
        {
            Else e = new Else(previous().getLine());
            instr.add(e);
            consume(OPEN_CURVED_BRACKET);
            while(!match(CLOSE_CURVED_BRACKET))
            {
                line();
            }
            e.setJump(instr.size() - 1);
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
            noReturnForLastFunction();
        }
        
        int forConditionStart = instr.size() - 1;
        if(!match(SEMICOLON))
        {
            expression();
            consume(SEMICOLON);
        }
        Goto forGoto = new Goto(instr.get(instr.size() - 1).getLine(), 0);
        instr.add(forGoto);
        
        int forLoopFunctionStart = instr.size() - 1;
        if(!match(CLOSE_BRACKET))
        {
            expression();
            consume(CLOSE_BRACKET);
            noReturnForLastFunction();
        }
        Goto conditionGoto = new Goto(instr.get(instr.size() - 1).getLine(), 0);
        conditionGoto.setJump(forConditionStart);
        instr.add(conditionGoto);
        
        int forStart = instr.size() - 1;
        forGoto.setJump(forStart);
        For f = new For(t.getLine()); 
        instr.add(f);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        Goto loopFunctionGoto = new Goto(instr.get(instr.size() - 1).getLine(), 0);
        loopFunctionGoto.setJump(forLoopFunctionStart);
        instr.add(loopFunctionGoto);
        int forEnd = instr.size() - 1;
        f.setJump(forEnd);
        
        setBreakContinueJumps(forLoopFunctionStart, forEnd);
    }
    
    private void setBreakContinueJumps(int start, int end)
    {
        while(!continueStack.empty())
        {
            continueStack.pop().setJump(start);
        }
        while(!breakStack.empty())
        {
            breakStack.pop().setJump(end);
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
        String name = t.getData().toString().toLowerCase();
        UserFunction uf = new UserFunction(t.getLine(), name, list.toArray(new String[list.size()]));
        functions.put(name, instr.size());
        instr.add(uf);
        consume(OPEN_CURVED_BRACKET);
        inFunction = name;
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        inFunction = null;
        instr.add(new Return(instr.get(instr.size() - 1).getLine(), 0));
        uf.setJump(instr.size() - 1);
    }
    
    private void handleReturn()
    {
        Token t = previous();
        int args = 0;
        if(!match(SEMICOLON))
        {
            args = 1;
            expression();
            consume(SEMICOLON);
        }
        instr.add(new Return(t.getLine(), args));
    }
    
    private void handleWhile()
    {
        int whileStart = instr.size() - 1;
        Token t = previous();
        consume(OPEN_BRACKET);
        expression();
        While w = new While(t.getLine());
        instr.add(w);
        consume(CLOSE_BRACKET);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        addGoto(instr.get(instr.size() - 1).getLine(), whileStart);
        int whileEnd = instr.size() - 1;
        w.setJump(whileEnd);
        setBreakContinueJumps(whileStart, whileEnd);
    }
    
    private void handleTry()
    {
        Try t = new Try(previous().getLine());
        instr.add(t);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        consume(CATCH);
        Catch c = new Catch(previous().getLine());
        instr.add(c);
        t.setJump(instr.size() - 1);
        consume(OPEN_CURVED_BRACKET);
        while(!match(CLOSE_CURVED_BRACKET))
        {
            line();
        }
        c.setJump(instr.size() - 1);
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
            lineExpression = true;
        }
    }   
    
    private void logicalOr()
    {
        logicalAnd();
        while(match(OR))
        {
            Token t = previous();
            IfGoto ifGoto = new IfGoto(t.getLine(), true);
            instr.add(ifGoto);
            logicalAnd();
            ifGoto.setJump(instr.size());
            addFunction(t.getLine(), 2, t.getType().getName());
        }
    }  
    
    private void logicalAnd()
    {
        equality();
        while(match(AND))
        {
            Token t = previous();
            IfGoto ifGoto = new IfGoto(t.getLine(), false);
            instr.add(ifGoto);
            equality();
            ifGoto.setJump(instr.size());
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
        while(match(DIV, MUL, MOD))
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
            if(t.getType() == INC || t.getType() == DEC)
            {
                lineExpression = true;
            }
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
            lineExpression = true;
        }
    }

    private void primary()
    {
        Token t = advance();
        switch(t.getType())
        {
            case FALSE: addConstant(t.getLine(), ConstantBoolean.FALSE); return;
            case TRUE: addConstant(t.getLine(), ConstantBoolean.TRUE); return;
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
        lineExpression = true;
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
        boolean global = name.startsWith("$");
        if(inFunction != null && !global)
        {
            LocalVariable v = localVars.get(name);
            if(v != null)
            {
                return v;
            }
            v = new LocalVariable(name);
            localVars.put(name, v);
            return v;
        }
        
        if(global)
        {
            name = name.substring(1);
        }
        
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