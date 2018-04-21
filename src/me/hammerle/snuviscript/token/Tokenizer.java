package me.hammerle.snuviscript.token;

import java.util.LinkedList;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class Tokenizer 
{
    private final char[] code;
    private int line;
    
    private final LinkedList<TokenData> data;
    
    public Tokenizer(String code)
    {
        this.code = code.toCharArray();
        this.data = new LinkedList<>();
    }
    
    private void addToken(Token t)
    {
        data.add(new TokenData(t, line));
    }
    
    private void addToken(Token t, Object o)
    {
        data.add(new TokenData(t, line, o));
    }
    
    public void tokenize()
    {
        line = 0;
        for(int index = 0; index < code.length; index++)
        {
            if(Character.isLetter(code[index]))
            {
                int old = index;
                index++;
                while(index < code.length && Character.isLetterOrDigit(code[index]))
                {
                    index++;
                }
                String s = new String(code, old, index - old);
                switch(s)
                {
                    case "if": addToken(Token.IF); break;
                    case "else": addToken(Token.ELSE); break;
                    case "for": addToken(Token.FOR); break;
                    case "while": addToken(Token.WHILE); break;
                    case "function": addToken(Token.FUNCTION); break;
                    case "break": addToken(Token.BREAK); break;
                    case "continue": addToken(Token.CONTINUE); break;
                    case "return": addToken(Token.RETURN); break;
                    case "goto": addToken(Token.GOTO); break;
                    case "gosub": addToken(Token.GOSUB); break;
                    default:
                        addToken(Token.VAR, s);
                }
                index--;
            }
            else if(Character.isDigit(code[index]))
            {
                int value = code[index] - '0';
                index++;
                boolean b = true;
                while(index < code.length)
                {
                    if(Character.isDigit(code[index]))
                    {
                        if(value <= (2147483647 - code[index] + '0') / 10)
                        {
                            value = value * 10 + code[index] - '0';
                        }
                        else
                        {
                            throw new PreScriptException("int to big", line);
                        }
                    }
                    else if(code[index] == '.')
                    {
                        index++;
                        double dValue = value;
                        double factor = 10.0;
                        while(index < code.length)
                        {
                            if(Character.isDigit(code[index]))
                            {
                                dValue = dValue + (code[index] - '0') / factor;
                                factor *= 10.0;
                            }
                            else
                            {
                                break;
                            }
                            index++;
                        }
                        addToken(Token.DOUBLE, dValue);
                        b = false;
                        break;
                    }
                    else
                    {
                        break;
                    }
                    index++;
                }
                if(b)
                {
                    addToken(Token.INT, value);
                }
                index--;
            }
            else
            {
                int startLine = line;
                try
                {
                    switch(code[index])
                    {
                        case '\n':
                            line++;
                            break;
                        case '@':
                            int old = index;
                            index++;
                            while(index < code.length && Character.isLetterOrDigit(code[index]))
                            {
                                index++;
                            }
                            addToken(Token.LABEL, new String(code, old, index - old));
                            index--;
                            break;
                        case '+':
                            switch(code[index + 1])
                            {
                                case '+':
                                    addToken(Token.INC);
                                    index++;
                                    break;
                                case '=':
                                    addToken(Token.ADD_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.ADD);
                            }
                            break;
                        case '-':
                            switch(code[index + 1])
                            {
                                case '-':
                                    addToken(Token.DEC);
                                    index++;
                                    break;
                                case '=':
                                    addToken(Token.SUB_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.SUB);
                            }
                            break;
                        case '*':
                            if(code[index + 1] == '=')
                            {
                                addToken(Token.MUL_SET);
                                index++;
                            }
                            else
                            {
                                addToken(Token.MUL);
                            }
                            break;
                        case '/':
                            switch(code[index + 1])
                            {
                                case '/':
                                    index += 2;
                                    while(code[index] != '\n')
                                    {
                                        index++;
                                    }
                                    index--;
                                    break;
                                case '*':
                                    index += 2;
                                    while(code[index] != '*' && code[index + 1] != '/')
                                    {
                                        if(code[index] == '\n')
                                        {
                                            line++;
                                        }
                                        index++;
                                    }
                                    index++;
                                    break;
                                case '=':
                                    addToken(Token.DIV_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.DIV);
                            }
                            break;
                        case '!':
                            if(code[index + 1] == '=')
                            {
                                addToken(Token.NOT_EQUAL);
                                index++;
                                break;
                            }
                            else
                            {
                                addToken(Token.INVERT);
                            }
                            break;
                        case '~':
                            addToken(Token.BIT_INVERT);
                            break;
                        case '%':
                            if(code[index + 1] == '=')
                            {
                                addToken(Token.MOD_SET);
                                index++;
                            }
                            else
                            {
                                addToken(Token.MOD);
                            }
                            break;
                        case '<':
                            switch(code[index + 1])
                            {
                                case '<':
                                    if(code[index + 2] == '=')
                                    {
                                        addToken(Token.LEFT_SHIFT_SET);
                                        index += 2;
                                    }
                                    else
                                    {
                                        addToken(Token.LEFT_SHIFT);
                                        index++;
                                    }
                                    break;
                                case '=':
                                    addToken(Token.SMALLER_EQUAL);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.SMALLER);
                            }
                            break;
                        case '>':
                            switch(code[index + 1])
                            {
                                case '>':
                                    if(code[index + 2] == '=')
                                    {
                                        addToken(Token.RIGHT_SHIFT_SET);
                                        index += 2;
                                    }
                                    else
                                    {
                                        addToken(Token.RIGHT_SHIFT);
                                        index++;
                                    }
                                    break;
                                case '=':
                                    addToken(Token.GREATER_EQUAL);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.GREATER);
                            }
                            break;
                        case '=':
                            if(code[index + 1] == '=')
                            {
                                addToken(Token.EQUAL);
                                index++;
                                break;
                            }
                            else
                            {
                                addToken(Token.SET);
                            }
                            break;
                        case '&':
                            switch(code[index + 1])
                            {
                                case '&':
                                    addToken(Token.AND);
                                    index++;
                                    break;
                                case '=':
                                    addToken(Token.BIT_AND_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.BIT_AND);
                            }
                            break;
                        case '^':
                            if(code[index + 1] == '=')
                            {
                                addToken(Token.BIT_XOR_SET);
                                index++;
                                break;
                            }
                            else
                            {
                                addToken(Token.BIT_XOR);
                            }
                            break;
                        case '|':
                            switch(code[index + 1])
                            {
                                case '|':
                                    addToken(Token.OR);
                                    index++;
                                    break;
                                case '=':
                                    addToken(Token.BIT_OR_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(Token.BIT_OR);
                            }
                            break;
                        case ',':
                            addToken(Token.COMMA);
                            break;
                        case '(':
                            addToken(Token.OPEN_BRACKET);
                            break;
                        case ')':
                            addToken(Token.CLOSE_BRACKET);
                            break;
                        case '[':
                            addToken(Token.OPEN_SQUARE_BRACKET);
                            break;
                        case ']':
                            addToken(Token.CLOSE_SQUARE_BRACKET);
                            break;
                        case '{':
                            addToken(Token.OPEN_CURVED_BRACKET);
                            break;
                        case '}':
                            addToken(Token.CLOSE_CURVED_BRACKET);
                            break;
                        case ';':
                            addToken(Token.SEMICOLON);
                            break;
                    }
                }
                catch(ArrayIndexOutOfBoundsException ex)
                {
                    throw new PreScriptException("unexpected code end", startLine, line);
                }
            }
        }
        
        data.forEach(d -> System.out.println(d));
    }
}
