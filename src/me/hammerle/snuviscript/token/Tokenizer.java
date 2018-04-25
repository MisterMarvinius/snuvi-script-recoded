package me.hammerle.snuviscript.token;

import java.util.LinkedList;
import me.hammerle.snuviscript.code.Instruction;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class Tokenizer 
{
    private final char[] code;
    private int line;
    
    private final LinkedList<Token> data;
    
    public Tokenizer(String code)
    {
        this.code = code.toCharArray();
        this.data = new LinkedList<>();
    }
    
    private void addToken(TokenType t)
    {
        data.add(new Token(t, line + 1));
    }
    
    private void addToken(TokenType t, Object o)
    {
        data.add(new Token(t, line + 1, o));
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
                while(index < code.length && (Character.isLetterOrDigit(code[index]) || code[index] == '.' || code[index] == '_'))
                {
                    index++;
                }
                String s = new String(code, old, index - old);
                switch(s)
                {
                    case "if": addToken(TokenType.IF); break;
                    case "elseif": addToken(TokenType.ELSE_IF); break;
                    case "else": addToken(TokenType.ELSE); break;
                    case "for": addToken(TokenType.FOR); break;
                    case "while": addToken(TokenType.WHILE); break;
                    case "function": addToken(TokenType.FUNCTION); break;
                    case "break": addToken(TokenType.BREAK); break;
                    case "continue": addToken(TokenType.CONTINUE); break;
                    case "return": addToken(TokenType.RETURN); break;
                    case "try": addToken(TokenType.TRY); break;
                    case "catch": addToken(TokenType.CATCH); break;
                    default:
                        addToken(TokenType.VAR, s);
                }
                index--;
            }
            else if(Character.isDigit(code[index]))
            {
                int old = index;
                index++;
                while(index < code.length)
                {
                    switch(code[index])
                    {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        {
                            index++;
                            continue;
                        }
                        case '.':
                        {
                            index++;
                            while(index < code.length && Character.isDigit(code[index]))
                            {
                                index++;
                            }
                            break;
                        }
                    }
                    break;
                }
                addToken(TokenType.DOUBLE, Double.parseDouble(new String(code, old, index - old)));
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
                        {
                            line++;
                            break;
                        }
                        case '@':
                        {
                            int old = index;
                            index++;
                            while(index < code.length && (Character.isLetterOrDigit(code[index]) || code[index] == '.' || code[index] == '_'))
                            {
                                index++;
                            }
                            addToken(TokenType.LABEL, new String(code, old, index - old));
                            index--;
                            break;
                        }
                        case '"':
                        {
                            int old = index + 1;
                            index++;
                            while(index < code.length && code[index] != '"')
                            {
                                index++;
                            }
                            addToken(TokenType.TEXT, new String(code, old, index - old));
                            break;
                        }
                        case '+':
                        {
                            switch(code[index + 1])
                            {
                                case '+':
                                    addToken(TokenType.INC);
                                    index++;
                                    break;
                                case '=':
                                    addToken(TokenType.ADD_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.ADD);
                            }
                            break;
                        }
                        case '-':
                        {
                            switch(code[index + 1])
                            {
                                case '-':
                                    addToken(TokenType.DEC);
                                    index++;
                                    break;
                                case '=':
                                    addToken(TokenType.SUB_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.SUB);
                            }
                            break;
                        }
                        case '*':
                        {
                            if(code[index + 1] == '=')
                            {
                                addToken(TokenType.MUL_SET);
                                index++;
                            }
                            else
                            {
                                addToken(TokenType.MUL);
                            }
                            break;
                        }
                        case '/':
                        {
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
                                    while(code[index] != '*' || code[index + 1] != '/')
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
                                    addToken(TokenType.DIV_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.DIV);
                            }
                            break;
                        }
                        case '!':
                        {
                            if(code[index + 1] == '=')
                            {
                                addToken(TokenType.NOT_EQUAL);
                                index++;
                                break;
                            }
                            else
                            {
                                addToken(TokenType.INVERT);
                            }
                            break;
                        }
                        case '~':
                        {
                            addToken(TokenType.BIT_INVERT);
                            break;
                        }
                        case '%':
                        {
                            if(code[index + 1] == '=')
                            {
                                addToken(TokenType.MOD_SET);
                                index++;
                            }
                            else
                            {
                                addToken(TokenType.MOD);
                            }
                            break;
                        }
                        case '<':
                        {
                            switch(code[index + 1])
                            {
                                case '<':
                                    if(code[index + 2] == '=')
                                    {
                                        addToken(TokenType.LEFT_SHIFT_SET);
                                        index += 2;
                                    }
                                    else
                                    {
                                        addToken(TokenType.LEFT_SHIFT);
                                        index++;
                                    }
                                    break;
                                case '=':
                                    addToken(TokenType.LESS_EQUAL);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.LESS);
                            }
                            break;
                        }
                        case '>':
                        {
                            switch(code[index + 1])
                            {
                                case '>':
                                    if(code[index + 2] == '=')
                                    {
                                        addToken(TokenType.RIGHT_SHIFT_SET);
                                        index += 2;
                                    }
                                    else
                                    {
                                        addToken(TokenType.RIGHT_SHIFT);
                                        index++;
                                    }
                                    break;
                                case '=':
                                    addToken(TokenType.GREATER_EQUAL);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.GREATER);
                            }
                            break;
                        }
                        case '=': 
                        {
                            if(code[index + 1] == '=')
                            {
                                addToken(TokenType.EQUAL);
                                index++;
                                break;
                            }
                            else
                            {
                                addToken(TokenType.SET);
                            }
                            break;
                        }
                        case '&':
                        {
                            switch(code[index + 1])
                            {
                                case '&':
                                    addToken(TokenType.AND);
                                    index++;
                                    break;
                                case '=':
                                    addToken(TokenType.BIT_AND_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.BIT_AND);
                            }
                            break;
                        }
                        case '^':
                        {
                            if(code[index + 1] == '=')
                            {
                                addToken(TokenType.BIT_XOR_SET);
                                index++;
                                break;
                            }
                            else
                            {
                                addToken(TokenType.BIT_XOR);
                            }
                            break;
                        }
                        case '|':
                        {
                            switch(code[index + 1])
                            {
                                case '|':
                                    addToken(TokenType.OR);
                                    index++;
                                    break;
                                case '=':
                                    addToken(TokenType.BIT_OR_SET);
                                    index++;
                                    break;
                                default:
                                    addToken(TokenType.BIT_OR);
                            }
                            break;
                        }
                        case ',':
                            addToken(TokenType.COMMA);
                            break;
                        case '(':
                            addToken(TokenType.OPEN_BRACKET);
                            break;
                        case ')':
                            addToken(TokenType.CLOSE_BRACKET);
                            break;
                        case '[':
                            addToken(TokenType.OPEN_SQUARE_BRACKET);
                            break;
                        case ']':
                            addToken(TokenType.CLOSE_SQUARE_BRACKET);
                            break;
                        case '{':
                            addToken(TokenType.OPEN_CURVED_BRACKET);
                            break;
                        case '}':
                            addToken(TokenType.CLOSE_CURVED_BRACKET);
                            break;
                        case ';':
                            addToken(TokenType.SEMICOLON);
                            break;
                    }
                }
                catch(ArrayIndexOutOfBoundsException ex)
                {
                    throw new PreScriptException("unexpected code end", startLine, line);
                }
            }
        }       
        addToken(TokenType.END_OF_FILE);
        //data.forEach(e -> System.out.println(e));
        
        Parser p = new Parser(data);
        for(Instruction in : p.parseTokens())
        {
            System.out.println(in);
        }
    }
}
