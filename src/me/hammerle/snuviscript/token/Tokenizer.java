package me.hammerle.snuviscript.token;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import me.hammerle.snuviscript.exceptions.PreScriptException;

public class Tokenizer
{
    private final ArrayList<Token> tokens = new ArrayList<>();
    private String code = "";
    private int index = 0;
    private int old = 0;
    private int line = 1;
    
    private boolean isLiteralCharacter(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_' || c == '.';
    }
    
    private char charAt(int index)
    {
        if(index < 0 || index >= code.length())
        {
            return ' ';
        }
        return code.charAt(index);
    }
    
    private void add(int from, int to)
    {
        String part = code.substring(from, to);
        try
        {
            double d = Double.parseDouble(part);
            tokens.add(new DataToken(TokenType.NUMBER, line, d));
        }
        catch(NumberFormatException ex)
        {
            tokens.add(new DataToken(TokenType.LITERAL, line, part));
        }
    }
    
    private void addIfNotEmpty(int from, int to)
    {
        if(to - from <= 0)
        {
            return;
        }
        add(from, to);
    }
    
    private void skipOneLineComment()
    {
        while(index < code.length() && code.charAt(index) != '\n')
        {
            index++;
        }
        line++;
        old = index + 1;
    }
    
    private void skipComment()
    {
        while(index < code.length())
        {
            if(code.charAt(index) == '\n')
            {
                line++;
            }
            else if(code.charAt(index) == '*' && charAt(index + 1) == '/')
            {
                break;
            }
            index++;
        }
        old = index + 2;
        index++;
    }
    
    private void handleString()
    {
        addIfNotEmpty(old, index);
        old = index + 1;
        index++;
        while(index < code.length() && code.charAt(index) != '"')
        {
            if(code.charAt(index) == '\n')
            {
                line++;
            }
            index++;
        }
        add(old, index);
        old = index + 1;
    }
    
    private void handleNewLine()
    {
        addIfNotEmpty(old, index);
        old = index + 1;
        line++;
    }
    
    private void handleSpace()
    {
        addIfNotEmpty(old, index);
        old = index + 1;
    }
    
    private void handleSpecialCharacter()
    {
        addIfNotEmpty(old, index);

        TokenType type = TokenType.getMatching(code.charAt(index), charAt(index + 1), charAt(index + 2));
        switch(type)
        {
            case UNKNOWN: throw new PreScriptException("unknown token", line);
            case ONE_LINE_COMMENT: 
                skipOneLineComment(); 
                break;
            case COMMENT: 
                skipComment(); 
                break;
            default:
                tokens.add(new Token(type, line));
                old = index + type.getLength();
                index = old - 1;
        }
    }
    
    public Token[] tokenize(InputStream in)
    {
        code = readStream(in);
        index = 0;
        old = 0;
        tokens.clear();
        
        for(; index < code.length(); index++)
        {
            char c = code.charAt(index);
            if(isLiteralCharacter(c))
            {
                continue;
            }
            switch(c)
            {
                case '\n': handleNewLine(); break;
                case '"': handleString(); break;
                case '\t':
                case ' ': handleSpace(); break;
                default: handleSpecialCharacter();
            }
        }
        return tokens.toArray(new Token[tokens.size()]);
    }
    
    private String readStream(InputStream in)
    {
        try
        {
            int bufferSize = in.available();
            StringBuilder sb = new StringBuilder(bufferSize);
            
            while(in.available() > 0)
            {
                int data = in.read();
                if((data & 0x80) != 0)
                {
                    data = ((data & 0x1F) << 6) | (in.read() & 0x3F);
                }
                sb.append((char) data);
            }
            
            return sb.toString();
        }
        catch(IOException ex)
        {
            throw new PreScriptException("cannot read stream - " + ex.getMessage(), -1);
        }
    }
}