package me.hammerle.snuviscript.code;

import me.hammerle.snuviscript.inputprovider.InputProvider;
import java.util.Random;

public class SnuviUtils 
{
    private static final Random RANDOM = new Random();
    
    public static int randomInt(int min, int max)
    {
        return RANDOM.nextInt((max - min) + 1) + min;
    }
    
    public static String toString(double d)
    {
        if(d == (int) d)
        {
            return String.valueOf((int) d);
        }
        return String.valueOf(d);
    }
    
    // -------------------------------------------------------------------------
    // connectors
    // -------------------------------------------------------------------------
    
    public static String connect(Script sc, InputProvider[] c, int skip) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for(int i = skip; i < c.length; i++)
        {
            sb.append(c[i].getString(sc));
        }
        return sb.toString();
    }
    
    public static String connect(Script sc, InputProvider[] c, String s, int skip) throws Exception
    {
        StringBuilder sb = new StringBuilder();
        if(skip < c.length)
        {
            sb.append(c[skip].getString(sc));
        }
        for(int i = skip + 1; i < c.length; i++)
        {
            sb.append(s);
            sb.append(c[i].getString(sc));
        }
        return sb.toString();
    }
    
    public static Object convert(String input)
    {
        if(input == null)
        {
            return null;
        }
        input = input.trim();
        if(input.equals("true"))
        {
            return true;
        }
        else if(input.equals("false"))
        {
            return false;
        }
        else if(input.equals("null"))
        {
            return null;
        }
        else if(input.startsWith("\"") && input.endsWith("\""))
        {
            if(input.length() == 1)
            {
                return "\"";
            }
            return input.substring(1, input.length() - 1);
        }
        try
        {
            return Double.parseDouble(input);
        }
        catch(NumberFormatException ex)
        {
            return input;
        }
    }
}
