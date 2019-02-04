package me.hammerle.snuviscript.token;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import me.hammerle.snuviscript.array.DynamicArray;
import me.hammerle.snuviscript.code.Function;
import me.hammerle.snuviscript.code.FunctionLoader;
import me.hammerle.snuviscript.code.InputProvider;
import me.hammerle.snuviscript.code.Instruction;
import me.hammerle.snuviscript.code.JumpData;
import me.hammerle.snuviscript.code.SignInverter;
import me.hammerle.snuviscript.constants.ConstantBoolean;
import me.hammerle.snuviscript.constants.ConstantDouble;
import me.hammerle.snuviscript.constants.ConstantNull;
import me.hammerle.snuviscript.constants.ConstantString;
import me.hammerle.snuviscript.exceptions.PreScriptException;
import static me.hammerle.snuviscript.token.TokenType.*;
import me.hammerle.snuviscript.variable.ArrayVariable;
import me.hammerle.snuviscript.variable.Variable;

public class Parser 
{
    private final HashMap<String, Variable> vars = new HashMap<>();
    private final HashMap<String, Integer> labels = new HashMap<>();
    
    private boolean tryState = false;
    private boolean cancel = false;
    
    private class JumpWrapper
    {
        private final JumpData data;
        private final String function;
        
        public JumpWrapper(JumpData data, String function)
        {
            this.data = data;
            this.function = function;
        }
    }
    
    private final Stack<JumpWrapper> jumps = new Stack<>();
    private final Stack<JumpWrapper> loopJumps = new Stack<>();
    private final LinkedList<JumpData> breakContinueJumps = new LinkedList<>();
    
    private final Token[] tokens;
    private int current = 0;
    private int layer = 0;
    
    private final LinkedList<Instruction> inst = new LinkedList<>();
    
    public Parser(LinkedList<Token> tokens)
    {
        this.tokens = tokens.toArray(new Token[tokens.size()]);
        //tokens.forEach(t -> System.out.println(t));
    }
    
    // -------------------------------------------------------------------------
    // utility
    // -------------------------------------------------------------------------
    
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
    
    private boolean check(TokenType tokenType) 
    {
        if(current >= tokens.length) 
        {
            return false;
        }
        return tokens[current].getToken() == tokenType;
    }
    
    private Token advance() 
    {
        if(current < tokens.length)
        {
            current++;
        }
        return previous();
    }

    private Token previous() 
    {
        return tokens[current - 1];
    }
    
    private void consume(TokenType type, String s)
    {
        if(tokens[current].getToken() != type)
        {
            throw new PreScriptException(s, tokens[current].getLine());
        }
        current++;
    }
    
    private void peek(TokenType type, String s)
    {
        if(tokens[current].getToken() != type)
        {
            throw new PreScriptException(s, tokens[current].getLine());
        }
    }
    
    // -------------------------------------------------------------------------
    // parsing precedence layers
    // http://en.cppreference.com/w/c/language/operator_precedence
    // -------------------------------------------------------------------------
    
    private void addInstruction(int line, String function, InputProvider... in)
    {
        inst.add(new Instruction(line, (byte) layer, new Function(FunctionLoader.getFunction(function), in)));
    }
    
    public Instruction[] parseTokens()
    {
        while(current < tokens.length && tokens[current].getToken() != END_OF_FILE)
        {
            while(match(LABEL))
            {
                if(labels.put(previous().getData().toString(), inst.size()) != null)
                {
                    throw new PreScriptException("label duplicate", previous().getLine());
                }
                match(SEMICOLON);
            }
            if(match(END_OF_FILE))
            {
                break;
            }
            
            int line = tokens[current].getLine();
            inst.add(new Instruction(line, (byte) layer, parseExpression()));
            tryState = false;
            if(cancel)
            {
                cancel = false;
                continue;
            }
            
            if(match(OPEN_CURVED_BRACKET))
            {
                layer++;
            }
            else
            {
                consume(SEMICOLON, "unexpected token after expression: " + tokens[current]);
            }

            while(match(CLOSE_CURVED_BRACKET))
            {
                layer--;
                
                if(jumps.isEmpty())
                {
                    throw new PreScriptException("} without a corresponding function and / or {", previous().getLine());
                }
                JumpWrapper data = jumps.pop();
                switch(data.function)
                {
                    case "try":
                    {
                        peek(CATCH, "try without catch");
                        data.data.setRelativeJump(inst.size());
                        tryState = true;
                        break;
                    }
                    case "catch":
                    {
                        data.data.setRelativeJump(inst.size());
                        break;
                    }
                    case "else":
                    case "elseif":
                    case "if":
                    {
                        data.data.setRelativeJump(inst.size() + 1);
                        addInstruction(previous().getLine(), "endif");
                        break;
                    }
                    case "for":
                    {
                        loopJumps.pop();
                        createBreakContinue(inst.size());
                        JumpData jump = data.data;
                        jump.setRelativeJump(inst.size());
                        addInstruction(previous().getLine(), "next", new JumpData(-jump.getInt(null) - 1));
                        break;
                    }
                    case "while":
                    {
                        loopJumps.pop();
                        createBreakContinue(inst.size());

                        JumpData jump = data.data;
                        jump.setRelativeJump(inst.size() + 1);
                        addInstruction(previous().getLine(), "wend", new JumpData(-jump.getInt(null) - 1));
                        break;
                    }
                }
            }
        }
        return inst.toArray(new Instruction[inst.size()]);
    }
    
    private void createBreakContinue(int current)
    {
        breakContinueJumps.forEach(jump -> jump.setRelativeJump(current));
        breakContinueJumps.clear();
    }
    
    private InputProvider binaryFunction(InputProvider left, Object t, InputProvider right)
    {
        return new Function(FunctionLoader.getFunction(t.toString()), new InputProvider[] {left, right});
    }
    
    private InputProvider unaryFunction(InputProvider in, Object t)
    {
        return new Function(FunctionLoader.getFunction(t.toString()), new InputProvider[] {in});
    }
    
    private InputProvider parseExpression()
    {
        return parseAssignment();
    }
    
    // level 14
    private InputProvider parseAssignment() 
    {
        InputProvider expr = parseLogicalOr();
        while(match(SET, ADD_SET, SUB_SET, MUL_SET, DIV_SET, MOD_SET, LEFT_SHIFT_SET, RIGHT_SHIFT_SET, BIT_AND_SET, BIT_XOR_SET, BIT_OR_SET)) 
        {
            Token operator = previous();
            InputProvider right = parseAssignment();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 12
    private InputProvider parseLogicalOr() 
    {
        InputProvider expr = parseLogicalAnd();
        while(match(OR)) 
        {
            Token operator = previous();
            InputProvider right = parseLogicalAnd();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 11
    private InputProvider parseLogicalAnd() 
    {
        InputProvider expr = parseBitOr();
        while(match(AND)) 
        {
            Token operator = previous();
            InputProvider right = parseBitOr();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 10
    private InputProvider parseBitOr() 
    {
        InputProvider expr = parseBitXor();
        while(match(BIT_OR)) 
        {
            Token operator = previous();
            InputProvider right = parseBitXor();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 9
    private InputProvider parseBitXor() 
    {
        InputProvider expr = parseBitAnd();
        while(match(BIT_XOR)) 
        {
            Token operator = previous();
            InputProvider right = parseBitAnd();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 8
    private InputProvider parseBitAnd() 
    {
        InputProvider expr = parseEquality();
        while(match(BIT_AND)) 
        {
            Token operator = previous();
            InputProvider right = parseEquality();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 7
    private InputProvider parseEquality() 
    {
        InputProvider expr = parseComparison();
        while(match(EQUAL, NOT_EQUAL)) 
        {
            Token operator = previous();
            InputProvider right = parseComparison();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 6
    private InputProvider parseComparison() 
    {
        InputProvider expr = parseShifting();
        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) 
        {
            Token operator = previous();
            InputProvider right = parseShifting();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 5
    private InputProvider parseShifting() 
    {
        InputProvider expr = parseAddition();
        while(match(LEFT_SHIFT, RIGHT_SHIFT)) 
        {
            Token operator = previous();
            InputProvider right = parseAddition();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 4
    private InputProvider parseAddition() 
    {
        InputProvider expr = parseMultiplication();
        while(match(SUB, ADD)) 
        {
            Token operator = previous();
            InputProvider right = parseMultiplication();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }

    // level 3
    private InputProvider parseMultiplication() 
    {
        InputProvider expr = parseUnary();
        while(match(DIV, MUL, MOD)) 
        {
            Token operator = previous();
            InputProvider right = parseUnary();
            expr = binaryFunction(expr, operator, right);
        }
        return expr;
    }
    
    // level 2
    private InputProvider parseUnary() 
    {
        if(match(INVERT, SUB, BIT_INVERT, INC, DEC)) 
        {
            if(previous().getToken() == SUB)
            {
                return new SignInverter(parseUnary());
            }
            Token operator = previous();
            InputProvider right = parseUnary();
            return unaryFunction(right, operator);
        }
        return parsePost();
    }
    
    // level 1
    private InputProvider parsePost() 
    {
        InputProvider expr = primary();
        while(true) 
        {
            if(match(INC, DEC))
            {
                Token operator = previous();
                expr = unaryFunction(expr, "p" + operator);
            }
            else if(match(OPEN_BRACKET))
            {
                expr = new Function(FunctionLoader.getFunction(expr.toString()), parseArguments(CLOSE_BRACKET));     
            }
            else if(match(OPEN_SQUARE_BRACKET))
            {
                String name = expr.toString();
                Variable oldVar = vars.get(name);
                if(oldVar == null)
                {
                    oldVar = new ArrayVariable(name);
                    vars.put(name, oldVar);
                }
                return new DynamicArray(oldVar, parseArguments(CLOSE_SQUARE_BRACKET));
            }
            else
            {
                break;
            }
        }
        return expr;
    }
    
    private InputProvider[] parseArguments(TokenType close)
    {
        if(match(close))
        {
            return new InputProvider[0];
        }
        LinkedList<InputProvider> list = new LinkedList<>();
        while(true)
        {
            list.add(parseExpression());
            if(match(close))
            {
                return list.toArray(new InputProvider[list.size()]);
            }
            consume(COMMA, "missing ',' in function");
        }
    }
    
    private InputProvider primary() 
    {
        if(match(FALSE)) 
        {
            return ConstantBoolean.FALSE;
        }
        else if(match(TRUE)) 
        {
            return ConstantBoolean.TRUE;
        }
        else if(match(NULL)) 
        {
            return ConstantNull.NULL;
        }
        else if(match(DOUBLE)) 
        {
            return new ConstantDouble((double) previous().getData());
        }
        else if(match(TEXT)) 
        {
            return new ConstantString((String) previous().getData());
        }
        else if(match(LABEL)) 
        {
            return new ConstantString(((String) previous().getData()).substring(1));
        }
        else if(match(VAR)) 
        {
            String name = (String) previous().getData();
            Variable v = vars.get(name);
            if(v == null)
            {
                v = new Variable(name);
                vars.put(name, v);
            }
            return v;
        }
        else if(match(IF, ELSE_IF))
        {
            String name = previous().getToken() == IF ? "if" : "elseif";
            
            consume(OPEN_BRACKET, "if without (");
            
            InputProvider[] input = parseArguments(CLOSE_BRACKET);
            InputProvider[] realInput = new InputProvider[input.length + 1];
            System.arraycopy(input, 0, realInput, 0, input.length);
            
            JumpData jump = new JumpData(inst.size());
            realInput[input.length] = jump;
            jumps.push(new JumpWrapper(jump, name));
            return new Function(FunctionLoader.getFunction(name), realInput);
        }
        else if(match(ELSE))
        {
            peek(OPEN_CURVED_BRACKET, "unexpected token after 'else': " + tokens[current]);
            JumpData jump = new JumpData(inst.size());
            jumps.push(new JumpWrapper(jump, "else"));
            return unaryFunction(jump, "else");
        }
        else if(match(FOR))
        {
            consume(OPEN_BRACKET, "for without (");
            // expected syntax
            // for(var, start, end, step)
            // for(var, start, end)
            InputProvider[] input = parseArguments(CLOSE_BRACKET);
            if(input.length != 3 && input.length != 4)
            {
                throw new PreScriptException("invalid 'for' syntax", previous().getLine());
            }
            InputProvider[] realInput = new InputProvider[5];

            System.arraycopy(input, 0, realInput, 0, input.length);

            if(input.length == 3)
            {
                realInput[3] = new ConstantDouble(1.0);
            }

            JumpData jump = new JumpData(inst.size());
            realInput[4] = jump;
            JumpWrapper wrapper = new JumpWrapper(jump, "for");
            jumps.push(wrapper);
            loopJumps.push(wrapper);
            return new Function(FunctionLoader.getFunction("for"), realInput);
        }
        else if(match(WHILE))
        {
            consume(OPEN_BRACKET, "for without (");
            // expected syntax
            // while(condition)
            InputProvider[] input = parseArguments(CLOSE_BRACKET);
            if(input.length != 1)
            {
                throw new PreScriptException("invalid conditions at 'while'", previous().getLine());
            }
            InputProvider[] realInput = new InputProvider[2];
            realInput[0] = input[0];

            JumpData jump = new JumpData(inst.size());
            realInput[1] = jump;

            JumpWrapper wrapper = new JumpWrapper(jump, "while");
            jumps.push(wrapper);
            loopJumps.push(wrapper);

            return new Function(FunctionLoader.getFunction("while"), realInput);
        }
        else if(match(FUNCTION))
        {
            cancel = true;
            int counter = 0;
            while(current < tokens.length && tokens[current].getToken() != OPEN_CURVED_BRACKET)
            {
                current++;
            }
            counter++;
            current++;
            while(current < tokens.length && counter != 0)
            {
                if(tokens[current].getToken() == OPEN_CURVED_BRACKET)
                {
                    counter++;
                }
                else if(tokens[current].getToken() == CLOSE_CURVED_BRACKET)
                {
                    counter--;
                }
                current++;
            }
            return new Function(FunctionLoader.getFunction("nothing"), new InputProvider[0]);
        }
        else if(match(BREAK))
        {
            if(loopJumps.isEmpty())
            {
                throw new PreScriptException("break without a loop", previous().getLine());
            }
            JumpData jump = new JumpData(inst.size() - 1);
            breakContinueJumps.add(jump);
            return unaryFunction(jump, "break");
        }
        else if(match(CONTINUE))
        {
            if(loopJumps.isEmpty())
            {
                throw new PreScriptException("continue without a loop", previous().getLine());
            }
            JumpData jump = new JumpData(inst.size());
            breakContinueJumps.add(jump);
            return unaryFunction(jump, "continue");
        }
        else if(match(RETURN))
        {
            if(match(SEMICOLON))
            {
                current--;
                return new Function(FunctionLoader.getFunction("return"), new InputProvider[0]);
            }
            return unaryFunction(parseExpression(), "return");
        }
        else if(match(TRY))
        {
            peek(OPEN_CURVED_BRACKET, "unexpected token after 'try': " + tokens[current]);
            JumpData jump = new JumpData(inst.size());
            jumps.push(new JumpWrapper(jump, "try"));
            return unaryFunction(jump, "try");
        }
        else if(match(CATCH))
        {
            if(!tryState)
            {
                throw new PreScriptException("catch without try", previous().getLine());
            }
            peek(OPEN_CURVED_BRACKET, "unexpected token after 'catch': " + tokens[current]);
            JumpData jump = new JumpData(inst.size());
            jumps.push(new JumpWrapper(jump, "catch"));
            return unaryFunction(jump, "catch");
        }
        else if(match(OPEN_BRACKET)) 
        {
            InputProvider expr = parseExpression();
            consume(CLOSE_BRACKET, "'(' without ')'");
            return expr;
        }
        throw new PreScriptException("unexpected token: " + tokens[current], tokens[current].getLine());
    }
}